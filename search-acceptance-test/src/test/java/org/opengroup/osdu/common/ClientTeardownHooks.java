package org.opengroup.osdu.common;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

public final class ClientTeardownHooks {

    private ClientTeardownHooks() {
    }

    @BeforeAll
    public static void setUpSuiteLegalTag() {
        BaseSearchSteps.setupSuiteLegalTag();
    }

    @AfterAll
    public static void tearDownTrackedClients() {
        BaseSearchSteps.tearDownTrackedResources();
        BaseSearchSteps.tearDownSuiteLegalTag();
    }
}
