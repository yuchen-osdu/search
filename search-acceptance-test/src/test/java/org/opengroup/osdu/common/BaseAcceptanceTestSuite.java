package org.opengroup.osdu.common;

public abstract class BaseAcceptanceTestSuite {

    protected static void tearDownTrackedClients() {
        BaseSearchSteps.tearDownTrackedResources();
    }
}
