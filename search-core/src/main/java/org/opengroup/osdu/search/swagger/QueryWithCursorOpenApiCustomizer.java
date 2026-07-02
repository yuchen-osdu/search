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
 * Aligns cursor-pagination OpenAPI declarations with runtime validation on {@code SearchApi}.
 *
 * <h2>Why this exists</h2>
 * <p>Cursor pagination is documented in three places: the optional {@code search_after} query
 * parameter, the optional request-body {@code cursor} on first/subsequent pages, and the path
 * {@code cursor} on {@code DELETE /query_with_cursor/{cursor}}. Runtime validation uses
 * {@link ValidationDoc#CURSOR_PATTERN} ({@code ^[0-9A-F]{32}$}) and
 * {@link ValidationDoc#CURSOR_LENGTH} ({@code 32}) via {@code @Pattern} on the controller and
 * request models in os-core-common.
 *
 * <p>Springdoc can still emit a served {@code api-docs.yaml} that contract fuzzers treat as the
 * source of truth but that does not match those rules — for example a path {@code cursor} pattern
 * of {@code ^[A-Za-z0-9=_-]+$}, or body {@code cursor} fields with no pattern/length at all. That
 * gap produces Schemathesis false positives: fuzzers generate values that look schema-valid yet
 * are rejected with HTTP&nbsp;400, or the reverse.
 *
 * <h2>What this customizer changes</h2>
 * <p>Two hooks are registered from {@link SwaggerConfiguration}:
 * <ul>
 *   <li>{@link #customizeOperation(Operation, HandlerMethod)} — per-operation parameters on
 *       {@code POST /query_with_cursor} and {@code DELETE /query_with_cursor/{cursor}}.</li>
 *   <li>{@link #apply(OpenAPI)} — {@code CursorQueryRequest} and {@code CursorQueryResponse}
 *       component schemas.</li>
 * </ul>
 *
 * <h3>Example: path {@code cursor} before vs after</h3>
 * <p>Before (too permissive; accepts values the server rejects):
 * <pre>{@code
 * # DELETE /query_with_cursor/{cursor}
 * parameters:
 *   - name: cursor
 *     in: path
 *     schema:
 *       type: string
 *       pattern: "^[A-Za-z0-9=_-]+$"   # does not match runtime ValidationDoc.CURSOR_PATTERN
 * }</pre>
 *
 * <p>After (aligned with {@link org.opengroup.osdu.search.api.SearchApi#closeCursor}):
 * <pre>{@code
 * parameters:
 *   - name: cursor
 *     in: path
 *     schema:
 *       type: string
 *       pattern: "^[0-9A-F]{32}$"
 *       minLength: 32
 *       maxLength: 32
 *       example: "098F6BCD4621D373CADE4E832627B4F"
 * }</pre>
 *
 * <h3>Example: request-body {@code cursor} before vs after</h3>
 * <p>Before:
 * <pre>{@code
 * # components.schemas.CursorQueryRequest
 * cursor:
 *   type: string
 *   description: Search context to retrieve the next page of results...
 * }</pre>
 *
 * <p>After:
 * <pre>{@code
 * cursor:
 *   type: string
 *   pattern: "^[0-9A-F]{32}$"
 *   minLength: 32
 *   maxLength: 32
 *   example: "098F6BCD4621D373CADE4E832627B4F"
 *   nullable: true
 * }</pre>
 *
 * <p>The response {@code CursorQueryResponse.cursor} property is tightened the same way (nullable,
 * since the last page returns {@code null}).
 *
 * <h3>Example: {@code search_after} query parameter</h3>
 * <p>Both cursor endpoints expose an optional boolean query flag. The customizer ensures the
 * served schema is an explicit boolean with default {@code false}, matching
 * {@code @RequestParam(defaultValue = "false")} on the controller:
 * <pre>{@code
 * # POST /query_with_cursor?search_after=false
 * # DELETE /query_with_cursor/{cursor}?search_after=true
 * parameters:
 *   - name: search_after
 *     in: query
 *     required: false
 *     schema:
 *       type: boolean
 *       default: false
 * }</pre>
 *
 * <h2>What this does not fix</h2>
 * <p>Spring MVC <em>ignores</em> unknown query parameters. OpenAPI lists only {@code search_after}
 * (plus headers such as {@code data-partition-id}), so Schemathesis may send extra query params and
 * expect HTTP&nbsp;400 ({@code negative_data_rejection}). The service returns HTTP&nbsp;200 because
 * the extra params are dropped. Standard OpenAPI has no way to document “additional query
 * parameters are permitted and ignored”; that behaviour cannot be captured by this customizer.
 * Address it in Schemathesis configuration if needed.
 *
 * <h2>Related documentation</h2>
 * <p>The committed spec under {@code docs/api/community/v2/openapi.yaml} is updated manually to
 * mirror these rules. CI and dev environments load the <em>live</em> spec from
 * {@code /api-docs.yaml}, which is what these customizers shape at runtime.
 *
 * @see SortQueryOpenApiCustomizer
 * @see SpatialFilterBoundingBoxOpenApiCustomizer
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
                parameter.setSchema(cursorStringSchema(false));
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

        alignCursorProperty(components.getSchemas().get(CURSOR_QUERY_REQUEST_SCHEMA), true);
        alignCursorProperty(components.getSchemas().get(CURSOR_QUERY_RESPONSE_SCHEMA), true);
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

    private static void alignCursorProperty(Schema<?> schema, boolean nullable) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }

        Object cursorProperty = schema.getProperties().get(CURSOR_PROPERTY);
        if (cursorProperty instanceof Schema<?> cursorSchema) {
            applyCursorConstraints(cursorSchema, nullable);
        }
    }

    private static BooleanSchema searchAfterSchema() {
        return new BooleanSchema()._default(false);
    }

    private static StringSchema cursorStringSchema(boolean nullable) {
        StringSchema schema = new StringSchema();
        schema.setPattern(ValidationDoc.CURSOR_PATTERN);
        schema.setMinLength(ValidationDoc.CURSOR_LENGTH);
        schema.setMaxLength(ValidationDoc.CURSOR_LENGTH);
        schema.setExample(SwaggerDoc.CURSOR_EXAMPLE);
        if (nullable) {
            schema.setNullable(true);
        }
        return schema;
    }

    private static void applyCursorConstraints(Schema<?> cursorSchema, boolean nullable) {
        cursorSchema.setType("string");
        cursorSchema.setPattern(ValidationDoc.CURSOR_PATTERN);
        cursorSchema.setMinLength(ValidationDoc.CURSOR_LENGTH);
        cursorSchema.setMaxLength(ValidationDoc.CURSOR_LENGTH);
        cursorSchema.setExample(SwaggerDoc.CURSOR_EXAMPLE);
        if (nullable) {
            cursorSchema.setNullable(true);
            if (cursorSchema.getTypes() != null) {
                cursorSchema.getTypes().add("null");
            }
        }
    }
}
