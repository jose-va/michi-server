package com.michi.productos.mapper;

import com.michi.productos.dto.ProductDTO;
import com.michi.productos.model.Category;
import com.michi.productos.model.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring", imports = {Category.class})
public interface ProductMapper {

    ProductDTO toDto(Product product);
    Product toEntity(ProductDTO productDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ProductDTO dto, @MappingTarget Product product);
}
