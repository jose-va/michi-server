package com.michi.productos.controller;

import com.michi.productos.dto.ProductDTO;
import com.michi.productos.service.ProductService;
import com.michi.productos.uber.service.UberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final UberService uberService;

    @GetMapping("/all")
    public List<ProductDTO> findAll() {
        return productService.all();
    }

    @GetMapping("/{id}")
    public ProductDTO findByID(@PathVariable String id) {
        return productService.findByID(id);
    }

    @GetMapping
    public Page<ProductDTO> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return productService.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/search")
    public List<ProductDTO> find(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> allergens) {
        return productService.find(name, category, allergens);
    }

    @PostMapping
    public ProductDTO create(
            @RequestBody ProductDTO productDTO,
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        return productService.create(productDTO);
    }

    @PutMapping("/{id}")
    public ProductDTO update(
            @RequestBody ProductDTO productDTO,
            @PathVariable String id,
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        return productService.update(productDTO, id);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable String id,
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        productService.delete(id);
    }

    @PatchMapping("/{id}/deactivate")
    public ProductDTO deactivateProduct(
            @PathVariable String id,
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        return productService.deactivateProduct(id);
    }

    @PatchMapping("/{id}/activate")
    public ProductDTO activateProduct(
            @PathVariable String id,
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        return productService.activateProduct(id);
    }

    @PatchMapping
    public List<ProductDTO> activateAll(
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        return productService.activateAllProduct();
    }

    @PostMapping("/uber")
    public ResponseEntity<String> sync(
            @RequestHeader("User-Role") String role) {
        checkAdmin(role);
        uberService.sync();
        return ResponseEntity.ok("¡La sincronización con Michi Sushi Granada ha sido un exito!");
    }

    private void checkAdmin(String role) {
        if (!role.equals("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso exclusivo a administradores");
        }
    }
}
