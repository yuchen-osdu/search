package org.opengroup.osdu.search.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.model.search.QueryRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ObjectMapperConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapperConfiguration().objectMapper();

    @Test
    void should_useDefaults_whenPrimitiveFieldsAreExplicitNull() throws Exception {
        String body = """
                {
                  "kind": ["0:0:0:0"],
                  "offset": null,
                  "limit": null,
                  "trackTotalCount": null,
                  "queryAsOwner": null
                }
                """;

        QueryRequest request = objectMapper.readValue(body, QueryRequest.class);

        assertEquals(0, request.getFrom());
        assertEquals(0, request.getLimit());
        assertFalse(request.isTrackTotalCount());
        assertFalse(request.isQueryAsOwner());
    }

    @Test
    void should_acceptOmittedOffset_whenDeserializingQueryRequest() throws Exception {
        String body = """
                {
                  "kind": ["0:0:0:0"]
                }
                """;

        QueryRequest request = objectMapper.readValue(body, QueryRequest.class);

        assertEquals(0, request.getFrom());
    }
}
