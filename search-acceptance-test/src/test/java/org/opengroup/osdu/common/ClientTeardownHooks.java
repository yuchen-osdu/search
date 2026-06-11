package org.opengroup.osdu.common;

import io.cucumber.java.AfterAll;

public final class ClientTeardownHooks {

    private ClientTeardownHooks() {
    }

    @AfterAll
    public static void tearDownTrackedClients() {
        BaseSearchSteps.tearDownTrackedResources();
    }
}
