package org.opengroup.osdu.step_definitions.swagger;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.hc.core5.http.HttpStatus;
import org.opengroup.osdu.common.BaseSearchSteps;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.core.test.client.model.OpenApiSpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("unused")
public class SwaggerSteps extends BaseSearchSteps {

    private HttpResponse<OpenApiSpec> response;

    @Before
    public void before(Scenario scenario) {
        initScenario(scenario);
    }

    @When("I send get request to swagger endpoint")
    public void i_send_get_request_to_swagger_endpoint() {
        response = executeGetApiDocs(headers);
    }

    @Then("I should get openapi spec in response")
    public void i_should_get_openapi_spec_in_response() {
        OpenApiSpec spec = response.body();
        assertNotNull(spec.openapi());
        assertNotNull(spec.info());
        assertNotNull(spec.servers());
        assertNotNull(spec.security());
        assertNotNull(spec.tags());
        assertNotNull(spec.paths());
        assertNotNull(spec.components());
        assertEquals(HttpStatus.SC_OK, response.statusCode());
    }
}
