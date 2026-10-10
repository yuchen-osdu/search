// Copyright © Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.provider.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.ContentTooLongException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.Query;
import org.opengroup.osdu.core.common.model.search.SortOrder;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.search.cache.SearchAfterSettingsCache;
import org.opengroup.osdu.search.config.ElasticLoggingConfig;
import org.opengroup.osdu.search.context.UserContext;
import org.opengroup.osdu.search.logging.AuditLogger;
import org.opengroup.osdu.search.model.SearchAfterSettings;
import org.opengroup.osdu.search.util.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.POLICY_FEATURE_NAME;

@ExtendWith(MockitoExtension.class)
public class SearchAfterQueryServiceImplTest {
    private static final String reason = "Internal Server";
    private static final String message = "Search not completed";
    private static final String indexName = "index";
    private static final String userId = "userId";
    private static final String name = "name";
    private static final String text = "text";
    private static final String kind = "osdu:test:kind:1.0.0";

    @Mock
    private SearchAfterSettings cursorSettings;

    @Mock
    private ElasticsearchClient client;

    @Mock
    private HitsMetadata<Map<String, Object>> searchHits;

    @Mock
    private IQueryPerformanceLogger perfLogger;

    @Mock
    private Hit<Map<String, Object>> searchHit;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private JaxRsDpsLog log;

    @Mock
    private ElasticClientHandler elasticClientHandler;

    @Mock
    private SearchAfterSettingsCache cursorCache;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private CrossTenantUtils crossTenantUtils;
    @Mock
    private IQueryParserUtil queryParserUtil;
    @Mock
    private SuggestionsQueryUtil suggestionsQueryUtil;
    @Mock
    private ElasticLoggingConfig elasticLoggingConfig;
    @Mock
    private ResponseExceptionParser exceptionParser;
    @Mock
    public IFeatureFlag collaborationFeatureFlag;
    @InjectMocks
    private SearchAfterQueryServiceImpl sut;

    @Mock
    private UserContext userContext;

    @Mock
    private IFeatureFlag featureFlag;

    @BeforeEach
    public void init() {
        Mockito.lenient().doReturn(userId).when(dpsHeaders).getUserEmail();
        Mockito.lenient().doReturn(indexName).when(crossTenantUtils).getIndexName(any());
        Mockito.lenient().doReturn(cursorSettings).when(cursorCache).get(anyString());
        Mockito.lenient().doReturn(client).when(elasticClientHandler).getOrCreateRestClient();
        Mockito.lenient().when(elasticLoggingConfig.getEnabled()).thenReturn(false);
        Mockito.lenient().when(elasticLoggingConfig.getThreshold()).thenReturn(200L);
        Mockito.lenient().when(userContext.isRootUser()).thenReturn(false);
    }

    @Test
    public void testQueryIndex_whenSearchHitsIsNotEmpty() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, List<String>> highlightFields = getHighlightFields();
        Map<String, Object> hitFields = new HashMap<>();
        String cursor = "cursor";
        String pitId = "pitId";
        long totalHitsCount = 1L;

        CursorQueryRequest cachedRequest = new CursorQueryRequest();
        cachedRequest.setKind(kind);
        cachedRequest.setLimit(10);

        CursorQueryRequest cursorQueryRequest = new CursorQueryRequest();
        cursorQueryRequest.setKind(kind);
        cursorQueryRequest.setCursor(cursor);

        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(totalHitsCount).when(cursorSettings).getTotalCount();
        doReturn(cachedRequest).when(cursorSettings).getCachedQuery();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(pitId).when(searchResponse).pitId();
        doReturn(hits).when(searchHits).hits();
        doReturn(highlightFields).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();

        // act
        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(cursorQueryRequest);

