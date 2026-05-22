package com.michi.productos.mapper;

import com.michi.productos.dto.ProductDTO;
import com.michi.productos.model.Category;
import com.michi.productos.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", imports = {Category.class})
public interface ProductMapper {

    ProductDTO toDto(Product product);
    Product toEntity(ProductDTO productDTO);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(ProductDTO productDTO, @MappingTarget Product product);
}
