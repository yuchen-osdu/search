package org.opengroup.osdu.search.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.opengroup.osdu.core.common.SwaggerDoc;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.storage.validation.ValidationDoc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;

/**
 * Aligns cursor pagination in the served OpenAPI spec with runtime validation:
 * {@link ValidationDoc#CURSOR_PATTERN}, {@link ValidationDoc#CURSOR_LENGTH}, and the
 * {@code search_after} query parameter default. Extra query parameters are not documented;
 * Schemathesis uses {@code allow-extra-parameters = false}.
 *
 * @see SwaggerConfiguration#alignQueryWithCursorOperationParameters()
 * @see SwaggerConfiguration#alignQueryWithCursorSchemaDeclarations()
 */
final class QueryWithCursorOpenApiCustomizer {

    static final String CURSOR_PROPERTY = "cursor";
    static final String SEARCH_AFTER_PARAMETER = "search_after";
    static final String QUERY_WITH_CURSOR_PATH = "query_with_cursor";
    static final String CURSOR_QUERY_REQUEST_SCHEMA = CursorQueryRequest.class.getSimpleName();
    static final String CURSOR_QUERY_RESPONSE_SCHEMA = CursorQueryResponse.class.getSimpleName();

    private QueryWithCursorOpenApiCustomizer() {
    }

    /**
     * Tightens operation-level parameters for {@code SearchApi.queryWithCursor} and
     * {@code SearchApi.closeCursor} only (detected via {@link PostMapping} /
     * {@link DeleteMapping} paths containing {@value #QUERY_WITH_CURSOR_PATH}).
     *
     * @return the same {@code operation} instance, with parameter schemas updated in place
     */
    static Operation customizeOperation(Operation operation, HandlerMethod handlerMethod) {
        if (operation == null || handlerMethod == null || !isQueryWithCursorHandler(handlerMethod)) {
            return operation;
        }

        List<Parameter> parameters = operation.getParameters();
        if (parameters == null) {
            return operation;
        }

        for (Parameter parameter : parameters) {
            if (parameter == null || parameter.getName() == null) {
                continue;
            }
            if (SEARCH_AFTER_PARAMETER.equals(parameter.getName()) && "query".equals(parameter.getIn())) {
                parameter.setSchema(searchAfterSchema());
            }
            if (CURSOR_PROPERTY.equals(parameter.getName()) && "path".equals(parameter.getIn())) {
                parameter.setSchema(cursorStringSchema());
            }
        }
        return operation;
    }

    /**
     * Tightens {@code cursor} on {@link CursorQueryRequest} and {@link CursorQueryResponse}
     * in {@code components.schemas}. No-op when components are missing.
     */
    static void apply(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null || components.getSchemas() == null) {
            return;
        }

        alignCursorProperty(components.getSchemas().get(CURSOR_QUERY_REQUEST_SCHEMA));
        alignCursorProperty(components.getSchemas().get(CURSOR_QUERY_RESPONSE_SCHEMA));
    }

    /** Used by tests and {@link #customizeOperation} to scope changes to cursor-pagination handlers. */
    static boolean isQueryWithCursorHandler(HandlerMethod handlerMethod) {
        var method = handlerMethod.getMethod();

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null && mapsToQueryWithCursor(postMapping.value())) {
            return true;
        }

        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        return deleteMapping != null && mapsToQueryWithCursor(deleteMapping.value());
    }

    private static boolean mapsToQueryWithCursor(String[] paths) {
        return paths != null
                && Arrays.stream(paths).anyMatch(path -> path != null && path.contains(QUERY_WITH_CURSOR_PATH));
    }

    private static void alignCursorProperty(Schema<?> schema) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }

        Object cursorProperty = schema.getProperties().get(CURSOR_PROPERTY);
        if (cursorProperty instanceof Schema<?> cursorSchema) {
            applyCursorConstraints(cursorSchema);
        }
    }

    private static BooleanSchema searchAfterSchema() {
        return new BooleanSchema()._default(false);
    }

    private static StringSchema cursorStringSchema() {
        StringSchema schema = new StringSchema();
        schema.setPattern(ValidationDoc.CURSOR_PATTERN);
        schema.setMinLength(ValidationDoc.CURSOR_LENGTH);
        schema.setMaxLength(ValidationDoc.CURSOR_LENGTH);
        schema.setExample(SwaggerDoc.CURSOR_EXAMPLE);
        return schema;
    }

    private static void applyCursorConstraints(Schema<?> cursorSchema) {
        cursorSchema.setType("string");
        cursorSchema.setPattern(ValidationDoc.CURSOR_PATTERN);
        cursorSchema.setMinLength(ValidationDoc.CURSOR_LENGTH);
        cursorSchema.setMaxLength(ValidationDoc.CURSOR_LENGTH);
        cursorSchema.setExample(SwaggerDoc.CURSOR_EXAMPLE);
        cursorSchema.setNullable(true);
        if (cursorSchema.getTypes() != null) {
            cursorSchema.getTypes().add("null");
        }
    }
}
