package org.opengroup.osdu.common;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Scenario;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.core.test.auth.UserType;
import org.opengroup.osdu.core.test.client.ClientException;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.core.test.client.RetryConfiguration;
import org.opengroup.osdu.core.test.client.SchemaClient;
import org.opengroup.osdu.core.test.client.SearchClient;
import org.opengroup.osdu.core.test.client.StorageClient;
import org.opengroup.osdu.core.test.client.TidyTestClientRegistry;
import org.opengroup.osdu.core.test.client.model.OpenApiSpec;
import org.opengroup.osdu.core.test.client.model.storage.CreateRecordsResponse;
import org.opengroup.osdu.core.test.client.model.storage.RecordAcl;
import org.opengroup.osdu.core.test.client.model.storage.RecordLegal;
import org.opengroup.osdu.core.test.client.model.storage.StorageRecord;
import org.opengroup.osdu.core.test.config.TestInitializer;
import org.opengroup.osdu.core.test.service.ServiceType;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.models.schema.PersistentSchemaTestIndex;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.core.test.util.TestFileUtil;
import org.opengroup.osdu.util.SearchTestConfig;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.util.Config.getEntitlementsDomain;
import static org.opengroup.osdu.util.Config.getLegalTag;
import static org.opengroup.osdu.util.Config.getOtherRelevantDataCountries;
import static org.opengroup.osdu.core.test.util.ResponseUtil.fromJson;
import static org.opengroup.osdu.core.test.util.ResponseUtil.toJson;
import static org.opengroup.osdu.util.Utility.beautifyJsonString;

@Slf4j
public class BaseSearchSteps {
    private static final Gson GSON = new Gson();
    private static final List<ServiceType> SERVICE_TYPES = List.of(
        ServiceType.SCHEMA_V1,
        ServiceType.SEARCH_V2,
        ServiceType.STORAGE_V2,
        ServiceType.INDEXER_V2);
    private static final List<UserType> USER_TYPES = List.of(UserType.PRIVILEGED_USER);
    private static final TestInitializer TEST_INITIALIZER = TestInitializer.getSharedTestInitializer(
        USER_TYPES, SERVICE_TYPES, RetryConfiguration.none());
    protected Scenario scenario;
    protected Map<String, String> tenantMap = new HashMap<>();
    protected Map<String, String> headers = new HashMap<>();

    private static final ThreadLocal<Boolean> USE_SEARCH_AFTER_CURSOR = ThreadLocal.withInitial(() -> false);

    private static final StorageClient SHARED_STORAGE_CLIENT;
    private static final SchemaClient SHARED_SCHEMA_CLIENT;
    private static final SearchClient SHARED_SEARCH_CLIENT;

    static {
        SHARED_STORAGE_CLIENT = new StorageClient(
            TEST_INITIALIZER.getStringHttpClient(),
            UserType.PRIVILEGED_USER);
        SHARED_SCHEMA_CLIENT = new SchemaClient(
            TEST_INITIALIZER.getStringHttpClient(),
            UserType.PRIVILEGED_USER);
        SHARED_SEARCH_CLIENT = new SearchClient(
            TEST_INITIALIZER.getStringHttpClient(),
            UserType.PRIVILEGED_USER);
    }

    protected SpatialFilter spatialFilter = new SpatialFilter();
    protected SpatialFilter.ByBoundingBox byBoundingBox;
    protected SpatialFilter.ByIntersection byIntersection;
    protected SpatialFilter.ByWithinPolygon byWithinPolygon;

    protected static final String TIME_STAMP = String.valueOf(System.currentTimeMillis());
    protected BaseSearchSteps() {
        tenantMap.put("tenant1", Config.getDataPartitionIdTenant1());
        tenantMap.put("tenant2", Config.getDataPartitionIdTenant2());
        tenantMap.put("common", "common");
    }

    public void initScenario(Scenario scenario) {
        SearchTestConfig.updateEntitlementsDomainFromGroupId();
        this.scenario = scenario;
        headers = new HashMap<>();
    }

    public static void tearDownTrackedResources() {
        if (!hasTrackedResources()) {
            return;
        }
        TidyTestClientRegistry.teardownAll();
    }

    private static boolean hasTrackedResources() {
        return !SHARED_STORAGE_CLIENT.getTrackedIds().isEmpty()
            || !SHARED_SCHEMA_CLIENT.getTrackedIds().isEmpty();
    }

