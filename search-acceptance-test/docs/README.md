### Search acceptance tests

End-to-end Cucumber tests for the OSDU Search v2 service. Tests use **os-core-test** for HTTP clients, authentication, shared `/info` assertions, and entitlements role verification.

### Prerequisites

Export the variables below (or place them in a `.env` file loaded by `os-core-test`).

| name                                | value                                                                      | description                                          | sensitive? | required |
|-------------------------------------|----------------------------------------------------------------------------|------------------------------------------------------|------------|----------|
| `HOST`                              | eg `https://osdu.core-dev.gcp.gnrg-osdu.projects.epam.com`                 | Base URL for the OSDU platform                       | no         | yes      |
| `DATA_PARTITION_ID`                 | eg `osdu`                                                                  | Primary data partition ID (tenant 1)                 | no         | yes      |
| `DEFAULT_DATA_PARTITION_ID_TENANT2` | eg `non-exist`                                                             | Non-existing tenant for negative tests               | no         | yes      |
| `ENTITLEMENTS_DOMAIN`               | eg `group`                                                                 | Domain name for entitlements service                 | no         | yes      |
| `GROUP_ID`                          | eg `group`                                                                 | Group ID used in test data and ACLs                  | no         | yes      |

Legal tags for record ingestion are created automatically before each Cucumber suite and deleted afterward (country of origin: `US`). No `LEGAL_TAG` or `OTHER_RELEVANT_DATA_COUNTRIES` configuration is required.

Authentication via OIDC:

| name                                            | value                                   | description                                           | sensitive? |
|------------------------------------------------|-----------------------------------------|-------------------------------------------------------|------------|
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_ID`     | `********`                              | Client ID for privileged user authentication         | yes        |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********`                              | Client secret for privileged user authentication     | yes        |
| `TEST_OPENID_PROVIDER_URL`                      | `https://keycloak.com/auth/realms/osdu` | OpenID Connect provider URL                          | yes        |

Or provide a token directly:

| name                    | value      | description                                     | sensitive? |
|-------------------------|------------|-------------------------------------------------|------------|
| `PRIVILEGED_USER_TOKEN` | `********` | Bearer token for `PRIVILEGED_USER`              | yes        |

### Optional environment variables

| name                              | value            | description                                                                                  |
|-----------------------------------|------------------|----------------------------------------------------------------------------------------------|
| `SEARCH_INDEX_WAIT_MAX_ATTEMPTS`  | eg `30`          | Max attempts when waiting for indexed records (default: `30`)                                |
| `SEARCH_INDEX_WAIT_INTERVAL_SECONDS` | eg `10`       | Seconds between index-wait attempts (default: `10`)                                            |
| `EXPOSE_FEATUREFLAG_ENABLED`      | `true` / `false` | When `true`, `/info` tests assert exposed feature flags                                        |
| `USER_REQUIRED_ROLES_CONFIG`      | path             | Override path to `required-roles.json` (default: classpath `src/test/resources/required-roles.json`) |
| `cucumber.filter.tags`            | Cucumber expression | Override tag filter (see [Cucumber tags](#cucumber-tags) below)                          |

### Entitlements roles

Roles are defined in `src/test/resources/required-roles.json` as a map of user type to role list (for example `"PRIVILEGED_USER": ["users", ...]`). **os-core-test** verifies entitlements for configured user types at startup when this file is present; there is no separate enable/disable environment variable.

The integration account behind `PRIVILEGED_USER` must have at least:

| PRIVILEGED_USER           |
|---------------------------|
| users                     |
| service.entitlements.user |
| service.search.user       |
| data.test1                |
| data.integration.test     |

Also assign the deployment-specific group email `users@{DATA_PARTITION_ID}@{GROUP_ID}.com` in entitlements (not listed in `required-roles.json` because it varies by environment).

### Test suites

Each row is a JUnit Platform suite class run by `mvn test`.

| Suite class | Feature file | Notes |
|-------------|--------------|-------|
| `HealthAcceptanceTests` | `features/health/Health.feature` | 2 scenarios (`@health`) |
| `InfoAcceptanceTests` | `features/info/Info.feature` | 2 scenarios (`@default`) |
| `SwaggerAcceptanceTests` | `features/swagger/Swagger.feature` | 1 scenario (`@default`) |
| `QuerySingleClusterAcceptanceTests` | `features/query/singlecluster/Query.feature` | 105 scenario examples |
| `QuerySystemMetadataAcceptanceTests` | `features/query/singlecluster/SystemMetadata.feature` | 13 scenario examples |
| `QueryCollaborationAcceptanceTests` | `features/query/collaboration/QueryCollaboration.feature` | 12 scenario examples (`@xcollab`) |
| `QueryByCursorSingleClusterAcceptanceTests` | `features/querybycursor/singlecluster/QueryByCursor.feature` | scroll cursor |
| `QueryByCursorSearchAfterAcceptanceTests` | same `QueryByCursor.feature` | `search_after=true` via `SearchAfterCursorHooks` |

`Query.feature` has 105 examples vs 101 in the legacy `testing/search-test-core` file (+4 from splitting spatial invalid-longitude checks into `@SpatialLongitudeStandardRange` and `@SpatialLongitudeExtendedRange`).

### Allure reports

Each suite registers `io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm` in its Cucumber plugin
configuration. Raw results are written to `target/allure-results` (from `os-core-test`
`allure.properties` on the classpath).

```bash
cd search-acceptance-test && mvn clean test
ls target/allure-results
```

CI overrides the results directory by passing a Maven system property (forwarded to the test JVM by Surefire):

```bash
mvn test -Dallure.results.directory=aws/allure-results
```

Per-suite Cucumber JUnit XML is written under `target/cucumber-reports/TEST-*.xml`.

### Run tests

```bash
# Export the variables above, or place them in search-acceptance-test/.env
cd search-acceptance-test && mvn clean test
```

Run a single suite:

```bash
cd search-acceptance-test && mvn test -Dtest=QuerySingleClusterAcceptanceTests
```

### Cucumber tags

Default local filter (`src/test/resources/junit-platform.properties`):

```
@default or @health or @SpatialLongitudeStandardRange
```

CI overrides via `-Dcucumber.filter.tags` in pipeline config (Allure is configured in each
suite class, not via CI plugin options):


| Environment | Tags |
|-------------|------|
| Local default | `@default`, `@health`, `@SpatialLongitudeStandardRange` |
| Core-plus (cimpl) | `@default`, `@health`, `@SpatialLongitudeStandardRange` |
| AWS | `@default`, `@health`, `@SpatialLongitudeExtendedRange` |

| Tag | Covered by default? | Notes |
|-----|----------------------|-------|
| `@default` | Yes | Main query, cursor, info, swagger, system metadata scenarios |
| `@health` | Yes | Liveness and readiness checks |
| `@xcollab` | No | Collaboration header scenarios (`QueryCollaboration.feature`); run via `QueryCollaborationAcceptanceTests` with an explicit tag filter |
| `@SpatialLongitudeStandardRange` | Yes (local / cimpl) | Invalid longitude range `[-180, 180]` |
| `@SpatialLongitudeExtendedRange` | Yes (AWS CI only) | Invalid longitude range `[-360, 360]` |
| `@autocomplete` | No | Autocomplete scenario; excluded from simple/default runs |

With the local default filter, `QuerySingleClusterAcceptanceTests` runs **100** examples and skips **5** (`@SpatialLongitudeExtendedRange` × 4, `@autocomplete` × 1). The feature file contains **105** examples in total.

## License

Copyright © Google LLC

Copyright © EPAM Systems

Copyright © ExxonMobil

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
