package com.example.products.repository;

import com.example.products.model.Product;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository para la entidad Product
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  /**
   * Busca un producto por ID y que esté activo
   */
  Optional<Product> findByIdAndActiveTrue(Long id);

  /**
   * Busca productos activos
   */
  Page<Product> findByActiveTrue(Pageable pageable);

  /**
   * Busca productos por nombre (case-insensitive) y que estén activos
   */
  Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

  /**
   * Busca productos en un rango de precios y que estén activos
   */
  Page<Product> findByPriceBetweenAndActiveTrue(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

  /**
   * Verifica si existe un producto con el nombre dado (excluyendo un ID específico)
   */
  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

  /**
   * Verifica si existe un producto con el nombre dado
   */
  boolean existsByNameIgnoreCase(String name);

  /**
   * Busca productos por múltiples criterios usando JPQL
   */
  @Query("SELECT p FROM Product p WHERE " +
      "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
      "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
      "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
      "p.active = true")
  Page<Product> findProductsByCriteria(
      @Param("name") String name,
      @Param("minPrice") BigDecimal minPrice,
      @Param("maxPrice") BigDecimal maxPrice,
      Pageable pageable
  );

  /**
   * Encuentra los productos más caros (top N)
   */
  @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.price DESC")
  List<Product> findTopExpensiveProducts(Pageable pageable);

  /**
   * Cuenta productos activos
   */
  long countByActiveTrue();

  /**
   * Busca productos creados recientemente
   */
  @Query("SELECT p FROM Product p WHERE p.active = true AND p.createdAt >= :recentDate ORDER BY p.createdAt DESC")
  List<Product> findRecentProducts(@Param("days") LocalDate recentDate);

  /**
   * Obtiene estadísticas básicas de precios
   */
  @Query("SELECT MIN(p.price) as minPrice, MAX(p.price) as maxPrice, AVG(p.price) as avgPrice, COUNT(p) as totalProducts " +
      "FROM Product p WHERE p.active = true")
  ProductPriceStats getPriceStatistics();

  /**
   * Interface para proyección de estadísticas de precios
   */
  interface ProductPriceStats {
    BigDecimal getMinPrice();
    BigDecimal getMaxPrice();
    BigDecimal getAvgPrice();
    Long getTotalProducts();
  }
}