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

package org.opengroup.osdu.search.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartitionFeatureFlagCacheTest {

    private PartitionFeatureFlagCache cache;

    @BeforeEach
    void setUp() {
        cache = new PartitionFeatureFlagCache();
    }

    @Test
    void get_returnsNull_whenValueNotCached() {
        assertNull(cache.get("query-with-search-after", "opendes"));
    }

    @Test
    void put_andGet_returnsStoredTrueValue() {
        cache.put("query-with-search-after", "opendes", true);

        assertTrue(cache.get("query-with-search-after", "opendes"));
    }

    @Test
    void put_andGet_returnsStoredFalseValue() {
        cache.put("query-with-search-after", "opendes", false);

        assertFalse(cache.get("query-with-search-after", "opendes"));
    }

    @Test
    void cachesValuesIndependentlyPerPartition() {
        cache.put("query-with-search-after", "partition-a", true);
        cache.put("query-with-search-after", "partition-b", false);

        assertTrue(cache.get("query-with-search-after", "partition-a"));
        assertFalse(cache.get("query-with-search-after", "partition-b"));
    }

    @Test
    void cachesValuesIndependentlyPerFeatureName() {
        cache.put("flag-a", "opendes", true);
        cache.put("flag-b", "opendes", false);

        assertTrue(cache.get("flag-a", "opendes"));
        assertFalse(cache.get("flag-b", "opendes"));
    }
}
