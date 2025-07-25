package com.example.inventory.service;

import com.example.inventory.controller.dto.PurchaseRequestDto;
import com.example.inventory.controller.dto.PurchaseResponseDto;
import com.example.inventory.model.PurchaseHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Interfaz para el servicio de compras
 */
public interface PurchaseService {

  /**
   * Procesa una compra completa
   *
   * @param request Datos de la compra
   * @return Respuesta con los detalles de la compra
   */
  PurchaseResponseDto processPurchase(PurchaseRequestDto request);

  /**
   * Obtiene el detalle de una compra por su ID
   *
   * @param purchaseId UUID de la compra
   * @return Detalle de la compra
   */
  PurchaseResponseDto getPurchaseById(UUID purchaseId);

  /**
   * Obtiene el historial de compras con paginación
   *
   * @param pageable Información de paginación
   * @return Página con el historial de compras
   */
  Page<PurchaseResponseDto> getPurchaseHistory(Pageable pageable);

  /**
   * Obtiene el historial de compras de un producto específico
   *
   * @param productId ID del producto
   * @param pageable  Información de paginación
   * @return Página con el historial de compras del producto
   */
  Page<PurchaseResponseDto> getPurchaseHistoryByProduct(Long productId, Pageable pageable);

  /**
   * Obtiene el historial de compras con filtros
   *
   * @param productId   ID del producto (opcional)
   * @param status      Estado de la compra (opcional)
   * @param startDate   Fecha de inicio (opcional)
   * @param endDate     Fecha de fin (opcional)
   * @param minQuantity Cantidad mínima (opcional)
   * @param maxQuantity Cantidad máxima (opcional)
   * @param pageable    Información de paginación
   * @return Página con el historial filtrado
   */
  Page<PurchaseResponseDto> getPurchaseHistoryWithFilters(Long productId,
                                                          PurchaseHistory.PurchaseStatus status,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate,
                                                          Integer minQuantity,
                                                          Integer maxQuantity,
                                                          Pageable pageable);

  /**
   * Cancela una compra pendiente
   *
   * @param purchaseId UUID de la compra
   * @return true si se canceló exitosamente
   */
  boolean cancelPurchase(UUID purchaseId);

  /**
   * Marca una compra como reembolsada
   *
   * @param purchaseId UUID de la compra
   * @return true si se marcó como reembolsada exitosamente
   */
  boolean refundPurchase(UUID purchaseId);

  /**
   * Obtiene estadísticas de ventas para un período
   *
   * @param startDate Fecha de inicio
   * @param endDate   Fecha de fin
   * @return Estadísticas de ventas
   */
  Object getSalesStatistics(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Obtiene estadísticas de compras por producto
   *
   * @return Lista con estadísticas por producto
   */
  List<Object> getPurchaseStatisticsByProduct();

  /**
   * Obtiene los productos más vendidos
   *
   * @param limit Número máximo de productos
   * @return Lista de productos más vendidos
   */
  List<Object> getTopSellingProducts(Integer limit);

  /**
   * Obtiene un reporte de ventas diarias
   *
   * @param startDate Fecha de inicio
   * @param endDate   Fecha de fin
   * @return Reporte de ventas diarias
   */
  List<Object> getDailySalesReport(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Obtiene las compras pendientes por mucho tiempo
   *
   * @param hoursThreshold Horas de umbral para considerar una compra como pendiente
   * @return Lista de compras pendientes
   */
  List<PurchaseResponseDto> getStalePendingPurchases(Integer hoursThreshold);

  /**
   * Obtiene los ingresos por producto en un período
   *
   * @param startDate Fecha de inicio
   * @param endDate   Fecha de fin
   * @return Lista con ingresos por producto
   */
  List<Object> getRevenueByProduct(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Valida si una compra es posible
   *
   * @param request Datos de la compra
   * @return true si la compra es válida
   */
  boolean validatePurchaseRequest(PurchaseRequestDto request);

  /**
   * Calcula el precio total de una compra
   *
   * @param productId ID del producto
   * @param quantity  Cantidad a comprar
   * @return Precio total calculado
   */
  BigDecimal calculateTotalPrice(Long productId, Integer quantity);

  /**
   * Obtiene las compras recientes (últimas 24 horas por defecto)
   *
   * @param hours Número de horas hacia atrás
   * @return Lista de compras recientes
   */
  List<PurchaseResponseDto> getRecentPurchases(Integer hours);

  /**
   * Reintenta el procesamiento de una compra fallida
   *
   * @param purchaseId UUID de la compra
   * @return true si se pudo reintentar exitosamente
   */
  boolean retryFailedPurchase(UUID purchaseId);
}