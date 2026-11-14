// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.search.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.model.search.AggregationResponse;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks that the Jackson 3 HTTP mapper reads and writes the search API models like the Jackson 2 mapper did on
 * Spring Boot 3. Property order is not compared: Jackson 3 sorts properties alphabetically by default.
 */
class Jackson3ContractParityTest {

    private final ObjectMapper jackson2 = jackson2Mapper();
    private final JsonMapper jackson3 = new ObjectMapperConfiguration().objectMapper();

    private static ObjectMapper jackson2Mapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }

    @Test
    void queryResponse_serializesLikeJackson2() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("Name", "well-1");
        data.put("Depth", null);
        data.put("Tags", Arrays.asList("a", null));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", "opendes:well:1");
        result.put("kind", "opendes:wks:well:1.0.0");
        result.put("data", data);
        result.put("createTime", null);

        QueryResponse response = new QueryResponse();
        response.setResults(List.of(result));
        response.setAggregations(List.of(AggregationResponse.builder().key("opendes:wks:well:1.0.0").count(3L).build(),
                AggregationResponse.builder().build()));
        response.setTotalCount(42);

        assertSameJson(response);
        assertSameJson(QueryResponse.getEmptyResponse());
        assertSameJson(new QueryResponse());
    }

    @Test
    void cursorQueryResponse_serializesLikeJackson2() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("id", "opendes:well:1");
        result.put("data", null);

        CursorQueryResponse response = new CursorQueryResponse();
        response.setCursor("abc");
        response.setResults(List.of(result));
        response.setTotalCount(1);

        assertSameJson(response);
        assertSameJson(new CursorQueryResponse());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"kind\":\"opendes:wks:well:1.0.0\",\"query\":\"data.Name:well\",\"offset\":10,\"limit\":5,"
                    + "\"aggregateBy\":\"kind\",\"returnedFields\":[\"id\"],\"trackTotalCount\":true,"
                    + "\"queryAsOwner\":false,\"returnHighlightedFields\":true,\"highlightedFields\":[\"data.Name\"],"
                    + "\"sort\":{\"field\":[\"id\"],\"order\":[\"asc\"]},"
                    + "\"spatialFilter\":{\"field\":\"data.Location\",\"byBoundingBox\":{"
                    + "\"topLeft\":{\"latitude\":10.5,\"longitude\":-5},"
                    + "\"bottomRight\":{\"latitude\":1,\"longitude\":5.25}}}}",
            "{\"kind\":[\"a:b:c:1.0.0\",\"d:e:f:1.0.0\"],\"sort\":{\"field\":[\"id\",\"kind\"],\"order\":[\"DESC\",\"Asc\"]}}",
            "{\"kind\":\"*:*:*:*\",\"offset\":null,\"limit\":null,\"trackTotalCount\":null,\"unknownField\":{\"x\":1}}",
            "{\"kind\":\"*:*:*:*\"} {\"trailing\":true}",
            "{}"
    })
    void queryRequest_deserializesLikeJackson2(String json) throws Exception {
        assertEquals(
                jackson2.valueToTree(jackson2.readValue(json, QueryRequest.class)),
                jackson2.valueToTree(jackson3.readValue(json, QueryRequest.class)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"kind\":\"opendes:wks:well:1.0.0\",\"cursor\":\"abc\",\"limit\":5,\"returnedFields\":[\"id\"]}",
            "{\"kind\":\"*:*:*:*\",\"cursor\":null,\"limit\":null,\"unknownField\":1}"
    })
    void cursorQueryRequest_deserializesLikeJackson2(String json) throws Exception {
        assertEquals(
                jackson2.valueToTree(jackson2.readValue(json, CursorQueryRequest.class)),
                jackson2.valueToTree(jackson3.readValue(json, CursorQueryRequest.class)));
    }

    private void assertSameJson(Object value) throws Exception {
        JsonNode expected = jackson2.readTree(jackson2.writeValueAsString(value));
        JsonNode actual = jackson2.readTree(jackson3.writeValueAsString(value));
        assertEquals(expected, actual);
    }
}
