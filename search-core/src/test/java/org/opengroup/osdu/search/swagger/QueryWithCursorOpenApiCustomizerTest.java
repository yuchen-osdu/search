package org.opengroup.osdu.search.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.SwaggerDoc;
import org.opengroup.osdu.core.common.model.storage.validation.ValidationDoc;
import org.opengroup.osdu.search.api.SearchApi;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryWithCursorOpenApiCustomizerTest {

    @Test
    void isQueryWithCursorHandler_shouldMatchCursorEndpointsOnly() throws Exception {
        HandlerMethod queryWithCursor = handlerMethod("queryWithCursor");
        HandlerMethod closeCursor = handlerMethod("closeCursor");
        HandlerMethod queryRecords = handlerMethod("queryRecords");

        assertTrue(QueryWithCursorOpenApiCustomizer.isQueryWithCursorHandler(queryWithCursor));
        assertTrue(QueryWithCursorOpenApiCustomizer.isQueryWithCursorHandler(closeCursor));
        assertFalse(QueryWithCursorOpenApiCustomizer.isQueryWithCursorHandler(queryRecords));
    }

    @Test
    void customizeOperation_shouldAlignSearchAfterAndPathCursor() throws Exception {
        Operation operation = new Operation()
                .parameters(List.of(
                        new Parameter().name("search_after").in("query").schema(new StringSchema()),
                        new Parameter().name("cursor").in("path").schema(new StringSchema()),
                        new Parameter().name("data-partition-id").in("header").schema(new StringSchema())));

        QueryWithCursorOpenApiCustomizer.customizeOperation(operation, handlerMethod("closeCursor"));

        Parameter searchAfter = operation.getParameters().get(0);
        assertInstanceOf(BooleanSchema.class, searchAfter.getSchema());
        assertEquals(false, ((BooleanSchema) searchAfter.getSchema()).getDefault());

        Parameter cursor = operation.getParameters().get(1);
        StringSchema cursorSchema = (StringSchema) cursor.getSchema();
        assertEquals(ValidationDoc.CURSOR_PATTERN, cursorSchema.getPattern());
        assertEquals(ValidationDoc.CURSOR_LENGTH, cursorSchema.getMinLength());
        assertEquals(ValidationDoc.CURSOR_LENGTH, cursorSchema.getMaxLength());
        assertEquals(SwaggerDoc.CURSOR_EXAMPLE, cursorSchema.getExample());
    }

    @Test
    void apply_shouldAlignRequestAndResponseCursorProperties() {
        Schema<Object> request = new Schema<>()
                .type("object")
                .addProperty("cursor", new StringSchema());
        Schema<Object> response = new Schema<>()
                .type("object")
                .addProperty("cursor", new StringSchema().example("AoE="));

        Map<String, Schema> schemas = new LinkedHashMap<>();
        schemas.put(QueryWithCursorOpenApiCustomizer.CURSOR_QUERY_REQUEST_SCHEMA, request);
        schemas.put(QueryWithCursorOpenApiCustomizer.CURSOR_QUERY_RESPONSE_SCHEMA, response);
        OpenAPI openAPI = new OpenAPI().components(new Components().schemas(schemas));

        QueryWithCursorOpenApiCustomizer.apply(openAPI);

        assertCursorSchema(request.getProperties().get("cursor"));
        assertCursorSchema(response.getProperties().get("cursor"));
    }

    private static void assertCursorSchema(Object property) {
        assertInstanceOf(Schema.class, property);
        Schema<?> cursorSchema = (Schema<?>) property;
        assertEquals(ValidationDoc.CURSOR_PATTERN, cursorSchema.getPattern());
        assertEquals(ValidationDoc.CURSOR_LENGTH, cursorSchema.getMinLength());
        assertEquals(ValidationDoc.CURSOR_LENGTH, cursorSchema.getMaxLength());
        assertEquals(SwaggerDoc.CURSOR_EXAMPLE, cursorSchema.getExample());
        assertTrue(cursorSchema.getNullable());
    }

    private static HandlerMethod handlerMethod(String methodName) throws Exception {
        Method method = switch (methodName) {
            case "queryWithCursor" -> SearchApi.class.getMethod(
                    "queryWithCursor",
                    org.opengroup.osdu.core.common.model.search.CursorQueryRequest.class,
                    boolean.class);
            case "closeCursor" -> SearchApi.class.getMethod(
                    "closeCursor",
                    String.class,
                    boolean.class);
            case "queryRecords" -> SearchApi.class.getMethod(
                    "queryRecords",
                    org.opengroup.osdu.core.common.model.search.QueryRequest.class);
            default -> throw new IllegalArgumentException(methodName);
        };
        return new HandlerMethod(new SearchApi(), method);
    }
}
