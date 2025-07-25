package com.example.inventory.repository;

import com.example.inventory.model.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Repository para la entidad Inventory
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

  /**
   * Busca inventario por ID de producto
   */
  Optional<Inventory> findByProductId(Long productId);

  /**
   * Busca inventario por ID de producto con bloqueo pesimista para operaciones concurrentes
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
  Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);

  /**
   * Verifica si existe inventario para un producto
   */
  boolean existsByProductId(Long productId);

  /**
   * Busca productos con stock bajo (cantidad <= nivel mínimo)
   */
  @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.minStockLevel ORDER BY i.quantity ASC")
  List<Inventory> findLowStockProducts();

  /**
   * Busca productos fuera de stock
   */
  @Query("SELECT i FROM Inventory i WHERE i.quantity = 0 ORDER BY i.updatedAt DESC")
  List<Inventory> findOutOfStockProducts();

  /**
   * Busca productos con stock disponible mayor a la cantidad especificada
   */
  @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) >= :requiredQuantity")
  List<Inventory> findProductsWithAvailableStock(@Param("requiredQuantity") Integer requiredQuantity);

  /**
   * Obtiene inventarios paginados
   */
  Page<Inventory> findAllByOrderByUpdatedAtDesc(Pageable pageable);

  /**
   * Busca inventarios por rango de cantidad
   */
  @Query("SELECT i FROM Inventory i WHERE i.quantity BETWEEN :minQuantity AND :maxQuantity ORDER BY i.quantity DESC")
  Page<Inventory> findByQuantityBetween(@Param("minQuantity") Integer minQuantity,
                                        @Param("maxQuantity") Integer maxQuantity,
                                        Pageable pageable);

  /**
   * Actualiza la cantidad de un producto de forma atómica
   */
  @Modifying
  @Query("UPDATE Inventory i SET i.quantity = :newQuantity, i.updatedAt = CURRENT_TIMESTAMP " +
      "WHERE i.productId = :productId AND i.quantity = :expectedCurrentQuantity")
  int updateQuantityAtomically(@Param("productId") Long productId,
                               @Param("newQuantity") Integer newQuantity,
                               @Param("expectedCurrentQuantity") Integer expectedCurrentQuantity);

  /**
   * Reserva stock de forma atómica
   */
  @Modifying
  @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity + :quantity, " +
      "i.updatedAt = CURRENT_TIMESTAMP " +
      "WHERE i.productId = :productId AND (i.quantity - i.reservedQuantity) >= :quantity")
  int reserveStockAtomically(@Param("productId") Long productId, @Param("quantity") Integer quantity);

  /**
   * Libera stock reservado de forma atómica
   */
  @Modifying
  @Query("UPDATE Inventory i SET i.reservedQuantity = CASE " +
      "WHEN i.reservedQuantity >= :quantity THEN i.reservedQuantity - :quantity " +
      "ELSE 0 END, i.updatedAt = CURRENT_TIMESTAMP " +
      "WHERE i.productId = :productId")
  int releaseReservedStockAtomically(@Param("productId") Long productId, @Param("quantity") Integer quantity);

  /**
   * Confirma compra reduciendo stock físico y reservado de forma atómica
   */
  @Modifying
  @Query("UPDATE Inventory i SET i.quantity = i.quantity - :quantity, " +
      "i.reservedQuantity = i.reservedQuantity - :quantity, i.updatedAt = CURRENT_TIMESTAMP " +
      "WHERE i.productId = :productId AND i.quantity >= :quantity AND i.reservedQuantity >= :quantity")
  int confirmPurchaseAtomically(@Param("productId") Long productId, @Param("quantity") Integer quantity);

  /**
   * Obtiene estadísticas generales de inventario
   */
  @Query("SELECT COUNT(*) as totalProducts, " +
      "COUNT(CASE WHEN i.quantity > 0 THEN 1 END) as productsInStock, " +
      "COUNT(CASE WHEN i.quantity = 0 THEN 1 END) as productsOutOfStock, " +
      "COUNT(CASE WHEN i.quantity <= i.minStockLevel AND i.quantity > 0 THEN 1 END) as productsLowStock, " +
      "COALESCE(SUM(i.quantity), 0) as totalQuantity, " +
      "COALESCE(SUM(i.reservedQuantity), 0) as totalReserved " +
      "FROM Inventory i")
  InventoryStatistics getInventoryStatistics();

  /**
   * Busca productos por múltiples filtros
   */
  @Query("SELECT i FROM Inventory i WHERE " +
      "(:hasStock IS NULL OR (:hasStock = true AND i.quantity > 0) OR (:hasStock = false AND i.quantity = 0)) AND " +
      "(:isLowStock IS NULL OR (:isLowStock = true AND i.quantity <= i.minStockLevel) OR (:isLowStock = false AND i.quantity > i.minStockLevel)) AND " +
      "(:minQuantity IS NULL OR i.quantity >= :minQuantity) AND " +
      "(:maxQuantity IS NULL OR i.quantity <= :maxQuantity) " +
      "ORDER BY i.updatedAt DESC")
  Page<Inventory> findByFilters(@Param("hasStock") Boolean hasStock,
                                @Param("isLowStock") Boolean isLowStock,
                                @Param("minQuantity") Integer minQuantity,
                                @Param("maxQuantity") Integer maxQuantity,
                                Pageable pageable);

  /**
   * Cuenta productos con stock disponible para venta
   */
  @Query("SELECT COUNT(i) FROM Inventory i WHERE (i.quantity - i.reservedQuantity) > 0")
  long countProductsWithAvailableStock();

  /**
   * Obtiene los productos más vendidos basado en movimientos de inventario
   */
  @Query("SELECT i.productId, SUM(CASE WHEN im.quantityChange < 0 THEN ABS(im.quantityChange) ELSE 0 END) as totalSold " +
      "FROM Inventory i LEFT JOIN InventoryMovement im ON i.productId = im.productId " +
      "WHERE im.movementType = 'PURCHASE' OR im.movementType = 'STOCK_OUT' " +
      "GROUP BY i.productId " +
      "ORDER BY totalSold DESC")
  List<Object> findTopSellingProducts(Pageable pageable);

  /**
   * Interface para proyección de estadísticas de inventario
   */
  interface InventoryStatistics {
    Long getTotalProducts();
    Long getProductsInStock();
    Long getProductsOutOfStock();
    Long getProductsLowStock();
    Long getTotalQuantity();
    Long getTotalReserved();
  }
}