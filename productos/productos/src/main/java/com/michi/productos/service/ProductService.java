package com.michi.productos.service;

import com.michi.productos.dto.ProductDTO;
import com.michi.productos.mapper.ProductMapper;
import com.michi.productos.model.Allergen;
import com.michi.productos.model.Category;
import com.michi.productos.model.Product;
import com.michi.productos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;

    public List<ProductDTO> all() {
        return productRepository
                .findAll()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    public ProductDTO findByID(String id){
        return productRepository.findById(id)
                .map(productMapper::toDto)
                .orElseThrow(() -> new RuntimeException("No se ha encontrado al producto " + id));
    }

    public Page<ProductDTO> findAll(Pageable pageable) {
        return productRepository
                .findAll(pageable)
                .map(productMapper::toDto);
    }

    public List<ProductDTO> find(String name, String category, List<String> allergens) {
        return productRepository.findAll()
                .stream()
                .filter(p -> name == null || p.getName().toLowerCase().contains(name.toLowerCase()))
                .filter(p -> category == null || p.getCategory() == Category.valueOf(category.toUpperCase()))
                .filter(p -> allergens == null || new HashSet<>(p.getAllergens()).containsAll(
                        allergens.stream().map(Allergen::valueOf).toList()
                ))
                .map(productMapper::toDto)
                .toList();
    }

    public ProductDTO create(ProductDTO dto){
        Product product= productMapper.toEntity(dto);
        product.setStatus(true);

        Product newProduct= productRepository.save(product);
        return productMapper.toDto(newProduct);
    }

    public ProductDTO update(ProductDTO dto, String id){
        Product product= productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se ha podido actualizar el producto " + id));
        product.setStatus(true);

        productMapper.updateEntityFromDto(dto, product);

        Product updatedProduct= productRepository.save(product);
        return productMapper.toDto(updatedProduct);
    }

    public void delete(String id){
        productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se ha podido eliminar el producto " + id));

        productRepository.deleteById(id);
    }

    public ProductDTO deactivateProduct(String id){
        Product product= productRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("No se ha podido deshabilitar el producto " + id));

        product.setStatus(false);

        Product updatedProduct= productRepository.save(product);
        return productMapper.toDto(updatedProduct);
    }

    public ProductDTO activateProduct(String id){
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("No se ha podido habilitar el producto " + id));

        product.setStatus(true);

        Product updatedProduct = productRepository.save(product);
        return productMapper.toDto(updatedProduct);
    }

    public List<ProductDTO> activateAllProduct(){
        List<Product> products = productRepository.findByStatusFalse();

        products.forEach(product -> product.setStatus(true));

        return productRepository.saveAll(products)
                .stream()
                .map(productMapper::toDto)
                .toList();
    }
}
