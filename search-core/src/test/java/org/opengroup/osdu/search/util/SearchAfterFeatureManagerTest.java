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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.SEARCH_AFTER_FEATURE_NAME;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchAfterFeatureManagerTest {

    @Mock
    private BooleanFeatureFlagClient booleanFeatureFlagClient;

    @InjectMocks
    private SearchAfterFeatureManager searchAfterFeatureManager;

    @Test
    void isEnabled_returnsTrue_whenClientReturnsTrue() {
        when(booleanFeatureFlagClient.isEnabled(SEARCH_AFTER_FEATURE_NAME, true)).thenReturn(true);

        assertTrue(searchAfterFeatureManager.isEnabled());
        verify(booleanFeatureFlagClient).isEnabled(SEARCH_AFTER_FEATURE_NAME, true);
    }

    @Test
    void isEnabled_returnsFalse_whenClientReturnsFalse() {
        when(booleanFeatureFlagClient.isEnabled(SEARCH_AFTER_FEATURE_NAME, true)).thenReturn(false);

        assertFalse(searchAfterFeatureManager.isEnabled());
        verify(booleanFeatureFlagClient).isEnabled(SEARCH_AFTER_FEATURE_NAME, true);
    }
}
