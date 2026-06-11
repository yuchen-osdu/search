package org.opengroup.osdu.step_definitions.querybycursor.search_after;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.opengroup.osdu.common.BaseAcceptanceTestSuite;
import org.opengroup.osdu.common.CucumberGlue;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/querybycursor/singlecluster/QueryByCursor.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = CucumberGlue.QUERY_BY_CURSOR)
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty,junit:target/cucumber-reports/TEST-querybysearchafter-sc.xml,io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm")
public class QueryByCursorSearchAfterAcceptanceTests extends BaseAcceptanceTestSuite {
}
