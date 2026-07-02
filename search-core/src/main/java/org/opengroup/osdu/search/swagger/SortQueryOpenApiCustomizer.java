package org.opengroup.osdu.search.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.opengroup.osdu.core.common.model.search.SortQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI cannot express that sibling arrays must share the same length. {@link SortQuery} defines
 * item constraints, descriptions, and bounds; this customizer only wraps those generated properties
 * in a {@code oneOf} of fixed-size variants so contract fuzzers keep {@code field} and
 * {@code order} aligned.
 */
final class SortQueryOpenApiCustomizer {

    static final String SORT_QUERY_SCHEMA_NAME = SortQuery.class.getSimpleName();

    private SortQueryOpenApiCustomizer() {
    }

    static void apply(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null || components.getSchemas() == null) {
            return;
        }

        Schema<?> existing = components.getSchemas().get(SORT_QUERY_SCHEMA_NAME);
        ComposedSchema aligned = buildSortQuerySchema(existing);
        if (aligned != null) {
            components.addSchemas(SORT_QUERY_SCHEMA_NAME, aligned);
        }
    }

    static ComposedSchema buildSortQuerySchema(Schema<?> existing) {
        if (existing == null || existing.getProperties() == null) {
            return null;
        }

        Schema<?> fieldProperty = existing.getProperties().get("field");
        Schema<?> orderProperty = existing.getProperties().get("order");
        if (!isArraySchema(fieldProperty) || !isArraySchema(orderProperty)) {
            return null;
        }

        Schema<?> filterProperty = existing.getProperties().get("filter");
        List<String> required = existing.getRequired();
        if (required == null || required.isEmpty()) {
            required = List.of("field", "order");
        }

        List<Schema> variants = new ArrayList<>(SortQuery.MAX_SORT_CRITERIA);
        for (int size = 1; size <= SortQuery.MAX_SORT_CRITERIA; size++) {
            Schema<Object> variant = new Schema<>()
                    .type("object")
                    .required(required)
                    .addProperty("field", fixedSizeArray(fieldProperty, size))
                    .addProperty("order", fixedSizeArray(orderProperty, size));
            if (filterProperty != null) {
                variant.addProperty("filter", filterProperty);
            }
            variants.add(variant);
        }

        ComposedSchema composed = new ComposedSchema();
        composed.setDescription(existing.getDescription());
        composed.setOneOf(variants);
        return composed;
    }

    private static boolean isArraySchema(Schema<?> schema) {
        return schema instanceof ArraySchema
                || "array".equals(schema.getType())
                || (schema.getTypes() != null && schema.getTypes().contains("array"));
    }

    private static ArraySchema fixedSizeArray(Schema<?> template, int size) {
        ArraySchema array = new ArraySchema();
        array.setDescription(template.getDescription());
        array.setExample(template.getExample());
        array.setItems(template.getItems());
        array.setMinItems(size);
        array.setMaxItems(size);
        return array;
    }
}
