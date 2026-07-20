package org.opengroup.osdu.common;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class CucumberGlue {

    private static final String COMMON = "org.opengroup.osdu.common";

    public static final String HEALTH = COMMON + ",org.opengroup.osdu.step_definitions.health";
    public static final String INFO = COMMON + ",org.opengroup.osdu.step_definitions.info";
    public static final String SWAGGER = COMMON + ",org.opengroup.osdu.step_definitions.swagger";
    public static final String QUERY_SINGLE_CLUSTER =
        COMMON + ",org.opengroup.osdu.step_definitions.query.singlecluster";
    public static final String QUERY_BY_CURSOR =
        COMMON + ",org.opengroup.osdu.step_definitions.querybycursor";
}
