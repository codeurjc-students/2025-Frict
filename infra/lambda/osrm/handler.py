"""
Lambda function for OSRM routing proxy.
Invoked synchronously from the Spring Boot backend (prod profile only).

Input:  {"fromLat": 40.42, "fromLng": -3.70, "toLat": 40.50, "toLng": -3.65}
Output: {"durationSeconds": 1234.5, "distanceMeters": 5678.9, "coordinates": [[lng, lat], ...]}
        {"error": "Route not found"}  — when OSRM returns no routes
"""

import json
import urllib.request

OSRM_BASE = "https://router.project-osrm.org"


def lambda_handler(event, context):
    from_lat = event.get("fromLat")
    from_lng = event.get("fromLng")
    to_lat   = event.get("toLat")
    to_lng   = event.get("toLng")

    if None in (from_lat, from_lng, to_lat, to_lng):
        return {"error": "Missing coordinates"}

    url = (
        f"{OSRM_BASE}/route/v1/driving/"
        f"{from_lng},{from_lat};{to_lng},{to_lat}"
        f"?overview=full&geometries=geojson"
    )

    data = _fetch(url)
    if data is None or not data.get("routes"):
        return {"error": "Route not found"}

    route = data["routes"][0]
    coords = [
        [pt[0], pt[1]]
        for pt in route.get("geometry", {}).get("coordinates", [])
        if len(pt) >= 2
    ]

    return {
        "durationSeconds": route.get("duration", 0),
        "distanceMeters":  route.get("distance", 0),
        "coordinates":     coords,
    }


def _fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Frict-Lambda/1.0 (aws-osrm-proxy)"})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode())
    except Exception:
        return None
