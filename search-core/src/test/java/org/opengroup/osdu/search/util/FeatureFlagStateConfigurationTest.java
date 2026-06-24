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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.core.common.Constants.COLLABORATIONS_FEATURE_NAME;
import static org.opengroup.osdu.core.common.feature.PartitionFeatureFlagImpl.FF_SOURCE_DATA_PARTITION;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.AUTOCOMPLETE_FEATURE_NAME;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.POLICY_FEATURE_NAME;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.SEARCH_AFTER_FEATURE_NAME;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.model.info.FeatureFlagStateResolver.FeatureFlagState;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;

@ExtendWith(MockitoExtension.class)
class FeatureFlagStateConfigurationTest {

    private static final String PARTITION_A = "partition-a";
    private static final String PARTITION_B = "partition-b";
    private static final String APP_PROPERTY_SOURCE = "appProperty";

    @Mock
    private ITenantInfoService tenantInfoService;

    @Mock
    private IFeatureFlag featureFlagService;

    @Mock
    private BooleanFeatureFlagClient booleanFeatureFlagClient;

    private FeatureFlagStateConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new FeatureFlagStateConfiguration(
            tenantInfoService,
            featureFlagService,
            booleanFeatureFlagClient
        );
    }

    @Test
    void autocompleteFFResolver_returnsFeatureFlagStatePerTenant() {
        stubTenantsAndFeatureFlagSource();
        when(featureFlagService.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME, PARTITION_A)).thenReturn(true);
        when(featureFlagService.isFeatureEnabled(AUTOCOMPLETE_FEATURE_NAME, PARTITION_B)).thenReturn(false);

        List<FeatureFlagState> states = configuration.autocompleteFFResolver().retrieveStates();

        assertEquals(2, states.size());
        assertTrue(states.stream().anyMatch(state ->
            AUTOCOMPLETE_FEATURE_NAME.equals(state.getName())
                && PARTITION_A.equals(state.getPartition())
                && APP_PROPERTY_SOURCE.equals(state.getSource())
                && state.isEnabled()));
        assertTrue(states.stream().anyMatch(state ->
            AUTOCOMPLETE_FEATURE_NAME.equals(state.getName())
                && PARTITION_B.equals(state.getPartition())
                && !state.isEnabled()));
    }

    @Test
    void policyFFResolver_returnsFeatureFlagStatePerTenant() {
        stubTenantsAndFeatureFlagSource();
        when(featureFlagService.isFeatureEnabled(POLICY_FEATURE_NAME, PARTITION_A)).thenReturn(false);
        when(featureFlagService.isFeatureEnabled(POLICY_FEATURE_NAME, PARTITION_B)).thenReturn(true);

        List<FeatureFlagState> states = configuration.policyFFResolver().retrieveStates();

        assertEquals(2, states.size());
        assertTrue(states.stream().anyMatch(state ->
            POLICY_FEATURE_NAME.equals(state.getName()) && PARTITION_B.equals(state.getPartition()) && state.isEnabled()));
    }

    @Test
    void collaborationFFResolver_returnsFeatureFlagStatePerTenant() {
        stubTenantsAndFeatureFlagSource();
        when(featureFlagService.isFeatureEnabled(COLLABORATIONS_FEATURE_NAME, PARTITION_A)).thenReturn(true);
        when(featureFlagService.isFeatureEnabled(COLLABORATIONS_FEATURE_NAME, PARTITION_B)).thenReturn(true);

        List<FeatureFlagState> states = configuration.collaborationFFResolver().retrieveStates();

        assertEquals(2, states.size());
        assertTrue(states.stream().allMatch(state ->
            COLLABORATIONS_FEATURE_NAME.equals(state.getName()) && state.isEnabled()));
    }

    @Test
    void searchAfterFFResolver_returnsPartitionBackedFeatureFlagStatePerTenant() {
        when(tenantInfoService.getAllTenantInfos()).thenReturn(List.of(tenant(PARTITION_A), tenant(PARTITION_B)));
        when(booleanFeatureFlagClient.isEnabled(SEARCH_AFTER_FEATURE_NAME, true, PARTITION_A)).thenReturn(true);
        when(booleanFeatureFlagClient.isEnabled(SEARCH_AFTER_FEATURE_NAME, true, PARTITION_B)).thenReturn(false);

        List<FeatureFlagState> states = configuration.searchAfterFFResolver().retrieveStates();

        assertEquals(2, states.size());
        assertTrue(states.stream().anyMatch(state ->
            SEARCH_AFTER_FEATURE_NAME.equals(state.getName())
                && PARTITION_A.equals(state.getPartition())
                && FF_SOURCE_DATA_PARTITION.equals(state.getSource())
                && state.isEnabled()));
        assertTrue(states.stream().anyMatch(state ->
            SEARCH_AFTER_FEATURE_NAME.equals(state.getName())
                && PARTITION_B.equals(state.getPartition())
                && !state.isEnabled()));
        verify(booleanFeatureFlagClient).isEnabled(SEARCH_AFTER_FEATURE_NAME, true, PARTITION_A);
        verify(booleanFeatureFlagClient).isEnabled(SEARCH_AFTER_FEATURE_NAME, true, PARTITION_B);
    }

    private void stubTenantsAndFeatureFlagSource() {
        when(tenantInfoService.getAllTenantInfos()).thenReturn(List.of(tenant(PARTITION_A), tenant(PARTITION_B)));
        when(featureFlagService.source()).thenReturn(APP_PROPERTY_SOURCE);
    }

    private static TenantInfo tenant(String name) {
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setName(name);
        return tenantInfo;
    }
}
