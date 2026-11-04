package com.michi.productos.service;

import com.michi.productos.dto.ProductDTO;
import com.michi.productos.mapper.ProductMapper;
import com.michi.productos.model.Allergen;
import com.michi.productos.model.Category;
import com.michi.productos.model.Product;
import com.michi.productos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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

    private static final List<Category> CATEGORY_ORDER = List.of(
            Category.HOT_STARTERS,
            Category.KATSUMAKI,
            Category.SPECIAL_MAKI,
            Category.NIGIRI_SUSHI,
            Category.SOGIZUKURI,
            Category.GUNKAN_SUSHI,
            Category.ZUKE_DON,
            Category.TEMAKI,
            Category.HOSOMAKI,
            Category.URAMAKI,
            Category.FUTOMAKI,
            Category.TARTAR,
            Category.HOT_MAKI,
            Category.HOT_DISHES,
            Category.FRESH_STARTERS,
            Category.COMBOS,
            Category.DESSERTS
    );

    private static int categoryRank(Category category) {
        int index = CATEGORY_ORDER.indexOf(category);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static final Comparator<Product> PRODUCT_ORDER = Comparator
            .comparing((Product p) -> !p.isFeatured())
            .thenComparingInt(p -> categoryRank(p.getCategory()))
            .thenComparingInt(Product::getPosition)
            .thenComparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    public Page<ProductDTO> findAll(Pageable pageable) {
        List<Product> sorted = productRepository.findAll()
                .stream()
                .sorted(PRODUCT_ORDER)
                .toList();

        int start = (int) Math.min(pageable.getOffset(), sorted.size());
        int end = Math.min(start + pageable.getPageSize(), sorted.size());

        List<ProductDTO> content = sorted.subList(start, end)
                .stream()
                .map(productMapper::toDto)
                .toList();

        return new PageImpl<>(content, pageable, sorted.size());
    }

    public List<ProductDTO> find(String name, String category, List<String> allergens) {
        return productRepository.findAll()
                .stream()
                .filter(p -> name == null || p.getName().toLowerCase().contains(name.toLowerCase()))
                .filter(p -> category == null || p.getCategory() == Category.valueOf(category.toUpperCase()))
                .filter(p -> allergens == null || allergens.stream()
                        .map(Allergen::valueOf)
                        .noneMatch(p.getAllergens()::contains))
                .map(productMapper::toDto)
                .toList();
    }

    public List<ProductDTO> findFeatured(String name, List<String> allergens) {
        return find(name, null, allergens)
                .stream()
                .filter(p -> Boolean.TRUE.equals(p.featured()))
                .toList();
    }

    public ProductDTO create(ProductDTO dto) {
        Product product = productMapper.toEntity(dto);
        product.setStatus(true);

        int next = productRepository
                .findFirstByCategoryOrderByPositionDesc(product.getCategory())
                .map(p -> p.getPosition() + 1)
                .orElse(1);
        product.setPosition(next);

        Product newProduct = productRepository.save(product);
        return productMapper.toDto(newProduct);
    }

    public ProductDTO update(ProductDTO dto, String id){
        Product product= productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se ha podido actualizar el producto " + id));

        productMapper.updateEntityFromDto(dto, product);
        product.setFeatured(Boolean.TRUE.equals(dto.featured()));
        product.setStatus(true);

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
