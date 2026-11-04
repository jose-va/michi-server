package com.michi.productos.migration;

import com.michi.productos.model.Product;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FeaturedMigrationTest {

    @Test
    void backfillsOnlyDocumentsWithoutFeaturedField() throws Exception {
        MongoTemplate template = mock(MongoTemplate.class);
        new FeaturedMigration(template).run(null);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(template).updateMulti(query.capture(), update.capture(), eq(Product.class));

        assertTrue(query.getValue().getQueryObject().toJson().contains("$exists"));
        assertEquals(false, ((org.bson.Document) update.getValue().getUpdateObject().get("$set")).get("featured"));
    }

    private static Class<Product> eq(Class<Product> c) {
        return org.mockito.ArgumentMatchers.eq(c);
    }
}
