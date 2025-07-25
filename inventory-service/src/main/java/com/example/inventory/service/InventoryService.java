package com.example.inventory.service;

import com.example.inventory.controller.dto.InventoryDto;
import com.example.inventory.controller.dto.UpdateInventoryRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interfaz para el servicio de inventario
 */
public interface InventoryService {

  /**
   * Obtiene el inventario de un producto específico
   *
   * @param productId ID del producto
   * @return InventoryDto con la información del inventario
   */
  InventoryDto getInventoryByProductId(Long productId);

  /**
   * Actualiza el inventario de un producto
   *
   * @param productId ID del producto
   * @param request   Datos de actualización
   * @return InventoryDto actualizado
   */
  InventoryDto updateInventory(Long productId, UpdateInventoryRequestDto request);

  /**
   * Obtiene todos los inventarios con paginación
   *
   * @param pageable Información de paginación
   * @return Página con inventarios
   */
  Page<InventoryDto> getAllInventories(Pageable pageable);

  /**
   * Obtiene inventarios con filtros específicos
   *
   * @param hasStock     Filtrar por productos con stock
   * @param isLowStock   Filtrar por productos con stock bajo
   * @param minQuantity  Cantidad mínima
   * @param maxQuantity  Cantidad máxima
   * @param pageable     Información de paginación
   * @return Página con inventarios filtrados
   */
  Page<InventoryDto> getInventoriesWithFilters(Boolean hasStock, Boolean isLowStock,
                                               Integer minQuantity, Integer maxQuantity,
                                               Pageable pageable);

  /**
   * Obtiene productos con stock bajo
   *
   * @return Lista de inventarios con stock bajo
   */
  List<InventoryDto> getLowStockProducts();

  /**
   * Obtiene productos fuera de stock
   *
   * @return Lista de inventarios sin stock
   */
  List<InventoryDto> getOutOfStockProducts();

  /**
   * Verifica si hay stock disponible para una cantidad específica
   *
   * @param productId ID del producto
   * @param quantity  Cantidad requerida
   * @return true si hay stock disponible, false en caso contrario
   */
  boolean checkStockAvailability(Long productId, Integer quantity);

  /**
   * Reserva stock para una compra
   *
   * @param productId ID del producto
   * @param quantity  Cantidad a reservar
   * @return true si se pudo reservar, false en caso contrario
   */
  boolean reserveStock(Long productId, Integer quantity);

  /**
   * Libera stock reservado
   *
   * @param productId ID del producto
   * @param quantity  Cantidad a liberar
   * @return true si se pudo liberar, false en caso contrario
   */
  boolean releaseReservedStock(Long productId, Integer quantity);

  /**
   * Confirma una compra reduciendo el stock físico
   *
   * @param productId ID del producto
   * @param quantity  Cantidad comprada
   * @return true si se pudo confirmar, false en caso contrario
   */
  boolean confirmPurchase(Long productId, Integer quantity);

  /**
   * Crea un nuevo registro de inventario para un producto
   *
   * @param productId        ID del producto
   * @param initialQuantity  Cantidad inicial
   * @param minStockLevel    Nivel mínimo de stock
   * @param maxStockLevel    Nivel máximo de stock
   * @return InventoryDto creado
   */
  InventoryDto createInventory(Long productId, Integer initialQuantity,
                               Integer minStockLevel, Integer maxStockLevel);

  /**
   * Elimina (desactiva) un inventario
   *
   * @param productId ID del producto
   */
  void deleteInventory(Long productId);

  /**
   * Obtiene estadísticas generales del inventario
   *
   * @return Estadísticas del inventario
   */
  Object getInventoryStatistics();

  /**
   * Ajusta la cantidad de inventario con razón de auditoría
   *
   * @param productId   ID del producto
   * @param newQuantity Nueva cantidad
   * @param reason      Razón del ajuste
   * @param referenceId ID de referencia
   * @return InventoryDto actualizado
   */
  InventoryDto adjustInventory(Long productId, Integer newQuantity, String reason, String referenceId);

  /**
   * Obtiene los productos más vendidos
   *
   * @param limit Número máximo de productos a retornar
   * @return Lista de productos más vendidos
   */
  List<Object> getTopSellingProducts(Integer limit);

  /**
   * Sincroniza el inventario con información del servicio de productos
   *
   * @param productId ID del producto a sincronizar
   * @return InventoryDto sincronizado
   */
  InventoryDto synchronizeWithProductService(Long productId);
}