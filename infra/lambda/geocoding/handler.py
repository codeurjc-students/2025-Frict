"""
Lambda function for geocoding via Nominatim.
Invoked synchronously from the Spring Boot backend.

Input:  {"op": "reverse", "lat": 40.42, "lon": -3.70}
        {"op": "search", "address": "Calle Gran Vía, Madrid"}

Output (reverse): {"address": {"street": "...", "number": "...", "city": "...", "postalCode": "...", "country": "..."}}
Output (search):  {"lat": 40.42, "lon": -3.70}
Output (not found): {"address": null} or {"lat": null, "lon": null}
"""

import json
import urllib.request
import urllib.parse

NOMINATIM_BASE = "https://nominatim.openstreetmap.org"
USER_AGENT = "Frict-Lambda/1.0 (aws-geocoding-proxy)"


def lambda_handler(event, context):
    op = event.get("op")

    if op == "reverse":
        return reverse_geocode(event.get("lat"), event.get("lon"))
    elif op == "search":
        return direct_geocode(event.get("address", ""))
    else:
        return {"errorMessage": f"Unknown operation: {op}"}


def reverse_geocode(lat, lon):
    if lat is None or lon is None:
        return {"address": None}

    params = urllib.parse.urlencode({"format": "json", "lat": lat, "lon": lon})
    url = f"{NOMINATIM_BASE}/reverse?{params}"

    data = _fetch(url)
    if data is None or "address" not in data:
        return {"address": None}

    addr = data["address"]
    return {
        "address": {
            "street": addr.get("road", addr.get("pedestrian", addr.get("street", ""))),
            "number": addr.get("house_number", ""),
            "city": addr.get("city", addr.get("town", addr.get("village", ""))),
            "postalCode": addr.get("postcode", ""),
            "country": addr.get("country", ""),
        }
    }


def direct_geocode(address):
    if not address or not address.strip():
        return {"lat": None, "lon": None}

    params = urllib.parse.urlencode({"format": "json", "q": address, "limit": 1})
    url = f"{NOMINATIM_BASE}/search?{params}"

    data = _fetch(url)
    if data is None or not isinstance(data, list) or len(data) == 0:
        return {"lat": None, "lon": None}

    first = data[0]
    return {"lat": float(first["lat"]), "lon": float(first["lon"])}


def _fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=8) as resp:
            return json.loads(resp.read().decode())
    except Exception:
        return None
