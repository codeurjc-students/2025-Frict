package com.tfg.backend.utils;

import com.tfg.backend.dto.SpecFilterDTO;
import com.tfg.backend.model.Category;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.ProductSpec;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ProductSpecifications {

    public static Specification<Product> hasSearchTerm(String term) {
        return (root, query, cb) -> term == null || term.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + term.toLowerCase() + "%");
    }

    public static Specification<Product> hasCategories(List<Long> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return cb.conjunction();
            query.distinct(true);
            Join<Product, Category> join = root.join("categories", JoinType.LEFT);
            return join.get("id").in(ids);
        };
    }

    /**
     * El producto debe tener una ProductSpec con name=filter.name()
     * y al menos un valor de su lista en filter.values().
     */
    public static Specification<Product> hasSpec(SpecFilterDTO filter) {
        return (root, query, cb) -> {
            Subquery<Long> sq = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<ProductSpec> specRoot = sq.from(ProductSpec.class);
            Join<ProductSpec, String> valueJoin = specRoot.join("values");
            sq.select(specRoot.get("product").get("id"))
              .where(cb.and(
                  cb.equal(specRoot.get("product").get("id"), root.get("id")),
                  cb.equal(specRoot.get("name"), filter.name()),
                  valueJoin.in(filter.values())
              ));
            return cb.exists(sq);
        };
    }
}
