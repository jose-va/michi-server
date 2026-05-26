package com.michi.productos.mapper;

import com.michi.productos.dto.ProductDTO;
import com.michi.productos.model.Allergen;
import com.michi.productos.model.Category;
import com.michi.productos.model.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-22T20:51:57+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Microsoft)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductDTO toDto(Product product) {
        if ( product == null ) {
            return null;
        }

        String id = null;
        String name = null;
        String description = null;
        BigDecimal price = null;
        Category category = null;
        Allergen[] allergens = null;
        String japaneseName = null;
        int pieces = 0;
        boolean status = false;

        id = product.getId();
        name = product.getName();
        description = product.getDescription();
        price = BigDecimal.valueOf( product.getPrice() );
        category = product.getCategory();
        allergens = allergenListToAllergenArray( product.getAllergens() );
        japaneseName = product.getJapaneseName();
        pieces = product.getPieces();
        status = product.isStatus();

        ProductDTO productDTO = new ProductDTO( id, name, description, price, category, allergens, japaneseName, pieces, status );

        return productDTO;
    }

    @Override
    public Product toEntity(ProductDTO productDTO) {
        if ( productDTO == null ) {
            return null;
        }

        Product product = new Product();

        product.setId( productDTO.id() );
        product.setName( productDTO.name() );
        product.setJapaneseName( productDTO.japaneseName() );
        product.setDescription( productDTO.description() );
        product.setCategory( productDTO.category() );
        product.setAllergens( allergenArrayToAllergenList( productDTO.allergens() ) );
        product.setPieces( productDTO.pieces() );
        if ( productDTO.price() != null ) {
            product.setPrice( productDTO.price().doubleValue() );
        }
        product.setStatus( productDTO.status() );

        return product;
    }

    @Override
    public void updateEntityFromDto(ProductDTO productDTO, Product product) {
        if ( productDTO == null ) {
            return;
        }

        product.setName( productDTO.name() );
        product.setJapaneseName( productDTO.japaneseName() );
        product.setDescription( productDTO.description() );
        product.setCategory( productDTO.category() );
        if ( product.getAllergens() != null ) {
            List<Allergen> list = allergenArrayToAllergenList( productDTO.allergens() );
            if ( list != null ) {
                product.getAllergens().clear();
                product.getAllergens().addAll( list );
            }
            else {
                product.setAllergens( null );
            }
        }
        else {
            List<Allergen> list = allergenArrayToAllergenList( productDTO.allergens() );
            if ( list != null ) {
                product.setAllergens( list );
            }
        }
        product.setPieces( productDTO.pieces() );
        if ( productDTO.price() != null ) {
            product.setPrice( productDTO.price().doubleValue() );
        }
        product.setStatus( productDTO.status() );
    }

    protected Allergen[] allergenListToAllergenArray(List<Allergen> list) {
        if ( list == null ) {
            return null;
        }

        Allergen[] allergenTmp = new Allergen[list.size()];
        int i = 0;
        for ( Allergen allergen : list ) {
            allergenTmp[i] = allergen;
            i++;
        }

        return allergenTmp;
    }

    protected List<Allergen> allergenArrayToAllergenList(Allergen[] allergenArray) {
        if ( allergenArray == null ) {
            return null;
        }

        List<Allergen> list = new ArrayList<Allergen>( allergenArray.length );
        for ( Allergen allergen : allergenArray ) {
            list.add( allergen );
        }

        return list;
    }
}
