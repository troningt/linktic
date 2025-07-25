package com.example.products.exception;

import com.example.products.controller.dto.ApiResponse;
import com.example.products.controller.dto.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para el servicio de productos
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Maneja excepciones de producto no encontrado
   */
  @ExceptionHandler(ProductNotFoundException.class)
  public ResponseEntity<ApiResponse<Object>> handleProductNotFoundException(
      ProductNotFoundException ex, WebRequest request) {

    log.warn("Product not found: {}", ex.getMessage());

    ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), request.getDescription(false));
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  /**
   * Maneja excepciones de producto ya existente
   */
  @ExceptionHandler(ProductAlreadyExistsException.class)
  public ResponseEntity<ApiResponse<Object>> handleProductAlreadyExistsException(
      ProductAlreadyExistsException ex, WebRequest request) {

    log.warn("Product already exists: {}", ex.getMessage());

    ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), request.getDescription(false));
    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  /**
   * Maneja errores de validación de datos de entrada
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> handleValidationException(
      MethodArgumentNotValidException ex, WebRequest request) {

    log.warn("Validation error: {}", ex.getMessage());

    List<ValidationError> validationErrors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(this::mapFieldError)
        .collect(Collectors.toList());

    ApiResponse<Object> response = ApiResponse.error(
        "Errores de validación en los datos de entrada",
        validationErrors
    );
    response.setPath(request.getDescription(false));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * Maneja errores de validación de parámetros
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {

    log.warn("Constraint violation: {}", ex.getMessage());

    List<ValidationError> validationErrors = ex.getConstraintViolations()
        .stream()
        .map(this::mapConstraintViolation)
        .collect(Collectors.toList());

    ApiResponse<Object> response = ApiResponse.error(
        "Errores de validación en los parámetros",
        validationErrors
    );
    response.setPath(request.getDescription(false));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * Maneja errores de tipo de argumento incorrecto
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException ex, WebRequest request) {

    log.warn("Method argument type mismatch: {}", ex.getMessage());

    String message = String.format("El parámetro '%s' debe ser de tipo '%s'",
        ex.getName(), ex.getRequiredType().getSimpleName());

    ApiResponse<Object> response = ApiResponse.error(message, request.getDescription(false));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * Maneja errores de formato JSON
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex, WebRequest request) {

    log.warn("HTTP message not readable: {}", ex.getMessage());

    ApiResponse<Object> response = ApiResponse.error(
        "Formato de datos inválido. Verifique la estructura JSON.",
        request.getDescription(false)
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * Maneja errores de autenticación (API Key inválida)
   */
  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiResponse<Object>> handleSecurityException(
      SecurityException ex, WebRequest request) {

    log.warn("Security exception: {}", ex.getMessage());

    ApiResponse<Object> response = ApiResponse.error(
        "No autorizado. API Key inválida o faltante.",
        request.getDescription(false)
    );
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  /**
   * Maneja excepciones genéricas no controladas
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleGenericException(
      Exception ex, WebRequest request) {

    log.error("Unhandled exception: ", ex);

    ApiResponse<Object> response = ApiResponse.error(
        "Error interno del servidor. Por favor contacte al administrador.",
        request.getDescription(false)
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  /**
   * Mapea un FieldError a ValidationError
   */
  private ValidationError mapFieldError(FieldError fieldError) {
    return ValidationError.builder()
        .field(fieldError.getField())
        .rejectedValue(fieldError.getRejectedValue())
        .message(fieldError.getDefaultMessage())
        .build();
  }

  /**
   * Mapea un ConstraintViolation a ValidationError
   */
  private ValidationError mapConstraintViolation(ConstraintViolation<?> violation) {
    String fieldName = violation.getPropertyPath().toString();
    // Extraer solo el nombre del campo (última parte después del punto)
    if (fieldName.contains(".")) {
      fieldName = fieldName.substring(fieldName.lastIndexOf('.') + 1);
    }

    return ValidationError.builder()
        .field(fieldName)
        .rejectedValue(violation.getInvalidValue())
        .message(violation.getMessage())
        .build();
  }
}
