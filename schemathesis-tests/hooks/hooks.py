"""Schemathesis hooks for the search-service API-contract gate.

Registered via the ``hooks="./hooks/hooks.py"`` key in ``schemathesis.toml``; the
``@schemathesis.hook`` decorators below take effect on import.
"""

import schemathesis


@schemathesis.hook
def map_case(context, case):
    """Drop ``spatialFilter`` from generated request bodies.

    The ``SpatialFilter`` model (os-core-common) declares ``field`` plus five optional
    criteria — ``byBoundingBox``, ``byDistance``, ``byGeoPolygon``, ``byIntersection``,
    ``byWithinPolygon`` — as independent optional properties, but the server enforces (via
    ``SpatialFilterValidator``) that only one criterion may be set. The generated OpenAPI
    schema does not express that mutual exclusivity, so the data generator produces bodies
    with several criteria at once and the service correctly rejects them with HTTP 400,
    tripping the ``positive_data_acceptance`` check.

    ``spatialFilter`` is optional, so removing it keeps every generated body schema-valid and
    server-acceptable. This is a stopgap that excludes spatial filtering from contract
    coverage. The real fix is to model the exclusivity in the schema upstream (a ``oneOf`` on
    ``SpatialFilter`` in os-core-common, mirrored by the validator); once that lands, this hook
    should be removed.
    """
    body = getattr(case, "body", None)
    if isinstance(body, dict):
        body.pop("spatialFilter", None)
    return case
