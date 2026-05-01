"""
find-truck-parking.py

Find truck parking areas within 50 km of a given GPS position.
Results are filtered to show FENCED (secured) areas first when available.

Usage:
    RAPIDAPI_KEY=your_key python find-truck-parking.py [lat] [lon]

Examples:
    RAPIDAPI_KEY=xxx python find-truck-parking.py 45.7640 4.8357   # Lyon
    RAPIDAPI_KEY=xxx python find-truck-parking.py 48.8566 2.3522   # Paris
    RAPIDAPI_KEY=xxx python find-truck-parking.py                  # defaults to Lyon

Requirements: Python 3.6+ — stdlib only (urllib.request, json, sys, os)
"""

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

API_HOST = "logipoi.p.rapidapi.com"
BASE_URL = f"https://{API_HOST}"

# Default position: Lyon, France
DEFAULT_LAT = 45.7640
DEFAULT_LON = 4.8357

RADIUS_KM = 50
PAGE_SIZE = 50  # results per page

# Security level value that indicates a fenced/secured area
FENCED_SECURITY_LEVEL = "FENCED"

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------

api_key = os.environ.get("RAPIDAPI_KEY")
if not api_key:
    print("Error: RAPIDAPI_KEY environment variable is not set.")
    print("Get your free key at: https://rapidapi.com/logipoi/api/logipoi")
    sys.exit(1)

args = sys.argv[1:]
try:
    lat = float(args[0]) if len(args) > 0 else DEFAULT_LAT
    lon = float(args[1]) if len(args) > 1 else DEFAULT_LON
except ValueError:
    print("Error: lat and lon must be valid numbers.")
    print("Usage: python find-truck-parking.py [lat] [lon]")
    sys.exit(1)

# ---------------------------------------------------------------------------
# API call
# ---------------------------------------------------------------------------

def fetch_truck_parking(lat: float, lon: float, radius: int, api_key: str) -> list:
    """Call the LogiPOI truck-parking endpoint and return the list of results."""
    params = urllib.parse.urlencode({
        "lat": lat,
        "lon": lon,
        "radius": radius,
        "size": PAGE_SIZE,
    })
    url = f"{BASE_URL}/v1/truck-parking/nearby?{params}"

    req = urllib.request.Request(
        url,
        headers={
            "X-RapidAPI-Key": api_key,
            "X-RapidAPI-Host": API_HOST,
        },
    )

    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        print(f"API error {e.code}: {e.read().decode('utf-8')}")
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"Network error: {e.reason}")
        sys.exit(1)

    data = json.loads(body)

    # The API returns a Spring Page object: { content: [...], totalElements: N }
    if isinstance(data, dict) and "content" in data:
        return data["content"]
    if isinstance(data, list):
        return data
    return []


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def format_amenities(amenities) -> str:
    """Return a comma-separated string of amenities, or 'N/A'."""
    if not amenities:
        return "N/A"
    if isinstance(amenities, list):
        return ", ".join(str(a) for a in amenities) if amenities else "N/A"
    return str(amenities)


def get_security_level(area: dict) -> str:
    """Extract the security level string from the area dict."""
    return area.get("securityLevel") or area.get("security_level") or "UNKNOWN"


def get_capacity(area: dict):
    """Extract truck capacity (total spaces) from the area dict."""
    # Try common field names
    for key in ("capacity", "truckCapacity", "truck_capacity", "spaces"):
        val = area.get(key)
        if val is not None:
            return val
    return "N/A"


def get_coords(area: dict):
    """Return (lat, lon) tuple from the area dict."""
    loc = area.get("location", {})
    if loc:
        return loc.get("lat"), loc.get("lon")
    return area.get("lat"), area.get("lon")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    print(f"Searching for truck parking within {RADIUS_KM} km of ({lat}, {lon})...\n")

    areas = fetch_truck_parking(lat, lon, RADIUS_KM, api_key)

    if not areas:
        print("No truck parking areas found in this area.")
        return

    # Partition: fenced first, then the rest
    fenced = [a for a in areas if get_security_level(a) == FENCED_SECURITY_LEVEL]
    other = [a for a in areas if get_security_level(a) != FENCED_SECURITY_LEVEL]

    print(f"Found {len(areas)} truck parking area(s) total.")
    if fenced:
        print(f"  - {len(fenced)} FENCED (secured) area(s)  <-- shown first")
    if other:
        print(f"  - {len(other)} other area(s)\n")
    else:
        print()

    ordered = fenced + other

    # Print results
    for idx, area in enumerate(ordered, start=1):
        name = area.get("name") or "Unnamed"
        security = get_security_level(area)
        capacity = get_capacity(area)
        amenities = format_amenities(area.get("amenities"))
        area_lat, area_lon = get_coords(area)

        secured_badge = " [FENCED]" if security == FENCED_SECURITY_LEVEL else ""

        print(f"{idx}. {name}{secured_badge}")
        print(f"   Security    : {security}")
        print(f"   Capacity    : {capacity} trucks")
        print(f"   Amenities   : {amenities}")
        if area_lat is not None and area_lon is not None:
            print(f"   Coordinates : ({area_lat:.5f}, {area_lon:.5f})")
        print()

    print(f"Tip: add country=FR (or DE, ES, etc.) to narrow results to a single country.")


if __name__ == "__main__":
    main()
