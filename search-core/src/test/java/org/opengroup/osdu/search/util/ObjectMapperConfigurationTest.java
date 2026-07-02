package org.opengroup.osdu.search.util;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.model.search.QueryRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectMapperConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapperConfiguration().objectMapper();

    @Test
    void should_rejectNullOffset_whenDeserializingQueryRequest() {
        String body = """
                {
                  "kind": ["0:0:0:0"],
                  "offset": null
                }
                """;

        assertThrows(JsonMappingException.class, () -> objectMapper.readValue(body, QueryRequest.class));
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
