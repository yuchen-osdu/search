package org.opengroup.osdu.search.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI cannot express cross-property ordering on bounding-box corners (top latitude must exceed
 * bottom latitude; left longitude must be less than right longitude). {@link SpatialFilter.ByBoundingBox}
 * documents the structure; this customizer wraps it in a {@code oneOf} of coordinate variants so
 * contract fuzzers stay aligned with {@link org.opengroup.osdu.core.common.model.search.validation.SpatialFilterValidator}.
 */
final class SpatialFilterBoundingBoxOpenApiCustomizer {

    static final String BY_BOUNDING_BOX_SCHEMA_NAME = SpatialFilter.ByBoundingBox.class.getSimpleName();
    static final String POINT_SCHEMA_NAME = Point.class.getSimpleName();

    static final int MIN_BOTTOM_LATITUDE = -90;
    static final int MAX_BOTTOM_LATITUDE = 80;
    static final int LATITUDE_STEP = 10;
    static final int MIN_LEFT_LONGITUDE = -180;
    static final int MAX_LEFT_LONGITUDE = 170;
    static final int LONGITUDE_STEP = 10;

    static final int LATITUDE_VARIANTS =
            (MAX_BOTTOM_LATITUDE - MIN_BOTTOM_LATITUDE) / LATITUDE_STEP + 1;
    static final int LONGITUDE_VARIANTS =
            (MAX_LEFT_LONGITUDE - MIN_LEFT_LONGITUDE) / LONGITUDE_STEP + 1;
    static final int BOUNDING_BOX_VARIANTS = LATITUDE_VARIANTS * LONGITUDE_VARIANTS;

    private SpatialFilterBoundingBoxOpenApiCustomizer() {
    }

    static void apply(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null || components.getSchemas() == null) {
            return;
        }

        Schema<?> existing = components.getSchemas().get(BY_BOUNDING_BOX_SCHEMA_NAME);
        Schema<?> pointTemplate = components.getSchemas().get(POINT_SCHEMA_NAME);
        ComposedSchema aligned = buildByBoundingBoxSchema(existing, pointTemplate);
        if (aligned != null) {
            components.addSchemas(BY_BOUNDING_BOX_SCHEMA_NAME, aligned);
        }
    }

    static ComposedSchema buildByBoundingBoxSchema(Schema<?> existing, Schema<?> pointTemplate) {
        if (existing == null) {
            return null;
        }

        List<Schema> variants = new ArrayList<>(BOUNDING_BOX_VARIANTS);
        for (int bottomLat = MIN_BOTTOM_LATITUDE; bottomLat <= MAX_BOTTOM_LATITUDE; bottomLat += LATITUDE_STEP) {
            for (int leftLon = MIN_LEFT_LONGITUDE; leftLon <= MAX_LEFT_LONGITUDE; leftLon += LONGITUDE_STEP) {
                variants.add(buildVariant(bottomLat, leftLon, existing, pointTemplate));
            }
        }

        ComposedSchema composed = new ComposedSchema();
        composed.setDescription(existing.getDescription());
        composed.setExample(existing.getExample());
        composed.setOneOf(variants);
        return composed;
    }

    private static Schema<Object> buildVariant(int bottomLat, int leftLon, Schema<?> template, Schema<?> pointTemplate) {
        Schema<Object> variant = new Schema<>()
                .type("object")
                .required(List.of("topLeft", "bottomRight"))
                .addProperty("topLeft", cornerPoint(
                        pointTemplate,
                        template,
                        "topLeft",
                        fixedCoordinate(bottomLat + 1, 90),
                        fixedCoordinate(leftLon, leftLon)))
                .addProperty("bottomRight", cornerPoint(
                        pointTemplate,
                        template,
                        "bottomRight",
                        fixedCoordinate(bottomLat, bottomLat),
                        fixedCoordinate(leftLon + 1, 180)));

        return variant;
    }

    private static Schema<?> cornerPoint(
            Schema<?> pointTemplate,
            Schema<?> boundingBoxTemplate,
            String cornerName,
            NumberSchema latitude,
            NumberSchema longitude) {
        Schema<?> cornerTemplate = extractNestedProperty(boundingBoxTemplate, cornerName);
        Schema<Object> point = new Schema<>().type("object").required(List.of("latitude", "longitude"));
        if (cornerTemplate != null && cornerTemplate.getDescription() != null) {
            point.setDescription(cornerTemplate.getDescription());
        } else if (pointTemplate != null) {
            point.setDescription(pointTemplate.getDescription());
        }
        copyCoordinateDescription(latitude, pointTemplate, cornerTemplate, "latitude");
        copyCoordinateDescription(longitude, pointTemplate, cornerTemplate, "longitude");
        point.addProperty("latitude", latitude);
        point.addProperty("longitude", longitude);
        return point;
    }

    private static void copyCoordinateDescription(
            NumberSchema coordinate,
            Schema<?> pointTemplate,
            Schema<?> cornerTemplate,
            String axis) {
        Schema<?> source = cornerTemplate != null
                ? extractNestedProperty(cornerTemplate, axis)
                : extractNestedProperty(pointTemplate, axis);
        if (source != null) {
            coordinate.setDescription(source.getDescription());
            coordinate.setExample(source.getExample());
        }
    }

    private static Schema<?> extractNestedProperty(Schema<?> parent, String property) {
        if (parent == null || parent.getProperties() == null) {
            return null;
        }
        Object prop = parent.getProperties().get(property);
        return prop instanceof Schema<?> schema ? schema : null;
    }

    private static NumberSchema fixedCoordinate(int minimum, int maximum) {
        NumberSchema schema = new NumberSchema();
        if (minimum == maximum) {
            schema._const(BigDecimal.valueOf(minimum));
        } else {
            schema.setMinimum(BigDecimal.valueOf(minimum));
            schema.setMaximum(BigDecimal.valueOf(maximum));
        }
        return schema;
    }
}
