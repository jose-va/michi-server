package com.michi.productos.repository;
import com.michi.productos.model.Category;
import com.michi.productos.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByStatusFalse();

    Optional<Product> findFirstByCategoryOrderByPositionDesc(Category category);
}
