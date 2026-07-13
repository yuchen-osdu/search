"""Schemathesis hooks for the search-service API-contract gate.

``blend_query_kind_and_spatial_bodies`` blends known-good ``kind`` / ``spatialFilter`` values
into POST ``/query*`` generation (avoids ``filter_too_much`` on 4.22.4).

``normalize_query_kind_and_cursor_case`` dedupes ``kind`` lists and drops body ``cursor`` on
``POST /query_with_cursor`` (first-page only).

``data-partition-id`` is pinned via ``[headers]`` in ``schemathesis.toml``.

Loaded via ``hooks = "./hooks/hooks.py"`` in ``schemathesis.toml``.
"""

from __future__ import annotations

import hypothesis.strategies as st
import schemathesis

POST_QUERY_PATHS = frozenset({"/query", "/query_with_cursor"})

KIND_VALUES = ("osdu:osdu:wellbore:1.0.0", "opendes:osdu:wellbore:1.0.0")
KIND_STRATEGY = st.one_of(
    st.sampled_from(KIND_VALUES),
    st.lists(st.sampled_from(KIND_VALUES), min_size=1, max_size=2, unique=True),
)

# One criterion per filter, aligned with SpatialFilterValidator.
SPATIAL_FILTER_SAMPLES = (
    {
        "field": "data.Location",
        "byBoundingBox": {
            "topLeft": {"latitude": 45.0, "longitude": -100.0},
            "bottomRight": {"latitude": 40.0, "longitude": -95.0},
        },
    },
    {
        "field": "data.Location",
        "byDistance": {
            "distance": 1500.0,
            "point": {"latitude": 30.0, "longitude": -95.0},
        },
    },
    {
        "field": "data.Location",
        "byGeoPolygon": {
            "points": [
                {"latitude": 30.0, "longitude": -95.0},
                {"latitude": 31.0, "longitude": -94.0},
                {"latitude": 32.0, "longitude": -93.0},
            ],
        },
    },
)

PREDEFINED_BODIES = st.one_of(
    st.fixed_dictionaries({"kind": KIND_STRATEGY}),
    st.fixed_dictionaries(
        {
            "kind": KIND_STRATEGY,
            "spatialFilter": st.sampled_from(SPATIAL_FILTER_SAMPLES),
        }
    ),
)


@schemathesis.hook("before_generate_body")
def blend_query_kind_and_spatial_bodies(context, strategy):
    operation = context.operation
    if (
        operation is not None
        and operation.method.upper() == "POST"
        and operation.path in POST_QUERY_PATHS
    ):
        return st.one_of(strategy, PREDEFINED_BODIES)
    return strategy


@schemathesis.hook("map_case")
def normalize_query_kind_and_cursor_case(context, case):
    body = case.body
    if not isinstance(body, dict):
        return case

    kind = body.get("kind")
    if isinstance(kind, list):
        deduped = list(dict.fromkeys(kind))
        body["kind"] = deduped[0] if len(deduped) == 1 else deduped

    if case.method.upper() == "POST" and case.path == "/query_with_cursor":
        body.pop("cursor", None)

    return case
