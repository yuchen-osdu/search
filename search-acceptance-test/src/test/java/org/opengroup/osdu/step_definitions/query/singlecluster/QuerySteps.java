package org.opengroup.osdu.step_definitions.query.singlecluster;

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
import org.opengroup.osdu.core.common.model.search.AggregationResponse;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.opengroup.osdu.core.test.client.ClientException;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.Utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.core.test.util.ResponseUtil.fromJson;
import static org.opengroup.osdu.util.Utility.parseCommaSeparatedList;

@SuppressWarnings("unused")
public class QuerySteps extends BaseSearchSteps {

    private final QueryRequest requestQuery = new QueryRequest();

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

    @When("I ingest records with the {string} with {string} for a given {string} with {string} header")
    public void i_ingest_records_with_the_for_a_given_with_xcollaboration_header(
            String record, String dataGroup, String kind, String xCollaborationHeader) {
        i_ingest_records_with_the_for_a_given_with_header(record, dataGroup, kind, xCollaborationHeader);
    }

    @When("I send request with {string} header")
    public void i_send_request_with_xcollaboration_header(String xCollaborationHeader) {
        i_send_request_with_xcollab_header(xCollaborationHeader);
    }

    @When("I limit the count of returned results to {int}")
    public void i_limit_the_count_of_returned_results_to(int limit) {
        requestQuery.setLimit(limit);
    }

    @When("^I send \"(.*?)\" with (\\d+) copies of \"(.*?)\"$")
    public void i_send_with_multi_kinds(String query, int number, String kind) {
        headers.putIfAbsent(DpsHeaders.DATA_PARTITION_ID, getTenantMapping("tenant1"));
        requestQuery.setQuery(query);
        Object resolvedKind = resolveKindForRequest(kind);
        if (!(resolvedKind instanceof String actualKind)) {
            requestQuery.setKind(resolvedKind);
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number; i++) {
            if (!builder.isEmpty()) {
                builder.append(",");
            }
            builder.append(actualKind);
        }
        requestQuery.setKind(builder.toString());
    }

    @When("^I send None with (\\d+) copies of \"(.*?)\"$")
    public void i_send_None_with_copies(int number, String kind) {
        i_send_with_multi_kinds(null, number, kind);
    }

    @When("^I send None with (?!\\d+ copies of )(.+)$")
    public void i_send_None_with(String kind) {
        headers.putIfAbsent(DpsHeaders.DATA_PARTITION_ID, getTenantMapping("tenant1"));
        Object resolvedKind = resolveKindForRequest(kind);
        requestQuery.setQuery(null);
        requestQuery.setKind(resolvedKind);
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
        requestQuery.setQuery(query == null || query.isBlank() ? null : query);
        requestQuery.setKind(resolveKindForRequest(kind));
    }

    @When("^I want the results sorted by (.*?)$")
    public void i_sort_with(String sortJson) {
        SortQuery sortArg = fromJson(new Gson(), sortJson, SortQuery.class);
        requestQuery.setSort(sortArg);
    }

