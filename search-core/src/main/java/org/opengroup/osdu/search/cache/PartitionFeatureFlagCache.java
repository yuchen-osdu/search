/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.search.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.stereotype.Component;

/**
 * Cache for feature flag values keyed by data partition and flag name.
 * Used when resolving flags for multiple partitions (for example, the {@code /info} endpoint).
 */
@Component
public class PartitionFeatureFlagCache {

    private final VmCache<String, Boolean> cache = new VmCache<>(600, 1000);

    public Boolean get(String featureName, String partitionId) {
        return cache.get(cacheKey(featureName, partitionId));
    }

    public void put(String featureName, String partitionId, boolean value) {
        cache.put(cacheKey(featureName, partitionId), value);
    }

    private static String cacheKey(String featureName, String partitionId) {
        return partitionId + "-" + featureName;
    }
}
