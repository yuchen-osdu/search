/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.search.util;

import static org.opengroup.osdu.core.common.Constants.COLLABORATIONS_FEATURE_NAME;
import static org.opengroup.osdu.core.common.feature.PartitionFeatureFlagImpl.FF_SOURCE_DATA_PARTITION;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.AUTOCOMPLETE_FEATURE_NAME;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.POLICY_FEATURE_NAME;
import static org.opengroup.osdu.search.config.SearchConfigurationProperties.SEARCH_AFTER_FEATURE_NAME;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.feature.CommonFeatureFlagStateResolverUtil;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.model.info.FeatureFlagStateResolver;
import org.opengroup.osdu.core.common.model.info.FeatureFlagStateResolver.FeatureFlagState;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.search.config.FeatureConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = FeatureConstants.EXPOSE_FEATUREFLAG_ENABLED_PROPERTY, havingValue = "true", matchIfMissing = true)
public class FeatureFlagStateConfiguration {

    private final ITenantInfoService tenantInfoService;
    private final IFeatureFlag featureFlagService;
    private final BooleanFeatureFlagClient booleanFeatureFlagClient;

    @Bean
    @RequestScope
    public FeatureFlagStateResolver autocompleteFFResolver() {
        return CommonFeatureFlagStateResolverUtil.buildCommonFFStateResolver(
            AUTOCOMPLETE_FEATURE_NAME,
            tenantInfoService,
            featureFlagService
        );
    }

    @Bean
    @RequestScope
    public FeatureFlagStateResolver policyFFResolver() {
        return CommonFeatureFlagStateResolverUtil.buildCommonFFStateResolver(
            POLICY_FEATURE_NAME,
            tenantInfoService,
            featureFlagService
        );
    }

    @Bean
    @RequestScope
    public FeatureFlagStateResolver collaborationFFResolver() {
        return CommonFeatureFlagStateResolverUtil.buildCommonFFStateResolver(
            COLLABORATIONS_FEATURE_NAME,
            tenantInfoService,
            featureFlagService
        );
    }

    @Bean
    @RequestScope
    public FeatureFlagStateResolver searchAfterFFResolver() {
        // Partition lookups are cached per data partition in BooleanFeatureFlagClient.
        return () -> tenantInfoService.getAllTenantInfos().stream()
            .map(tenantInfo -> FeatureFlagState.builder()
                .partition(tenantInfo.getName())
                .name(SEARCH_AFTER_FEATURE_NAME)
                .source(FF_SOURCE_DATA_PARTITION)
                .enabled(booleanFeatureFlagClient.isEnabled(
                    SEARCH_AFTER_FEATURE_NAME, true, tenantInfo.getName()))
                .build())
            .toList();
    }
}