    protected void waitForSearchIndex(String kind) {
        String resolvedKind = generateActualName(kind);
        QueryRequest request = new QueryRequest();
        request.setKind(resolvedKind);
        Map<String, String> probeHeaders = Map.of(
            DpsHeaders.DATA_PARTITION_ID, getTenantMapping("tenant1"));
        int maxAttempts = Config.getSearchIndexWaitMaxAttempts();
        int intervalSeconds = Config.getSearchIndexWaitIntervalSeconds();
        log.info(
            "Probing search index for kind '{}' (resolved='{}'), maxAttempts={}, intervalSeconds={}",
            kind, resolvedKind, maxAttempts, intervalSeconds);
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int attemptNumber = attempt + 1;
            try {
                HttpResponse<QueryResponse> response = searchClient().query(request, probeHeaders);
                int resultCount = response.body() == null || response.body().getResults() == null
                    ? 0
                    : response.body().getResults().size();
                log.debug(
                    "Search index probe attempt {}/{} response for kind '{}' (resolved='{}'): {}",
                    attemptNumber, maxAttempts, kind, resolvedKind, formatResponseBody(response.body()));
                if (response.statusCode() == HttpStatus.SC_OK && resultCount > 0) {
                    log.info(
                        "Search index ready for kind '{}' (resolved='{}') after {} probe attempt(s), resultCount={}",
                        kind, resolvedKind, attemptNumber, resultCount);
                    return;
                }
                log.info(
                    "Search index probe attempt {}/{} for kind '{}' (resolved='{}'): status={}, resultCount={}",
                    attemptNumber, maxAttempts, kind, resolvedKind, response.statusCode(), resultCount);
            } catch (ClientException exception) {
                log.info(
                    "Search index probe attempt {}/{} for kind '{}' (resolved='{}') failed: {}",
                    attemptNumber, maxAttempts, kind, resolvedKind, exception.getMessage());
                if (exception.getError() != null) {
                    log.debug(
                        "Search index probe attempt {}/{} error body for kind '{}' (resolved='{}'): {}",
                        attemptNumber, maxAttempts, kind, resolvedKind,
                        formatResponseBody(exception.getError()));
                }
            }
            if (attemptNumber < maxAttempts) {
                try {
                    TimeUnit.SECONDS.sleep(intervalSeconds);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interruptedException);
                }
            }
        }
        String message = String.format(
            "Search index not ready for kind '%s' (resolved='%s') after %d probe attempt(s)",
            kind, resolvedKind, maxAttempts);
        log.warn(message);
        throw new AssertionError(message);
    }

    protected void i_send_request_with_xcollab_header(String xCollaborationHeader) {
        if (xCollaborationHeader != null && !xCollaborationHeader.isEmpty()) {
            headers.put(DpsHeaders.COLLABORATION, xCollaborationHeader);
        }
    }

    public void i_send_request_to_tenant(String tenant) {
        headers.put(DpsHeaders.DATA_PARTITION_ID, getTenantMapping(tenant));
        if (shouldUseInvalidAuthorizationForTenant(tenant)) {
            headers.put(DpsHeaders.AUTHORIZATION, "Bearer invalid-token");
        } else {
            headers.remove(DpsHeaders.AUTHORIZATION);
        }
    }

    private boolean shouldUseInvalidAuthorizationForTenant(String tenant) {
        if (!"tenant2".equals(tenant)) {
            return false;
        }
        String tenant2Partition = getTenantMapping("tenant2");
        return tenant2Partition == null || tenant2Partition.isBlank()
            || tenant2Partition.equals(getTenantMapping("tenant1"));
    }

    public void define_bounding_box_with_points_and(Double topLatitude, Double topLongitude, Double bottomLatitude, Double
            bottomLongitude) {
        Point bottomRight = new Point(bottomLatitude, bottomLongitude);
        Point topLeft = new Point(topLatitude, topLongitude);
        byBoundingBox = new SpatialFilter.ByBoundingBox(topLeft, bottomRight);
        spatialFilter.setByBoundingBox(byBoundingBox);
    }

    public void define_intersection_polygon_with_points(Double latitude1, Double longitude1, Double latitude2, Double longitude2,
                                                        Double latitude3, Double longitude3, Double latitude4, Double longitude4,
                                                        Double latitude5, Double longitude5) {
        Point point1 = new Point(latitude1, longitude1);
        Point point2 = new Point(latitude2, longitude2);
        Point point3 = new Point(latitude3, longitude3);
        Point point4 = new Point(latitude4, longitude4);
        Point point5 = new Point(latitude5, longitude5);
        List<Point> points = Arrays.asList(point1, point2, point3, point4, point5);
        Polygon polygon = new Polygon(points);
        List<Polygon> polygons = List.of(polygon);
        byIntersection = new SpatialFilter.ByIntersection(polygons);
        spatialFilter.setByIntersection(byIntersection);
    }

    public void define_within_polygon_with_points(Double latitude1, Double longitude1) {
        Point point1 = new Point(latitude1, longitude1);
        List<Point> points = List.of(point1);
        byWithinPolygon = new SpatialFilter.ByWithinPolygon(points);
        spatialFilter.setByWithinPolygon(byWithinPolygon);
    }

    protected HttpResponse<QueryResponse> executeQuery(QueryRequest request,
                                                       Map<String, String> requestHeaders) {
        HttpResponse<QueryResponse> response = searchClient().query(request, requestHeaders);
        logResponse(response);
        return response;
    }

    public static void enableSearchAfterCursor() {
        USE_SEARCH_AFTER_CURSOR.set(true);
    }

    public static void disableSearchAfterCursor() {
        USE_SEARCH_AFTER_CURSOR.remove();
    }

    protected HttpResponse<CursorQueryResponse> executeCursorQuery(CursorQueryRequest request,
                                                                   Map<String, String> requestHeaders) {
        HttpResponse<CursorQueryResponse> response = useSearchAfterCursor()
            ? searchClient().queryWithSearchAfter(request, requestHeaders)
            : searchClient().queryWithCursor(request, requestHeaders);
        logResponse(response);
        return response;
    }

    private static boolean useSearchAfterCursor() {
        return Boolean.TRUE.equals(USE_SEARCH_AFTER_CURSOR.get());
    }

    protected HttpResponse<OpenApiSpec> executeGetApiDocs(Map<String, String> requestHeaders) {
        HttpResponse<OpenApiSpec> response = searchClient().getApiDocs(requestHeaders);
        logResponse(response);
        return response;
    }

    protected ClientException executeQueryExpectingError(QueryRequest request,
                                                         Map<String, String> requestHeaders) {
        return executeExpectingError(() -> searchClient().query(request, requestHeaders));
    }

    protected ClientException executeCursorQueryExpectingError(CursorQueryRequest request,
                                                               Map<String, String> requestHeaders) {
        if (useSearchAfterCursor()) {
            return executeExpectingError(() -> searchClient().queryWithSearchAfter(request, requestHeaders));
        }
        return executeExpectingError(() -> searchClient().queryWithCursor(request, requestHeaders));
    }

    protected StorageClient storageClient() {
        return SHARED_STORAGE_CLIENT;
    }

    protected SchemaClient schemaClient() {
        return SHARED_SCHEMA_CLIENT;
    }

    protected SearchClient searchClient() {
        return SHARED_SEARCH_CLIENT;
    }

    private ClientException executeExpectingError(Runnable action) {
        try {
            action.run();
        } catch (ClientException exception) {
            return exception;
        }
        throw new AssertionError("Expected ClientException");
    }

    protected void logResponse(HttpResponse<?> response) {
        String correlationId = getHeaderValue(response.headers(), DpsHeaders.CORRELATION_ID);
        log.info("Scenario Name: {}, Correlation id: {}, Status code: {}",
            scenario.getName(), correlationId, response.statusCode());
        Object body = response.body();
        if (body != null) {
            log.debug("Scenario Name: {}, Response body: {}",
                scenario.getName(), formatResponseBody(body));
        }
    }

    private String formatResponseBody(Object body) {
        if (body == null) {
            return "null";
        }
        String payload = body instanceof String stringBody ? stringBody : toJson(body);
        return beautifyJsonString(payload);
    }

    protected String getTenantMapping(String tenant) {
        if (tenantMap.containsKey(tenant)) {
            return tenantMap.get(tenant);
        }
        return null;
    }

    private static String stripSurroundingQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    public String generateActualName(String rawName) {
        for (String tenant : tenantMap.keySet()) {
            rawName = rawName.replace(tenant, getTenantMapping(tenant));
        }
        return rawName.replace("<timestamp>", TIME_STAMP);
    }

    protected Object resolveKindForRequest(String kind) {
        if (kind == null) {
            return null;
        }
        String normalized = stripSurroundingQuotes(kind.trim());
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            if (normalized.length() == 2) {
                return Collections.emptyList();
            }
            String[] kinds = fromJson(GSON, normalized, String[].class);
            return Arrays.stream(kinds)
                .map(this::generateActualName)
                .toList();
        }
        if (normalized.matches("^\\d+$")) {
            return Long.parseLong(normalized);
        }
        if (normalized.contains(",")) {
            return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .map(this::generateActualName)
                .collect(Collectors.joining(","));
        }
        return generateActualName(normalized);
    }

    protected Legal generateLegalTag() {
        Legal legal = new Legal();
        Set<String> legalTags = new HashSet<>();
        legalTags.add(getLegalTag());
        legal.setLegaltags(legalTags);
        Set<String> otherRelevantCountries = new HashSet<>();
        otherRelevantCountries.add(getOtherRelevantDataCountries());
        legal.setOtherRelevantDataCountries(otherRelevantCountries);
        return legal;
    }

    private static RecordLegal toRecordLegal(Legal legal) {
        return new RecordLegal(
            legal.getLegaltags().toArray(String[]::new),
            legal.getOtherRelevantDataCountries().toArray(String[]::new));
    }

    private RecordAcl toRecordAcl(String dataGroup) {
        String[] acl = {generateActualName(dataGroup) + "." + getEntitlementsDomain()};
        return new RecordAcl(acl, acl);
    }

    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
        dataTable.asMaps().stream()
            .map(this::toSetup)
            .toList()
            .forEach(this::createSchema);
    }

    private Setup toSetup(Map<String, String> row) {
        Setup setup = new Setup();
        setup.setKind(row.get("kind"));
        setup.setSchemaFile(row.get("schemaFile"));
        return setup;
    }

    private void createSchema(Setup input) {
        PersistentSchemaTestIndex testIndex = new PersistentSchemaTestIndex(schemaClient(), this);
        testIndex.setSchemaFile(input.getSchemaFile());
        testIndex.setupSchema();
        testIndex.setKind(testIndex.getSchemaModel().getSchemaInfo().getSchemaIdentity().getId());
    }

    public String generateActualNameWithoutTs(String rawName) {
        for (Map.Entry<String, String> tenant : tenantMap.entrySet()) {
            rawName = rawName.replace(tenant.getKey(), tenant.getValue());
        }
        return rawName.replace("<timestamp>", "");
    }

    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {
        i_ingest_records_with_the_for_a_given_with_header(record, dataGroup, kind, "");
    }

    protected void i_ingest_records_with_the_for_a_given_with_header(String record, String dataGroup, String kind,
                                                                     String xCollaborationHeader) {
        String actualKind = generateActualName(kind);
        try {
            String fileContent = TestFileUtil.readTestDataFile(String.format("%s.%s", record, "json"));
            StorageRecord[] templates = fromJson(GSON, fileContent, StorageRecord[].class);
            String createTime = java.time.Instant.now().toString();
            RecordLegal legal = toRecordLegal(generateLegalTag());
            RecordAcl acl = toRecordAcl(dataGroup);
            StorageRecord[] storageRecords = Arrays.stream(templates)
                .map(template -> new StorageRecord(
                    generateActualId(template.id(), actualKind),
                    template.version(),
                    actualKind,
                    acl,
                    template.data(),
                    legal,
                    template.ancestry(),
                    template.tags(),
                    template.meta(),
                    template.modifyTime(),
                    template.modifyUser(),
                    createTime,
                    "TestUser"))
                .toArray(StorageRecord[]::new);
            log.info("Start ingesting records= {}", toJson(storageRecords));
            headers.putIfAbsent(DpsHeaders.DATA_PARTITION_ID, getTenantMapping("tenant1"));
            Map<String, String> ingestHeaders = new HashMap<>(headers);
            if (xCollaborationHeader != null && !xCollaborationHeader.isEmpty()) {
                ingestHeaders.put(DpsHeaders.COLLABORATION, xCollaborationHeader);
            }
            HttpResponse<CreateRecordsResponse> response =
                storageClient().putRecords(storageRecords, ingestHeaders);
            logResponse(response);
            assertEquals(HttpStatus.SC_CREATED, response.statusCode());
            trackIngestedResources(storageRecords, response, actualKind);
            waitForSearchIndex(kind);
        } catch (Exception ex) {
            throw new AssertionError(ex.getMessage());
        }
    }

    protected String generateActualId(String rawName, String kind) {
        rawName = generateActualName(rawName);
        String kindSubType = kind.split(":")[2];
        return rawName.replaceAll("<kindSubType>", kindSubType);
    }

    private void trackIngestedResources(StorageRecord[] storageRecords,
                                      HttpResponse<CreateRecordsResponse> response,
                                      String actualKind) {
        StorageClient client = storageClient();
        for (StorageRecord record : storageRecords) {
            client.trackId(record.id());
        }
        CreateRecordsResponse body = response.body();
        if (body != null && body.recordIds() != null) {
            for (String recordId : body.recordIds()) {
                client.trackId(recordId);
            }
        }
        if (body != null && body.recordIdVersions() != null) {
            for (String recordIdVersion : body.recordIdVersions()) {
                client.trackId(recordIdFromVersion(recordIdVersion));
            }
        }
        schemaClient().trackId(actualKind);
    }

    private static String recordIdFromVersion(String recordIdVersion) {
        int lastColon = recordIdVersion.lastIndexOf(':');
        return lastColon > 0 ? recordIdVersion.substring(0, lastColon) : recordIdVersion;
    }

    private static String getHeaderValue(Header[] headers, String name) {
        if (headers == null) {
            return null;
        }
        for (Header header : headers) {
            if (name.equalsIgnoreCase(header.getName())) {
                return header.getValue();
            }
        }
        return null;
    }
}
