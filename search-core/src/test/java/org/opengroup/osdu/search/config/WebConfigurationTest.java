package org.opengroup.osdu.search.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class WebConfigurationTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
        .addFilters(new WebConfiguration().trailingSlashFilter().getFilter())
        .build();

    @ParameterizedTest
    @ValueSource(strings = {"/query", "/query/"})
    void shouldPreserveBodyAndQueryWithEitherTrailingSlash(String path) throws Exception {
        mvc.perform(post(path).queryParam("mode", "full")
                .contentType(MediaType.APPLICATION_JSON).content("{\"kind\":\"a:b:c:1.0.0\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string("full:{\"kind\":\"a:b:c:1.0.0\"}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/health/liveness_check", "/health/liveness_check/"})
    void shouldMatchGetEndpointWithEitherTrailingSlash(String path) throws Exception {
        mvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(content().string("alive"));
    }

    @RestController
    static class ProbeController {
        @PostMapping("/query")
        public String query(@RequestParam("mode") String mode, @RequestBody String body) {
            return mode + ":" + body;
        }

        @GetMapping("/health/liveness_check")
        public String liveness() {
            return "alive";
        }
    }
}
