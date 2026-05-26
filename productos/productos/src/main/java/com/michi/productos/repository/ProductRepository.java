package com.michi.productos.repository;
import com.michi.productos.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByStatusFalse();
}
