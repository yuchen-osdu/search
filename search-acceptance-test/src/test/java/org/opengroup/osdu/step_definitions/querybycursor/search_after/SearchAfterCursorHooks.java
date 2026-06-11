package org.opengroup.osdu.step_definitions.querybycursor.search_after;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.opengroup.osdu.common.BaseSearchSteps;

@SuppressWarnings("unused")
public class SearchAfterCursorHooks {

    @Before(order = Integer.MIN_VALUE)
    public void enableSearchAfterCursor() {
        BaseSearchSteps.enableSearchAfterCursor();
    }

    @After
    public void disableSearchAfterCursor() {
        BaseSearchSteps.disableSearchAfterCursor();
    }
}
