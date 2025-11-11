package com.tfg.backend.repository;

import com.tfg.backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByReferenceCode(String referenceCode);

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.categories c
    WHERE
      (:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    AND
      (:categoryIds IS NULL OR c.id IN :categoryIds)
    """)
    Page<Product> findByFilters(@Param("searchTerm") String searchTerm,
                                @Param("categoryIds") List<Long> categoryIds,
                                Pageable pageable);
}
