package org.opengroup.osdu.step_definitions.querybycursor.singlecluster;

import com.google.gson.Gson;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.hc.core5.http.HttpStatus;
import org.opengroup.osdu.common.BaseSearchSteps;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CursorQueryRequest;
import org.opengroup.osdu.core.common.model.search.CursorQueryResponse;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.core.test.client.ClientException;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.util.Utility;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.core.test.util.ResponseUtil.fromJson;
import static org.opengroup.osdu.util.Utility.parseCommaSeparatedList;

@SuppressWarnings("unused")
public class QueryByCursorSteps extends BaseSearchSteps {

    private CursorQueryRequest requestQuery = new CursorQueryRequest();

    @Before
    public void before(Scenario scenario) {
        initScenario(scenario);
    }

    @Given("the schema is created with the following kind")
    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
        super.the_schema_is_created_with_the_following_kind(dataTable);
    }

    @When("I ingest records with the {string} with {string} for a given {string}")
    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {
        super.i_ingest_records_with_the_for_a_given(record, dataGroup, kind);
    }

    @When("I limit the count of returned results to {int}")
    public void i_limit_the_count_of_returned_results_to(int limit) {
        requestQuery.setLimit(limit);
    }

    @When("^I send None with (?!\\d+ copies of )(.+)$")
    public void i_send_None_with(String kind) {
        headers.putIfAbsent(DpsHeaders.DATA_PARTITION_ID, getTenantMapping("tenant1"));
        requestQuery.setQuery(null);
        requestQuery.setKind(resolveKindForRequest(kind));
    }

    @When("^I send (?!None with )\"(.*)\" with \"([^\"]+)\"$")
    public void i_send_with_quoted_query(String query, String kind) {
        i_send_with(query, kind);
    }

    @When("^I send (?!None with )(?!\"|\\[)(.+) with \"([^\"]+)\"$")
    public void i_send_with_unquoted_query(String query, String kind) {
        i_send_with(query, kind);
    }

    public void i_send_with(String query, String kind) {
        headers.putIfAbsent(DpsHeaders.DATA_PARTITION_ID, getTenantMapping("tenant1"));
        requestQuery.setQuery(query);
        requestQuery.setKind(resolveKindForRequest(kind));
    }

    @When("^I want the results sorted by (.*?)$")
    public void i_sort_with(String sortJson) {
        SortQuery sortArg = fromJson(new Gson(), sortJson, SortQuery.class);
        requestQuery.setSort(sortArg);
    }

    @When("^I want to search as owner (.*?)$")
    public void i_search_as(String isOwner) {
        requestQuery.setQueryAsOwner(Boolean.parseBoolean(isOwner));
    }

    @When("^I set the fields I want in response as (.*) and (.*)$")
    public void i_set_the_fields_I_want_in_response_with_returnedFields_but_not_excludedFields(
        String returnedFields, String excludedFields) {
        setReturnedAndExcludedFields(parseCommaSeparatedList(returnedFields), parseCommaSeparatedList(excludedFields));
    }

    @When("^I set the fields I want in response as ((?:(?! and ).)+)$")
    public void i_set_the_fields_I_want_in_response_as(String returnedFields) {
        setReturnedFields(parseCommaSeparatedList(returnedFields));
    }

    @When("^I set autocomplete phrase to (.*?)$")
    public void i_set_autocomplete_phrase(String autocompletePhrase) {
        requestQuery.setSuggestPhrase(autocompletePhrase);
    }

    @When("I limit the count of returned results to None")
    public void limit_of_returned_results_as_None() {
    }

    @When("I send request to tenant {string}")
    public void i_send_request_to_tenant_step(String tenant) {
        i_send_request_to_tenant(tenant);
    }

    @When("I apply geographical query on field {string}")
    public void i_apply_geographical_query_on_field(String field) {
        spatialFilter.setField(field);
        requestQuery.setSpatialFilter(spatialFilter);
    }

    @When("^define bounding box with points \\((-?\\d+), (-?\\d+)\\) and  \\((-?\\d+), (-?\\d+)\\)$")
    public void define_bounding_box_with_points_and_step(Double topLatitude, Double topLongitude, Double bottomLatitude,
        Double bottomLongitude) {
        define_bounding_box_with_points_and(topLatitude, topLongitude, bottomLatitude, bottomLongitude);
    }

    @When("^I set an invalid cursor$")
    public void i_set_an_invalid_cursor() {
        requestQuery.setCursor("invalid cursor");
    }

    @When("^I send a subsequent request with only the kind and cursor$")
    public void i_send_a_subsequent_request_with_only_the_kind_and_cursor() {
        requestQuery = nextPageRequest();
    }

    @When("^I send a subsequent request with the cursor, limit (\\d+) and fields (.*)$")
    public void i_send_a_subsequent_request_with_the_cursor_limit_and_fields(int limit, String returnedFields) {
        CursorQueryRequest nextPageRequest = nextPageRequest();
        nextPageRequest.setLimit(limit);
        nextPageRequest.setReturnedFields(parseCommaSeparatedList(returnedFields));
        requestQuery = nextPageRequest;
    }

    @Then("^I should get in response (\\d+) records along with a cursor$")
    public void i_should_get_in_response_records_along_with_a_cursor(int resultCount) {
        HttpResponse<CursorQueryResponse> response = executeCursorQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(resultCount, response.body().getResults().size());
        requestQuery.setCursor(response.body().getCursor());
    }

    @Then("^I should get in response (\\d+) records containing (.*)$")
    public void i_should_get_in_response_records_containing_fields(int resultCount, String fields) {
        assertRecordsContainingFields(resultCount, parseCommaSeparatedList(fields));
    }

    @Then("^I should get in response (\\d+) records not containing (.*)$")
    public void i_should_get_in_response_records_not_containing_fields(int resultCount, String fields) {
        assertRecordsNotContainingFields(resultCount, parseCommaSeparatedList(fields));
    }

    @Then("^I should get in response (\\d+) records$")
    public void i_should_get_in_response_records(int resultCount) {
        HttpResponse<CursorQueryResponse> response = executeCursorQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(resultCount, response.body().getResults().size());
        if (resultCount == 0) {
            assertNull(response.body().getCursor());
        }
    }

    @Then("I should get records in right order first record id: {string}, last record id: {string}")
    public void i_should_get_records_in_right_order(String firstRecId, String lastRecId) {
        String actualFirstRecordId = generateActualName(firstRecId);
        String actualLastRecordId = generateActualName(lastRecId);
        HttpResponse<CursorQueryResponse> response = executeCursorQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertNotNull(response.body().getResults());
        assertFalse(response.body().getResults().isEmpty());
        assertEquals(actualFirstRecordId, response.body().getResults().get(0).get("id").toString());
        assertEquals(actualLastRecordId,
            response.body().getResults().get(response.body().getResults().size() - 1).get("id").toString());
    }

    @Then("I should get {int} response with reason: {string}, message: {string} and errors: {string}")
    public void i_should_get_response_with_reason_message_and_errors(int responseCode, String type, String msg,
        String error) {
        ClientException exception = executeCursorQueryExpectingError(requestQuery, headers);
        AppError appError = exception.getError();
        assertNotNull(appError);
        assertEquals(type, appError.getReason());
        assertEquals(generateActualName(msg), appError.getMessage());
        assertEquals(responseCode, exception.getStatusCode());
        if (appError.getErrors() != null && appError.getErrors().length > 0) {
            assertEquals(generateActualName(error), appError.getErrors()[0]);
        }
    }

    @Then("^I should get following autocomplete suggestions (.*)")
    public void i_should_get_following_autocomplete_suggestions(String autocompleteOptions) {
        HttpResponse<QueryResponse> response = executeQuery(toQueryRequest(requestQuery), headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(Arrays.asList(autocompleteOptions.split(",")), response.body().getPhraseSuggestions());
    }

    @Then("^I should get in response (\\d+) records with exactly (.*)$")
    public void i_should_get_in_response_records_with_exactly(int count, String fields) {
        List<String> expectedFields = parseCommaSeparatedList(fields);
        HttpResponse<CursorQueryResponse> response = executeCursorQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(count, response.body().getResults().size());

        for (Map<String, Object> result : response.body().getResults()) {
            Set<String> actualFieldPaths = flattenFieldPaths("", result);
            for (String expectedField : expectedFields) {
                assertTrue(Utility.containsField(result, expectedField),
                        "Expected field missing: " + expectedField);
            }
            for (String actualField : actualFieldPaths) {
                assertTrue(
                        expectedFields.stream().anyMatch(f -> actualField.equals(f) || actualField.startsWith(f + ".")),
                        "Unexpected field in response: " + actualField
                                + " — the effective returnedFields were not honored");
            }
        }
    }

    private Set<String> flattenFieldPaths(String prefix, Map<String, Object> map) {
        Set<String> paths = new HashSet<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                paths.addAll(flattenFieldPaths(path, (Map<String, Object>) entry.getValue()));
            } else {
                paths.add(path);
            }
        }
        return paths;
    }

    private void setReturnedFields(List<String> returnedFields) {
        if (returnedFields.contains("NULL")) {
            requestQuery.setReturnedFields(null);
        } else if (!returnedFields.contains("All")) {
            requestQuery.setReturnedFields(returnedFields);
        }
    }

    private void setReturnedAndExcludedFields(List<String> returnedFields, List<String> excludedFields) {
        setReturnedFields(returnedFields);
        if (excludedFields.contains("NULL")) {
            requestQuery.setExcludedFields(null);
        } else if (!excludedFields.contains("All")) {
            requestQuery.setExcludedFields(excludedFields);
        }
    }

    private void assertRecordsContainingFields(int resultCount, List<String> fields) {
        HttpResponse<CursorQueryResponse> response = executeCursorQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(resultCount, response.body().getResults().size());

        if (fields.contains("NULL") || fields.contains("All")) {
            return;
        }

        for (Map<String, Object> results : response.body().getResults()) {
            for (String includedField : fields) {
                assertTrue(Utility.containsField(results, includedField));
            }
        }
    }

    private void assertRecordsNotContainingFields(int resultCount, List<String> fields) {
        HttpResponse<CursorQueryResponse> response = executeCursorQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(resultCount, response.body().getResults().size());

        if (fields.contains("NULL") || fields.contains("All")) {
            return;
        }

        for (Map<String, Object> results : response.body().getResults()) {
            for (String excludedField : fields) {
                assertFalse(Utility.containsField(results, excludedField));
            }
        }
    }

    private static QueryRequest toQueryRequest(CursorQueryRequest cursorQuery) {
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setKind(cursorQuery.getKind());
        queryRequest.setLimit(cursorQuery.getLimit());
        queryRequest.setQuery(cursorQuery.getQuery());
        queryRequest.setReturnedFields(cursorQuery.getReturnedFields());
        queryRequest.setExcludedFields(cursorQuery.getExcludedFields());
        queryRequest.setSort(cursorQuery.getSort());
        queryRequest.setQueryAsOwner(cursorQuery.isQueryAsOwner());
        queryRequest.setSuggestPhrase(cursorQuery.getSuggestPhrase());
        queryRequest.setSpatialFilter(cursorQuery.getSpatialFilter());
        return queryRequest;
    }

    private CursorQueryRequest nextPageRequest() {
        CursorQueryRequest nextPageRequest = new CursorQueryRequest();
        nextPageRequest.setCursor(requestQuery.getCursor());
        nextPageRequest.setKind(requestQuery.getKind());
        return nextPageRequest;
    }
}
