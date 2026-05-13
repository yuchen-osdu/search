## Prerequisites

- `uv`: [installation](https://docs.astral.sh/uv/getting-started/installation/)
- `Schemathesis`: [docs](https://schemathesis.readthedocs.io/) and [configuration reference](https://schemathesis.readthedocs.io/en/stable/reference/configuration/)
- `Allure`: [docs](https://allurereport.org/docs/) and [installation](https://allurereport.org/docs/v2/install/)
- Python `3.12.12`: [release page](https://www.python.org/downloads/release/python-31212/) (pinned via `.python-version`)

### Environment variables

- `KEYCLOAK_ENDPOINT`
- `CLIENT_ID`
- `CLIENT_SECRET`
- `KEYCLOAK_SCOPE` (Optional). Default value: `openid`
- `BEARER_TOKEN` (Optional). keycloak_auth module will be used for token retrieval if not set.
- `SCHEMATHESIS_VERSION` (Optional). Default value: `4.17.0`

### Entitlements configuration for integration accounts

#### Users

- `service.entitlements.user`
- `service.search.admin`
- `data.default.viewers`
- `data.default.owners`

## Usage

`run_schemathesis.py` executes Schemathesis via `uvx` using `schemathesis.toml`. Pass schema location as the first argument. `--base-url` is optional and is useful when the schema location and target API URL are different. Additional Schemathesis CLI arguments can be passed through `run_schemathesis.py`.

```bash
export KEYCLOAK_ENDPOINT="https://keycloak.example.com/realms/osdu/protocol/openid-connect/token"
export CLIENT_ID="client-id"
export CLIENT_SECRET="client-secret"
uv run run_schemathesis.py "https://api.example.com/openapi.json"
```

```bash
uv run run_schemathesis.py "schema.json" --base-url "https://api.example.com"
```

```bash
uv run run_schemathesis.py "https://api.example.com/openapi.json" --max-examples 10 --include-method GET
```

CLI arguments kept in `run_schemathesis.py`:

- `schema_location`
- `--base-url`
- `--include-method`
- `--include-operation-id`
- `--include-path`

## `schemathesis.toml`

- `run_schemathesis.py` always loads `schemathesis.toml` via `--config-file schemathesis.toml`.
- Current parameters in this repo:
  - `seed`: fixed seed for reproducible runs
  - `workers`: number of parallel workers
  - `continue-on-failure`: continue after failed checks
  - `request-timeout`: timeout for one request in seconds
  - `[generation].mode`: test data generation mode
  - `[phases.stateful].enabled`: enable stateful phase
  - `[reports.junit].path`: JUnit XML output file
  - `[reports.allure].path`: Allure results directory
- Full parameters list: [Schemathesis configuration reference](https://schemathesis.readthedocs.io/en/stable/reference/configuration/)

## Reports

- JUnit: `output/junit/junit.xml`
- Allure results: `output/allure/results`
- JUnit is useful for CI result publishing and machine-readable test output.
- Allure results can be rendered locally into an HTML report.

```bash
allure generate --single-file output/allure/results -o output/allure/report --clean
```

Generated files are stored under `output/`.

## Debug

- `--report ndjson` enables NDJSON event reporting for Schemathesis run events.
- `--report-ndjson-path output/events.ndjson` writes those events to `output/events.ndjson`.
- NDJSON is useful when you need raw details for executed, skipped, passed, or failed scenarios.
- It is also useful when reviewing skip reasons together with the custom CLI reporter output.

```bash
uv run run_schemathesis.py "schema.json" --base-url "https://api.example.com" --report ndjson --report-ndjson-path output/events.ndjson
```