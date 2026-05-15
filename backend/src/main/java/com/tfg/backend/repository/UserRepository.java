package com.tfg.backend.repository;

import com.tfg.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsernameAndIsBannedTrue(String username);
    boolean existsByUsernameAndIsDeletedTrue(String username);

    boolean existsByEmailAndIsBannedTrue(String email);
    boolean existsByEmailAndIsDeletedTrue(String email);

    List<User> findByRolesContainingAndAssignedTruckIsNull(String role);

    Long countByRolesContaining(String role);
    Long countByIsBannedTrue();
    Long countByIsDeletedTrue();

    List<User> findByRolesContaining(String role);

    @Query("SELECT u.username FROM User u JOIN u.roles r WHERE r = :role")
    List<String> findUsernamesByRole(@Param("role") String role);

    @Query("SELECT u.username FROM User u WHERE u.selectedShop.id = :shopId")
    List<String> findUsernamesBySelectedShopId(@Param("shopId") Long shopId);

    @Query("SELECT u.username FROM User u JOIN u.favouriteProducts p WHERE p.id = :productId")
    List<String> findUsernamesByFavouriteProductId(@Param("productId") Long productId);

    @Query("SELECT DISTINCT u.username FROM User u JOIN u.allOrderItems oi WHERE oi.product.id = :productId")
    List<String> findUsernamesByProductInCart(@Param("productId") Long productId);

    @Query("""
            SELECT u.username FROM User u
            JOIN u.favouriteProducts p
            WHERE u.selectedShop.id = :shopId
              AND p.id = :productId
              AND u.isBanned = false
              AND u.isDeleted = false
            """)
    List<String> findUsernamesByFavoritedProductAndSelectedShop(@Param("shopId") Long shopId,
                                                                @Param("productId") Long productId);

    @Query("""
            SELECT DISTINCT u.username FROM User u
            LEFT JOIN u.favouriteProducts fp
            LEFT JOIN u.allOrderItems oi
            WHERE u.selectedShop.id = :shopId
              AND (fp.id = :productId OR oi.product.id = :productId)
              AND u.isBanned = false
              AND u.isDeleted = false
            """)
    List<String> findUsernamesBySelectedShopAndProductInFavoritesOrCart(@Param("shopId") Long shopId,
                                                                       @Param("productId") Long productId);
}
