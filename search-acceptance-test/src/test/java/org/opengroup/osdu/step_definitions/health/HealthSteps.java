/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.step_definitions.health;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.apache.hc.core5.http.HttpStatus;
import org.opengroup.osdu.common.BaseSearchSteps;
import org.opengroup.osdu.core.test.client.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unused")
public class HealthSteps extends BaseSearchSteps {

    private HttpResponse<Void> response;

    @Before
    public void before(Scenario scenario) {
        initScenario(scenario);
    }

    @Given("I send get request to liveness check endpoint")
    public void i_send_get_request_to_liveness_endpoint() {
        response = searchClient().livenessCheck(headers);
        logResponse(response);
    }

    @Given("I send get request to readiness check endpoint")
    public void i_send_get_request_to_readiness_endpoint() {
        response = searchClient().readinessCheck(headers);
        logResponse(response);
    }

    @Then("service should respond back with 200 in response")
    public void i_should_get_OK_in_response() {
        assertEquals(HttpStatus.SC_OK, response.statusCode());
    }
}
