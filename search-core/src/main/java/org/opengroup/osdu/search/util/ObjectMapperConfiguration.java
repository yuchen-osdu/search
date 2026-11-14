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

package org.opengroup.osdu.search.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Primary Jackson 3 mapper, used for HTTP request and response bodies. It replaces both Boot's mapper and
 * os-core-common's NON_NULL {@code jsonObjectMapper}, as the Jackson 2 {@code @Primary} mapper did before, and pins
 * the Jackson 2 behaviour the search API contract relies on: null values stay in responses (including null fields in
 * result records), unknown request properties are ignored, {@code null} for a primitive request field means the
 * default value, and trailing content after the request body is ignored.
 */
@Configuration
public class ObjectMapperConfiguration {

    // Declared as JsonMapper: Boot's mapper is @ConditionalOnMissingBean(JsonMapper.class), and the HTTP message
    // converter injects a JsonMapper, so @Primary picks this one over os-core-common's jsonObjectMapper.
    @Bean
    @Primary
    public JsonMapper objectMapper() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
