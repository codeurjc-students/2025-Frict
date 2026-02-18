package com.tfg.backend.repository;

import com.tfg.backend.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.name = :name")
    Optional<Category> findByNameWithChildren(@Param("name") String name);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.id = :id")
    Optional<Category> findByIdWithChildren(@Param("id") Long id);

    //Retrieves all roots category information and also their children information
    //Unlike List<Category> findByParentIsNull();, which makes N+1 queries to DB, all info is obtained in a single query
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL")
    List<Category> findRootsWithChildren();


    //Paged categories methods (avoids in-memory information processing and stops losing information due to JOIN FETCH and DB limits)
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
    Page<Category> findRoots(Pageable pageable);

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c IN :categories")
    List<Category> fetchChildrenFor(@Param("categories") List<Category> categories);
}