"""Schemathesis hooks for the search-service API-contract gate.

``blend_query_kind_and_spatial_bodies`` blends known-good ``kind`` / ``spatialFilter`` /
aligned ``sort`` values into POST ``/query*`` generation (avoids ``filter_too_much`` on 4.22.4).

``normalize_query_kind_and_cursor_case`` drops generated values that OpenAPI cannot express:
first-page ``cursor``, empty/multi-criteria spatial filters, and sort fields that Elasticsearch
rejects (for example ``"0"``). Paths are matched by suffix so a server prefix such as
``/api/search/v2`` does not skip the hook.

Loaded via ``hooks = "./hooks/hooks.py"`` in ``schemathesis.toml``.
"""

from __future__ import annotations

import hypothesis.strategies as st
import schemathesis

SPATIAL_CRITERIA = (
    "byBoundingBox",
    "byDistance",
    "byGeoPolygon",
    "byIntersection",
    "byWithinPolygon",
)

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

# field and order must be the same length (SortOrderValidator); max 16 is not enforced at runtime.
SORT_SAMPLES = (
    {"field": ["id"], "order": ["ASC"]},
    {"field": ["id", "kind"], "order": ["ASC", "DESC"]},
)

PREDEFINED_BODIES = st.one_of(
    st.fixed_dictionaries({"kind": KIND_STRATEGY}),
    st.fixed_dictionaries(
        {
            "kind": KIND_STRATEGY,
            "spatialFilter": st.sampled_from(SPATIAL_FILTER_SAMPLES),
        }
    ),
    st.fixed_dictionaries(
        {
            "kind": KIND_STRATEGY,
            "sort": st.sampled_from(SORT_SAMPLES),
        }
    ),
)


def _normalized_path(path: str | None) -> str:
    if not path:
        return ""
    return path.split("?", 1)[0].rstrip("/")


def _is_post_query(method: str, path: str | None) -> bool:
    return method.upper() == "POST" and _normalized_path(path).endswith("/query")


def _is_post_query_with_cursor(method: str, path: str | None) -> bool:
    return method.upper() == "POST" and _normalized_path(path).endswith("/query_with_cursor")


def _is_post_query_op(method: str, path: str | None) -> bool:
    return _is_post_query(method, path) or _is_post_query_with_cursor(method, path)


def _looks_like_index_field(value: object) -> bool:
    return isinstance(value, str) and any(ch.isalpha() or ch == "_" for ch in value)


def _points_ok(points: object, minimum: int) -> bool:
    return isinstance(points, list) and len(points) >= minimum and all(isinstance(p, dict) for p in points)


def _spatial_filter_ok(spatial_filter: object) -> bool:
    if not isinstance(spatial_filter, dict):
        return False
    field = spatial_filter.get("field")
    if not _looks_like_index_field(field):
        return False

    present = [key for key in SPATIAL_CRITERIA if spatial_filter.get(key) is not None]
    if len(present) != 1:
        return False

    criterion = present[0]
    value = spatial_filter[criterion]
    if not isinstance(value, dict):
        return False

    if criterion == "byBoundingBox":
        top_left = value.get("topLeft")
        bottom_right = value.get("bottomRight")
        if not isinstance(top_left, dict) or not isinstance(bottom_right, dict):
            return False
        try:
            top = float(top_left["latitude"])
            left = float(top_left["longitude"])
            bottom = float(bottom_right["latitude"])
            right = float(bottom_right["longitude"])
        except (KeyError, TypeError, ValueError):
            return False
        return top > bottom and left < right

    if criterion == "byDistance":
        try:
            return float(value.get("distance")) > 0 and isinstance(value.get("point"), dict)
        except (TypeError, ValueError):
            return False

    if criterion in {"byGeoPolygon", "byWithinPolygon"}:
        return _points_ok(value.get("points"), 3)

    if criterion == "byIntersection":
        polygons = value.get("polygons")
        if not isinstance(polygons, list) or not polygons:
            return False
        return all(isinstance(polygon, dict) and _points_ok(polygon.get("points"), 3) for polygon in polygons)

    return False


def _align_sort_field_and_order(body: dict) -> None:
    sort = body.get("sort")
    if not isinstance(sort, dict):
        return

    field = sort.get("field")
    order = sort.get("order")
    if not isinstance(field, list) or not isinstance(order, list):
        body.pop("sort", None)
        return

    size = min(len(field), len(order))
    if size == 0:
        body.pop("sort", None)
        return

    sort["field"] = field[:size]
    sort["order"] = order[:size]
    filt = sort.get("filter")
    if isinstance(filt, list):
        sort["filter"] = filt[:size]

    if not all(_looks_like_index_field(name) for name in sort["field"]):
        body.pop("sort", None)


def _sanitize_query_body(body: dict, method: str, path: str | None) -> None:
    kind = body.get("kind")
    if isinstance(kind, list):
        deduped = list(dict.fromkeys(kind))
        body["kind"] = deduped[0] if len(deduped) == 1 else deduped

    if _is_post_query_with_cursor(method, path):
        body.pop("cursor", None)

    if "spatialFilter" in body and not _spatial_filter_ok(body.get("spatialFilter")):
        body.pop("spatialFilter", None)

    _align_sort_field_and_order(body)


@schemathesis.hook("before_generate_body")
def blend_query_kind_and_spatial_bodies(context, strategy):
    operation = context.operation
    if operation is not None and _is_post_query_op(operation.method, operation.path):
        return st.one_of(strategy, PREDEFINED_BODIES)
    return strategy


@schemathesis.hook("map_case")
def normalize_query_kind_and_cursor_case(context, case):
    body = case.body
    if not isinstance(body, dict):
        return case
    if not _is_post_query_op(case.method, case.path):
        return case

    _sanitize_query_body(body, case.method, case.path)
    return case