        // assert
        ArgumentCaptor<SearchRequest> searchRequestArgumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        ArgumentCaptor<String> cursorArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Query> loggedQueryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(client).search(searchRequestArgumentCaptor.capture(), eq((Type)Map.class));
        verify(cursorCache).get(cursorArgumentCaptor.capture());
        SearchRequest searchRequest = searchRequestArgumentCaptor.getValue();
        String searchRequestCursor = cursorArgumentCaptor.getValue();
        assertEquals(obtainedQueryResponse.getResults().size(), 1);
        assertTrue(obtainedQueryResponse.getResults().get(0).keySet().contains("highlight"));
        assertEquals(((Map<String, List<String>>)obtainedQueryResponse.getResults().get(0).get("highlight")).get(name), List.of(text));
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
        assertEquals(searchRequest.pit().id(), pitId);
        assertEquals(searchRequestCursor, cursor);
        verify(this.auditLogger, times(1)).queryIndexWithCursorSuccess(anyList());
        verify(this.perfLogger, times(1)).log(loggedQueryCaptor.capture(), anyLong(), eq(200));
        // the executed (merged) query is the one reported, carrying the cursor of the incoming request
        assertEquals(cursor, ((CursorQueryRequest) loggedQueryCaptor.getValue()).getCursor());
        assertEquals(10, loggedQueryCaptor.getValue().getLimit());
    }

    @Test
    public void testQueryIndex_fallsBackToCachedQueryLimitAndReturnedFields_whenNotResent() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, Object> hitFields = new HashMap<>();
        String cursor = "cursor";
        String pitId = "pitId";

        // Cached original (page-1) request: the values that should actually be honored
        CursorQueryRequest cachedRequest = new CursorQueryRequest();
        cachedRequest.setKind("osdu:test:kind:1.0.0");
        cachedRequest.setLimit(1);
        cachedRequest.setReturnedFields(List.of("kind", "id"));

        // Page-2 request per documented contract: minimal, no limit/fields resent
        CursorQueryRequest incomingRequest = new CursorQueryRequest();
        incomingRequest.setKind("osdu:test:kind:1.0.0");
        incomingRequest.setCursor(cursor);

        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(1L).when(cursorSettings).getTotalCount();
        doReturn(cachedRequest).when(cursorSettings).getCachedQuery();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type) Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(pitId).when(searchResponse).pitId();
        doReturn(hits).when(searchHits).hits();
        doReturn(new HashMap<>()).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();

        sut.queryIndex(incomingRequest);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client).search(requestCaptor.capture(), eq((Type) Map.class));
        SearchRequest builtRequest = requestCaptor.getValue();

        assertEquals(1, builtRequest.size());
        assertTrue(builtRequest.source().filter().includes().contains("kind"));
        assertTrue(builtRequest.source().filter().includes().contains("id"));
    }

    @Test
    public void testQueryIndex_leavesCachedQueryUntouched_andCarriesCursorOnExecutedQuery() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, Object> hitFields = new HashMap<>();
        String cursor = "cursor";
        String pitId = "pitId";

        CursorQueryRequest incomingRequest = new CursorQueryRequest();
        incomingRequest.setKind(kind);
        incomingRequest.setCursor(cursor);

        CursorQueryRequest cachedRequest = new CursorQueryRequest();
        cachedRequest.setKind(kind);
        cachedRequest.setCursor(null);

        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(1L).when(cursorSettings).getTotalCount();
        doReturn(cachedRequest).when(cursorSettings).getCachedQuery();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type) Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(pitId).when(searchResponse).pitId();
        doReturn(hits).when(searchHits).hits();
        doReturn(new HashMap<>()).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();

        sut.queryIndex(incomingRequest);

        // the cached page-1 request is never mutated in place, it is merged into a fresh request
        assertNull(cachedRequest.getCursor());
        assertEquals(cursor, captureCarriedForwardQuery().getCursor());
    }

    @Test
    public void testQueryIndex_appliesLimitAndReturnedFieldsOverridesFromSubsequentRequest() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, Object> hitFields = new HashMap<>();
        String cursor = "cursor";
        String pitId = "pitId";

        // Page-1 request captured with the cursor
        CursorQueryRequest cachedRequest = new CursorQueryRequest();
        cachedRequest.setKind(kind);
        cachedRequest.setLimit(1);
        cachedRequest.setReturnedFields(List.of("kind", "id"));
        cachedRequest.setExcludedFields(List.of("data.rawData"));

        // Page-2 request changing limit and returnedFields
        CursorQueryRequest incomingRequest = new CursorQueryRequest();
        incomingRequest.setKind(kind);
        incomingRequest.setCursor(cursor);
        incomingRequest.setLimit(5);
        incomingRequest.setReturnedFields(List.of("data.wellName"));

        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(1L).when(cursorSettings).getTotalCount();
        doReturn(cachedRequest).when(cursorSettings).getCachedQuery();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type) Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(pitId).when(searchResponse).pitId();
        doReturn(hits).when(searchHits).hits();
        doReturn(new HashMap<>()).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();

        sut.queryIndex(incomingRequest);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client).search(requestCaptor.capture(), eq((Type) Map.class));
        SearchRequest builtRequest = requestCaptor.getValue();

        assertEquals(5, builtRequest.size());
        assertEquals(List.of("data.wellName"), builtRequest.source().filter().includes());
        assertFalse(builtRequest.source().filter().includes().contains("kind"));
        // fields that were not resent still fall back to the cached request
        assertTrue(builtRequest.source().filter().excludes().contains("data.rawData"));
    }

    @Test
    public void testQueryIndex_overridesAreCarriedForwardToTheNextPage() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, Object> hitFields = new HashMap<>();
        String cursor = "cursor";
        String pitId = "pitId";

        SortQuery cachedSort = new SortQuery();
        cachedSort.setField(List.of("id"));
        cachedSort.setOrder(List.of(SortOrder.ASC));

        CursorQueryRequest cachedRequest = new CursorQueryRequest();
        cachedRequest.setKind(kind);
        cachedRequest.setLimit(1);
        cachedRequest.setQuery("data.Country:\\\"US\\\"");
        cachedRequest.setSort(cachedSort);

        SortQuery changedSort = new SortQuery();
        changedSort.setField(List.of("kind"));
        changedSort.setOrder(List.of(SortOrder.DESC));

        CursorQueryRequest incomingRequest = new CursorQueryRequest();
        incomingRequest.setKind("osdu:other:kind:1.0.0");
        incomingRequest.setCursor(cursor);
        incomingRequest.setLimit(5);
        incomingRequest.setSort(changedSort);

        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(1L).when(cursorSettings).getTotalCount();
        doReturn(cachedRequest).when(cursorSettings).getCachedQuery();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type) Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(pitId).when(searchResponse).pitId();
        doReturn(hits).when(searchHits).hits();
        doReturn(new HashMap<>()).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();

        sut.queryIndex(incomingRequest);

        CursorQueryRequest carriedForward = captureCarriedForwardQuery();
        assertEquals(5, carriedForward.getLimit());
        assertEquals(cachedRequest.getQuery(), carriedForward.getQuery());
        assertEquals(kind, carriedForward.getKind());
        assertEquals(cachedSort, carriedForward.getSort());
        assertFalse(carriedForward.isTrackTotalCount());
    }

    @Test
    public void testQueryIndex_whenSearchFails_leavesTheCursorQueryUntouched() throws Exception {
        String cursor = "cursor";

        CursorQueryRequest cachedRequest = new CursorQueryRequest();
        cachedRequest.setKind(kind);
        cachedRequest.setLimit(1);
        cachedRequest.setReturnedFields(List.of("id"));

        CursorQueryRequest incomingRequest = new CursorQueryRequest();
        incomingRequest.setKind(kind);
        incomingRequest.setCursor(cursor);
        incomingRequest.setLimit(5);
        incomingRequest.setReturnedFields(List.of("data.wellName"));

        doReturn("pitId").when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(cachedRequest).when(cursorSettings).getCachedQuery();
        doThrow(new AppException(500, reason, message)).when(client).search(any(SearchRequest.class), eq((Type) Map.class));

        assertThrows(AppException.class, () -> sut.queryIndex(incomingRequest));

        // the overrides of the failed page are not carried over to the retry of the same cursor
        verify(cursorSettings, never()).setCachedQuery(any());
        assertEquals(1, cachedRequest.getLimit());
        assertEquals(List.of("id"), cachedRequest.getReturnedFields());
    }

    @Test
    public void testQueryIndex_whenSearchHitsIsEmpty_deletesCacheUnderOriginalCursor() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        String cursor = "cursor";
        String pitId = "pitId";

        CursorQueryRequest incomingRequest = new CursorQueryRequest();
        incomingRequest.setKind("osdu:test:kind:1.0.0");
        incomingRequest.setCursor(cursor);

        CursorQueryRequest cachedRequest = new CursorQueryRequest();
        cachedRequest.setKind("osdu:test:kind:1.0.0");
        cachedRequest.setCursor(null);

        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(0L).when(cursorSettings).getTotalCount();
        doReturn(cachedRequest).when(cursorSettings).getCachedQuery();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type) Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();

        sut.queryIndex(incomingRequest);

        verify(cursorCache, times(1)).delete(eq(cursor));
    }

    @Test
    public void testQueryIndex_resetsTrackTotalCount_afterCachingOnNoCursorPath() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, Object> hitFields = new HashMap<>();
        String pitId = "pitId";

        CursorQueryRequest request = new CursorQueryRequest();
        request.setKind("osdu:test:kind:1.0.0");
        request.setCursor(null);

        OpenPointInTimeResponse openPitResponse = mock(OpenPointInTimeResponse.class);
        SearchResponse searchResponse = mock(SearchResponse.class);
        TotalHits totalHits = mock(TotalHits.class);
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();
        doReturn(new HashMap<>()).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();
        doReturn(totalHits).when(searchHits).total();
        doReturn(1L).when(totalHits).value();
        doReturn(openPitResponse).when(client).openPointInTime(any(OpenPointInTimeRequest.class));
        doReturn(pitId).when(openPitResponse).id();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type) Map.class));

        sut.queryIndex(request);

        assertFalse(request.isTrackTotalCount());
    }

    @Test
    public void testQueryIndex_whenSearchHitsIsEmpty() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        String cursor = "cursor";
        String pitId = "pitId";
        long totalHitsCount = 0L;

        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(totalHitsCount).when(cursorSettings).getTotalCount();
        doReturn(cursorQueryRequest).when(cursorSettings).getCachedQuery();
        doReturn(cursor).when(cursorQueryRequest).getCursor();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();

        // act
        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(cursorQueryRequest);

        // assert
        ArgumentCaptor<SearchRequest> searchRequestArgumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        ArgumentCaptor<String> cursorArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).search(searchRequestArgumentCaptor.capture(), eq((Type)Map.class));
        verify(cursorCache).get(cursorArgumentCaptor.capture());
        SearchRequest searchRequest = searchRequestArgumentCaptor.getValue();
        String searchRequestCursor = cursorArgumentCaptor.getValue();
        assertEquals(obtainedQueryResponse.getResults().size(), 0);
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
        assertEquals(searchRequest.pit().id(), pitId);
        assertEquals(searchRequestCursor, cursor);
    }


    @Test
    public void testQueryIndex_whenNoCursorInSearchQuery() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, List<String>> highlightFields = getHighlightFields();
        Map<String, Object> hitFields = new HashMap<>();
        String pitId = "pitId";
        long totalHitsCount = 1L;

        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        OpenPointInTimeResponse openPitResponse = mock(OpenPointInTimeResponse.class);
        SearchResponse searchResponse = mock(SearchResponse.class);
        TotalHits totalHits = mock(TotalHits.class);
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();
        doReturn(highlightFields).when(searchHit).highlight();
        doReturn(hitFields).when(searchHit).source();
        doReturn(totalHits).when(searchHits).total();
        doReturn(totalHitsCount).when(totalHits).value();
        doReturn(openPitResponse).when(client).openPointInTime(any(OpenPointInTimeRequest.class));
        doReturn(pitId).when(openPitResponse).id();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));

        // act
        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(cursorQueryRequest);

        // assert
        assertEquals(obtainedQueryResponse.getResults().size(), 1);
        assertTrue(obtainedQueryResponse.getResults().get(0).keySet().contains("highlight"));
        assertEquals(((Map<String, List<String>>)obtainedQueryResponse.getResults().get(0).get("highlight")).get(name), List.of(text));
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
    }

    @Test
    public void testQueryIndex_whenNoCursorInSearchQueryAndSearchHitsIsEmpty() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        String pitId = "pitId";
        long totalHitsCount = 0L;

        CursorQueryRequest searchRequest = mock(CursorQueryRequest.class);
        OpenPointInTimeResponse openPitResponse = mock(OpenPointInTimeResponse.class);
        SearchResponse searchResponse = mock(SearchResponse.class);
        TotalHits totalHits = mock(TotalHits.class);
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();
        doReturn(totalHits).when(searchHits).total();
        doReturn(totalHitsCount).when(totalHits).value();
        doReturn(openPitResponse).when(client).openPointInTime(any(OpenPointInTimeRequest.class));
        doReturn(pitId).when(openPitResponse).id();
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        Mockito.lenient().when(featureFlag.isFeatureEnabled(POLICY_FEATURE_NAME)).thenReturn(false);

        // act
        CursorQueryResponse obtainedQueryResponse = sut.queryIndex(searchRequest);

        // assert
        assertEquals(obtainedQueryResponse.getResults().size(), 0);
        assertEquals(obtainedQueryResponse.getTotalCount(), totalHitsCount);
    }

    @Test
    public void testQueryIndex_whenMismatchCursorIssuerAndConsumer_thenThrowException() throws Exception {
        String cursor = "cursor";
        String mismatchUserId = "mismatchUserId";

        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        doReturn(cursor).when(cursorQueryRequest).getCursor();
        doReturn(mismatchUserId).when(cursorSettings).getUserId();
        doReturn(client).when(elasticClientHandler).getOrCreateRestClient();

        AppException e = assertThrows(AppException.class, () -> sut.queryIndex(cursorQueryRequest));

        int errorCode = 403;
        AppError error = e.getError();
        assertEquals(errorCode, error.getCode());
        assertEquals("cursor issuer doesn't match the cursor consumer", error.getReason());
        assertEquals("cursor sharing is forbidden", error.getMessage());
    }

    @Test
    public void testQueryIndex_whenCursorSettingsNotFoundInCursorCache_thenThrowException() throws Exception {
        String cursor = "cursor";
        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        doReturn(cursor).when(cursorQueryRequest).getCursor();
        doReturn(null).when(cursorCache).get(any());

        AppException e = assertThrows(AppException.class, () -> sut.queryIndex(cursorQueryRequest));

        int errorCode = 400;
        AppError error = e.getError();
        assertEquals("Can't find the given cursor", error.getReason());
        assertEquals("The given cursor is invalid or expired", error.getMessage());
        assertEquals(errorCode, error.getCode());
    }

    @Test
    public void testQueryIndex_whenCursorNotFound_thenThrowException() throws Exception {
        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        doReturn("cursor").when(cursorQueryRequest).getCursor();
        doReturn("cursor").when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(cursorQueryRequest).when(cursorSettings).getCachedQuery();
        ElasticsearchException exception = mock(ElasticsearchException.class);
        doReturn(HttpServletResponse.SC_NOT_FOUND).when(exception).status();
        doReturn("No search context found for id [47500324]").when(exception).getMessage();

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        try {
            sut.queryIndex(cursorQueryRequest);
        } catch (AppException e) {
            int errorCode = 400;
            AppError error = e.getError();
            assertEquals("Can't find the given cursor", error.getReason());
            assertEquals("The given cursor is invalid or expired", error.getMessage());
            assertEquals(errorCode, error.getCode());
        }
    }

    @Test
    public void testQueryIndex_whenResponseTooLong_thenThrowException() throws Exception {
        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        doReturn("cursor").when(cursorQueryRequest).getCursor();
        doReturn("cursor").when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(cursorQueryRequest).when(cursorSettings).getCachedQuery();
        IOException exception = mock(IOException.class);
        doReturn(new ContentTooLongException(null)).when(exception).getCause();

        doThrow(exception).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        AppException e = assertThrows(AppException.class, () -> sut.queryIndex(cursorQueryRequest));

        int errorCode = 413;
        AppError error = e.getError();
        assertEquals("Response is too long", error.getReason());
        assertEquals("Elasticsearch response is too long, max is 100Mb", error.getMessage());
        assertEquals(errorCode, error.getCode());
    }

    @Test
    public void testQueryIndex_whenSearchGives500_thenThrowException() throws Exception {
        CursorQueryRequest cursorQueryRequest = mock(CursorQueryRequest.class);
        OpenPointInTimeResponse openPitResponse = mock(OpenPointInTimeResponse.class);
        doReturn(null).when(cursorQueryRequest).getCursor();
        doReturn(openPitResponse).when(client).openPointInTime(any(OpenPointInTimeRequest.class));
        doReturn("pitId").when(openPitResponse).id();
        AppException ex = new AppException(500, reason, message);
        doReturn(client).when(elasticClientHandler).getOrCreateRestClient();

        doThrow(ex).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        AppException e = assertThrows(AppException.class, () -> sut.queryIndex(cursorQueryRequest));

        int errorCode = 500;
        AppError error = e.getError();
        assertEquals(errorCode, error.getCode());
        assertEquals(reason, error.getReason());
        assertEquals(message, error.getMessage());
    }

    @Test
    public void refreshCursorCache_update_returned_cursor() {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn("pitId").when(searchResponse).pitId();
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();

        String cursor = sut.refreshCursorCache(searchResponse, true, cursorSettings);
        assertNotNull(cursor);
    }

    @Test
    public void refreshCursorCache_pitNull()  {
        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(null).when(searchResponse).pitId();
        String cursor = sut.refreshCursorCache(searchResponse, true, cursorSettings);
        assertNull(cursor);
    }

    @Test
    public void close_open_cursor() throws Exception {
        String pitId = "pitId";
        String cursor = "cursor";
        doReturn(pitId).when(cursorSettings).getPitId();
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(false).when(cursorSettings).isClosed();

        // act
        sut.close(cursor);

        // assert
        verify(this.cursorCache, times(1)).delete(eq(cursor));
        verify(this.client, times(1)).closePointInTime(any(ClosePointInTimeRequest.class));
    }

    @Test
    public void close_closed_cursor() throws Exception {
        String cursor = "cursor";
        doReturn(userId).when(cursorSettings).getUserId();
        doReturn(true).when(cursorSettings).isClosed();

        // act
        sut.close(cursor);

        // assert
        verify(this.cursorCache, times(1)).delete(eq(cursor));
        verify(this.client, times(0)).closePointInTime(any(ClosePointInTimeRequest.class));
    }

    @Test
    public void close_cursor_whenMismatchCursorIssuerAndConsumer_thenThrowException() throws Exception {
        String cursor = "cursor";
        String mismatchUserId = "mismatchUserId";
        doReturn(mismatchUserId).when(cursorSettings).getUserId();

        AppException e = assertThrows(AppException.class, () -> sut.close(cursor));

        int errorCode = 403;
        AppError error = e.getError();
        assertEquals(errorCode, error.getCode());
        assertEquals("cursor issuer doesn't match the cursor consumer", error.getReason());
        assertEquals("cursor sharing is forbidden", error.getMessage());
    }

    private Map<String, List<String>> getHighlightFields() {
        Map<String, List<String>> highlightFields = new HashMap<>();
        highlightFields.put(name, List.of(text));
        return highlightFields;
    }

    private CursorQueryRequest captureCarriedForwardQuery() {
        ArgumentCaptor<CursorQueryRequest> cachedQueryCaptor = ArgumentCaptor.forClass(CursorQueryRequest.class);
        verify(cursorSettings).setCachedQuery(cachedQueryCaptor.capture());
        return cachedQueryCaptor.getValue();
    }
}
