package org.opengroup.osdu.search.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.SwaggerDoc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpatialFilterBoundingBoxOpenApiCustomizerTest {

    @Test
    void buildByBoundingBoxSchema_shouldExposeCornerOrderingVariants() {
        ComposedSchema schema = SpatialFilterBoundingBoxOpenApiCustomizer.buildByBoundingBoxSchema(
                sampleByBoundingBoxSchema(), samplePointSchema());

        assertNotNull(schema);
        assertNotNull(schema.getOneOf());
        assertEquals(SpatialFilterBoundingBoxOpenApiCustomizer.BOUNDING_BOX_VARIANTS, schema.getOneOf().size());
        assertEquals(SwaggerDoc.QUERY_BY_BOUNDING_BOX_DESCRIPTION, schema.getDescription());

        Schema<?> originVariant = schema.getOneOf().stream()
                .filter(variant -> hasConstCorner(variant, "bottomRight", "latitude", 0)
                        && hasConstCorner(variant, "topLeft", "longitude", 0)
                        && hasMinimumCorner(variant, "topLeft", "latitude", 1)
                        && hasMinimumCorner(variant, "bottomRight", "longitude", 1))
                .findFirst()
                .orElseThrow();

        assertNotNull(originVariant);
    }

    @Test
    void buildByBoundingBoxSchema_shouldReturnNullForMissingSchema() {
        assertNull(SpatialFilterBoundingBoxOpenApiCustomizer.buildByBoundingBoxSchema(null, samplePointSchema()));
    }

    @Test
    void apply_shouldReplaceExistingByBoundingBoxSchema() {
        Schema<?> original = sampleByBoundingBoxSchema();
        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put(SpatialFilterBoundingBoxOpenApiCustomizer.BY_BOUNDING_BOX_SCHEMA_NAME, original);
        schemas.put(SpatialFilterBoundingBoxOpenApiCustomizer.POINT_SCHEMA_NAME, samplePointSchema());
        OpenAPI openAPI = new OpenAPI().components(new Components().schemas(schemas));

        SpatialFilterBoundingBoxOpenApiCustomizer.apply(openAPI);

        Schema<?> updated = openAPI.getComponents().getSchemas()
                .get(SpatialFilterBoundingBoxOpenApiCustomizer.BY_BOUNDING_BOX_SCHEMA_NAME);
        assertInstanceOf(ComposedSchema.class, updated);
        assertEquals(SpatialFilterBoundingBoxOpenApiCustomizer.BOUNDING_BOX_VARIANTS,
                ((ComposedSchema) updated).getOneOf().size());
    }

    private static Schema<Object> sampleByBoundingBoxSchema() {
        return new Schema<>()
                .type("object")
                .description(SwaggerDoc.QUERY_BY_BOUNDING_BOX_DESCRIPTION)
                .addRequiredItem("topLeft")
                .addRequiredItem("bottomRight")
                .addProperty("topLeft", new Schema<>().$ref("#/components/schemas/Point"))
                .addProperty("bottomRight", new Schema<>().$ref("#/components/schemas/Point"));
    }

    private static Schema<Object> samplePointSchema() {
        return new Schema<>()
                .type("object")
                .description("A geographic coordinate expressed as a latitude/longitude pair.")
                .addProperty("latitude", new NumberSchema().minimum(BigDecimal.valueOf(-90)).maximum(BigDecimal.valueOf(90)))
                .addProperty("longitude", new NumberSchema().minimum(BigDecimal.valueOf(-180)).maximum(BigDecimal.valueOf(180)));
    }

    private static boolean hasConstCorner(Schema<?> variant, String corner, String axis, int expectedConst) {
        NumberSchema axisSchema = cornerAxisSchema(variant, corner, axis);
        return axisSchema != null
                && axisSchema.getConst() != null
                && expectedConst == axisSchema.getConst().intValue();
    }

    private static boolean hasMinimumCorner(Schema<?> variant, String corner, String axis, int expectedMinimum) {
        NumberSchema axisSchema = cornerAxisSchema(variant, corner, axis);
        return axisSchema != null
                && axisSchema.getMinimum() != null
                && expectedMinimum == axisSchema.getMinimum().intValue();
    }

    private static NumberSchema cornerAxisSchema(Schema<?> variant, String corner, String axis) {
        if (!(variant.getProperties().get(corner) instanceof Schema<?> cornerSchema)) {
            return null;
        }
        if (!(cornerSchema.getProperties().get(axis) instanceof NumberSchema axisSchema)) {
            return null;
        }
        return axisSchema;
    }
}