    @When("^I want to aggregate by \"(.*?)\"$")
    public void i_aggregate_by(String aggField) {
        requestQuery.setAggregateBy(aggField);
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

    @When("I set the offset of starting point as None")
    public void offset_of_starting_point_as_None_step() {
    }

    @When("I set the offset of starting point as {int}")
    public void i_set_the_offset_of_starting_point_as(int offset) {
        requestQuery.setFrom(offset);
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

    @When("^define bounding box with points \\(None, None\\) and  \\((\\d+), (\\d+)\\)$")
    public void define_bounding_box_with_points_None_None_and(Double bottomLatitude, Double bottomLongitude) {
        Point bottomRight = new Point(bottomLatitude, bottomLongitude);
        byBoundingBox = new SpatialFilter.ByBoundingBox(null, bottomRight);
        spatialFilter.setByBoundingBox(byBoundingBox);
    }

    @When("^define focus coordinates as \\((-?\\d+), (-?\\d+)\\) and search in a (\\d+) radius$")
    public void define_focus_coordinates_as_and_search_in_a_radius(Double latitude, Double longitude, int distance) {
        Point coordinate = new Point(latitude, longitude);
        SpatialFilter.ByDistance byDistance = new SpatialFilter.ByDistance(distance, coordinate);
        spatialFilter.setByDistance(byDistance);
    }

    @When("^define intersection polygon with points \\((-?\\d+), (-?\\d+)\\) and \\((-?\\d+), (-?\\d+)\\) and \\((-?\\d+), (-?\\d+)\\) and \\((-?\\d+), (-?\\d+)\\) and \\((-?\\d+), (-?\\d+)\\)")
    public void define_intersection_polygon_with_points_step(Double latitude1, Double longitude1, Double latitude2,
        Double longitude2, Double latitude3, Double longitude3, Double latitude4, Double longitude4,
        Double latitude5, Double longitude5) {
        define_intersection_polygon_with_points(latitude1, longitude1, latitude2, longitude2, latitude3,
            longitude3, latitude4, longitude4, latitude5, longitude5);
    }

    @When("^define within polygon with points \\((-?\\d+), (-?\\d+)\\)")
    public void define_within_polygon_with_points_step(Double latitude1, Double longitude1) {
        define_within_polygon_with_points(latitude1, longitude1);
    }

    @When("^define geo polygon with following points (.*)$")
    public void define_geo_polygon_with_following_points_points_list(String points) {
        Pattern pattern = Pattern.compile("[(](.*?);(.*?)[)]");
        List<Point> coordinatesPoints = new ArrayList<>();
        for (String point : parseCommaSeparatedList(points)) {
            Matcher matcher = pattern.matcher(point.trim());
            if (matcher.matches()) {
                coordinatesPoints.add(new Point(
                    Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2))));
            }
        }
        SpatialFilter.ByGeoPolygon byGeopolygon = new SpatialFilter.ByGeoPolygon(coordinatesPoints);
        spatialFilter.setByGeoPolygon(byGeopolygon);
    }

    @Then("I should get in response {int} records")
    public void i_should_get_in_response_records(int resultCount) {
        HttpResponse<QueryResponse> response = executeQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(resultCount, response.body().getResults().size());
    }

    @Then("^I should get in response (\\d+) records when searchAs owner is (.*?)$")
    public void i_should_get_in_response_records_using_search_as_mode(int resultCount, String isOwner) {
        HttpResponse<QueryResponse> response = executeQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(resultCount, response.body().getResults().size());
    }

    @Then("^I should get in response (\\d+) records with (.*)$")
    public void i_should_get_in_response_records_with_fields(int resultCount, String returnedFields) {
        HttpResponse<QueryResponse> response = executeQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(resultCount, response.body().getResults().size());

        List<String> fields = parseCommaSeparatedList(returnedFields);
        if (fields.contains("NULL") || fields.contains("All")) {
            return;
        }

        for (Map<String, Object> results : response.body().getResults()) {
            assertTrue(results.keySet().containsAll(fields));
        }
    }

    @Then("^I should get in response (\\d+) records containing (.*)$")
    public void i_should_get_in_response_records_containing_fields(int resultCount, String fields) {
        assertRecordsContainingFields(resultCount, parseCommaSeparatedList(fields));
    }

    @Then("^I should get in response (\\d+) records not containing (.*)$")
    public void i_should_get_in_response_records_not_containing_fields(int resultCount, String fields) {
        assertRecordsNotContainingFields(resultCount, parseCommaSeparatedList(fields));
    }

