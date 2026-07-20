package org.opengroup.osdu.step_definitions.info;

import java.util.List;
import org.opengroup.osdu.core.test.auth.UserType;
import org.opengroup.osdu.core.test.cucumber.BaseGetInfoCucumberAcceptanceTests;
import org.opengroup.osdu.core.test.service.ServiceType;

@SuppressWarnings("unused")
public class InfoSteps extends BaseGetInfoCucumberAcceptanceTests {

    private static final List<String> EXPECTED_FEATURE_FLAGS = List.of(
        "featureFlag.autocomplete.enabled",
        "featureFlag.policy.enabled",
        "collaborations-enabled",
        "query-with-search-after"
    );

    static {
        System.setProperty("expose_featureflag.enabled", "true");
    }

    public InfoSteps() {
        super(UserType.PRIVILEGED_USER, ServiceType.SEARCH_V2, EXPECTED_FEATURE_FLAGS);
    }
}
