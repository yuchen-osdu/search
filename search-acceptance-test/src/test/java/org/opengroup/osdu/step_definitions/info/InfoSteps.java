package org.opengroup.osdu.step_definitions.info;

import org.opengroup.osdu.core.test.auth.UserType;
import org.opengroup.osdu.core.test.cucumber.BaseGetInfoCucumberAcceptanceTests;
import org.opengroup.osdu.core.test.service.ServiceType;

import java.util.List;

@SuppressWarnings("unused")
public class InfoSteps extends BaseGetInfoCucumberAcceptanceTests {

    public InfoSteps() {
        super(UserType.PRIVILEGED_USER, ServiceType.SEARCH_V2, List.of());
    }
}
