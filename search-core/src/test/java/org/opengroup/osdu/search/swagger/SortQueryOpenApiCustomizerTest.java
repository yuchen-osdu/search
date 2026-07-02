package org.opengroup.osdu.search.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.SwaggerDoc;
import org.opengroup.osdu.core.common.model.search.SortQuery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SortQueryOpenApiCustomizerTest {

    @Test
    void buildSortQuerySchema_shouldReuseGeneratedPropertiesWithMatchingArraySizes() {
        ComposedSchema schema = SortQueryOpenApiCustomizer.buildSortQuerySchema(sampleSortQuerySchema());

        assertNotNull(schema);
        assertNotNull(schema.getOneOf());
        assertEquals(SortQuery.MAX_SORT_CRITERIA, schema.getOneOf().size());
        assertEquals("Defines sort criteria.", schema.getDescription());

        ArraySchema fieldArray = extractArrayProperty(schema.getOneOf().get(0), "field");
        ArraySchema orderArray = extractArrayProperty(schema.getOneOf().get(0), "order");
        assertEquals(SwaggerDoc.SORT_FIELD_DESCRIPTION, fieldArray.getDescription());
        assertEquals(SwaggerDoc.SORT_ORDER_DESCRIPTION, orderArray.getDescription());
        assertEquals(1, fieldArray.getMinItems());
        assertEquals(1, fieldArray.getMaxItems());
        assertEquals(1, orderArray.getMinItems());
        assertEquals(1, orderArray.getMaxItems());
        assertEquals(Integer.valueOf(1), fieldArray.getItems().getMinLength());

        int lastIndex = SortQuery.MAX_SORT_CRITERIA - 1;
        fieldArray = extractArrayProperty(schema.getOneOf().get(lastIndex), "field");
        orderArray = extractArrayProperty(schema.getOneOf().get(lastIndex), "order");
        assertEquals(SortQuery.MAX_SORT_CRITERIA, fieldArray.getMinItems());
        assertEquals(SortQuery.MAX_SORT_CRITERIA, fieldArray.getMaxItems());
        assertEquals(SortQuery.MAX_SORT_CRITERIA, orderArray.getMinItems());
        assertEquals(SortQuery.MAX_SORT_CRITERIA, orderArray.getMaxItems());
    }

    @Test
    void buildSortQuerySchema_shouldHandleSpringdocStyleArrayProperties() {
        StringSchema fieldItem = new StringSchema();
        fieldItem.setMinLength(1);
        Schema<?> field = new Schema<>().type("array");
        field.setMinItems(1);
        field.setMaxItems(SortQuery.MAX_SORT_CRITERIA);
        field.setItems(fieldItem);
        Schema<?> order = new Schema<>().type("array");
        order.setMinItems(1);
        order.setMaxItems(SortQuery.MAX_SORT_CRITERIA);
        order.setItems(new StringSchema()._enum(List.of("ASC", "DESC")));

        Schema<Object> sortQuery = new Schema<>()
                .type("object")
                .addRequiredItem("field")
                .addRequiredItem("order")
                .addProperty("field", field)
                .addProperty("order", order);

        ComposedSchema schema = SortQueryOpenApiCustomizer.buildSortQuerySchema(sortQuery);

        assertNotNull(schema);
        assertEquals(SortQuery.MAX_SORT_CRITERIA, schema.getOneOf().size());
    }

    @Test
    void buildSortQuerySchema_shouldReturnNullForUnexpectedShape() {
        assertNull(SortQueryOpenApiCustomizer.buildSortQuerySchema(new Schema<>().type("object")));
    }

    @Test
    void apply_shouldReplaceExistingSortQuerySchema() {
        Schema<?> original = sampleSortQuerySchema();
        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put(SortQueryOpenApiCustomizer.SORT_QUERY_SCHEMA_NAME, original);
        OpenAPI openAPI = new OpenAPI().components(new Components().schemas(schemas));

        SortQueryOpenApiCustomizer.apply(openAPI);

        Schema<?> updated = openAPI.getComponents().getSchemas().get(SortQueryOpenApiCustomizer.SORT_QUERY_SCHEMA_NAME);
        assertInstanceOf(ComposedSchema.class, updated);
        assertEquals("Defines sort criteria.", updated.getDescription());
        assertEquals(SortQuery.MAX_SORT_CRITERIA, ((ComposedSchema) updated).getOneOf().size());
    }

    private static Schema<Object> sampleSortQuerySchema() {
        StringSchema fieldItem = new StringSchema();
        fieldItem.setMinLength(1);
        ArraySchema field = new ArraySchema();
        field.setDescription(SwaggerDoc.SORT_FIELD_DESCRIPTION);
        field.setMinItems(1);
        field.setMaxItems(SortQuery.MAX_SORT_CRITERIA);
        field.setItems(fieldItem);

        ArraySchema order = new ArraySchema();
        order.setDescription(SwaggerDoc.SORT_ORDER_DESCRIPTION);
        order.setMinItems(1);
        order.setMaxItems(SortQuery.MAX_SORT_CRITERIA);
        order.setItems(new StringSchema()._enum(List.of("ASC", "DESC")));

        return new Schema<>()
                .type("object")
                .description("Defines sort criteria.")
                .addRequiredItem("field")
                .addRequiredItem("order")
                .addProperty("field", field)
                .addProperty("order", order)
                .addProperty("filter", new ArraySchema().items(new StringSchema()));
    }

    private static ArraySchema extractArrayProperty(Schema variant, String propertyName) {
        assertInstanceOf(ArraySchema.class, variant.getProperties().get(propertyName));
        return (ArraySchema) variant.getProperties().get(propertyName);
    }
}