    @Then("I should get records in right order first record id: {string}, last record id: {string}")
    public void i_should_get_records_in_right_order(String firstRecId, String lastRecId) {
        String actualFirstId = generateActualName(firstRecId);
        String actualLastId = generateActualName(lastRecId);
        HttpResponse<QueryResponse> response = executeQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertNotNull(response.body().getResults());
        assertFalse(response.body().getResults().isEmpty());
        assertEquals(actualFirstId, response.body().getResults().get(0).get("id").toString());
        assertEquals(actualLastId,
            response.body().getResults().get(response.body().getResults().size() - 1).get("id").toString());
    }

    @Then("I should get {int} unique values")
    public void i_should_get_aggregation_with_unique_values(int uniqueValueCount) {
        HttpResponse<QueryResponse> response = executeQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(uniqueValueCount, response.body().getAggregations().size());
        assertEquals(0, response.body().getAggregations().stream().filter(Objects::isNull).count());
    }

    @Then("^I should get \"(.+)\" and should not get \"(.+)\" from aggregations$")
    public void i_should_get_included_kinds_and_should_not_get_excluded_kinds_from_aggregation(
        String includedKinds, String excludedKinds) {
        List<String> expectedIncluded = resolveExpectedAggregationKinds(includedKinds);
        List<String> expectedExcluded = resolveExpectedAggregationKinds(excludedKinds);
        int maxAttempts = Config.getSearchIndexWaitMaxAttempts();
        int intervalSeconds = Config.getSearchIndexWaitIntervalSeconds();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            HttpResponse<QueryResponse> response = executeQuery(requestQuery, headers);
            assertEquals(HttpStatus.SC_OK, response.statusCode());
            Set<String> kinds = response.body().getAggregations().stream()
                .map(AggregationResponse::getKey)
                .collect(Collectors.toSet());

            boolean allIncludedPresent = expectedIncluded.stream().allMatch(kinds::contains);
            boolean allExcludedAbsent = expectedExcluded.stream().noneMatch(kinds::contains);
            if (allIncludedPresent && allExcludedAbsent) {
                return;
            }

            if (attempt < maxAttempts - 1) {
                try {
                    TimeUnit.SECONDS.sleep(intervalSeconds);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interruptedException);
                }
            }
        }

        HttpResponse<QueryResponse> response = executeQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        Set<String> kinds = response.body().getAggregations().stream()
            .map(AggregationResponse::getKey)
            .collect(Collectors.toSet());
        for (String kind : expectedIncluded) {
            assertTrue(kinds.contains(kind), "Expected included kind '" + kind + "' in aggregations " + kinds);
        }
        for (String kind : expectedExcluded) {
            assertFalse(kinds.contains(kind), "Did not expect excluded kind '" + kind + "' in aggregations " + kinds);
        }
    }

    @Then("I should get {int} response with reason: {string}, message: {string} and errors: {string}")
    public void i_should_get_response_with_reason_message_and_errors(int responseCode, String type, String msg,
        String error) {
        ClientException exception = executeQueryExpectingError(requestQuery, headers);
        AppError appError = exception.getError();
        assertNotNull(appError);
        assertEquals(type, appError.getReason());
        assertEquals(msg, appError.getMessage());
        assertEquals(responseCode, exception.getStatusCode());
        if (appError.getErrors() != null && appError.getErrors().length > 0) {
            assertEquals(generateActualName(error), appError.getErrors()[0]);
        }
    }

    @Then("^I should get following autocomplete suggestions (.*)")
    public void i_should_get_following_autocomplete_suggestions(String autocompleteOptions) {
        HttpResponse<QueryResponse> response = executeQuery(requestQuery, headers);
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals(Arrays.asList(autocompleteOptions.split(",")), response.body().getPhraseSuggestions());
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
        HttpResponse<QueryResponse> response = executeQuery(requestQuery, headers);
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
        HttpResponse<QueryResponse> response = executeQuery(requestQuery, headers);
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

    private List<String> resolveExpectedAggregationKinds(String kinds) {
        return Arrays.stream(kinds.split(","))
            .map(String::trim)
            .map(this::generateActualName)
            .toList();
    }
}
