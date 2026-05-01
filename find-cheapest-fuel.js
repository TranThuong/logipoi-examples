/**
 * find-cheapest-fuel.js
 *
 * Find the 5 cheapest DIESEL stations within 20 km of a given GPS position.
 *
 * Usage:
 *   RAPIDAPI_KEY=your_key node find-cheapest-fuel.js [lat] [lon]
 *
 * Examples:
 *   RAPIDAPI_KEY=xxx node find-cheapest-fuel.js 48.8566 2.3522   # Paris
 *   RAPIDAPI_KEY=xxx node find-cheapest-fuel.js 52.5200 13.4050  # Berlin
 *   RAPIDAPI_KEY=xxx node find-cheapest-fuel.js                  # defaults to Paris
 *
 * Requirements: Node.js 18+ (native fetch, no npm install needed)
 */

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

const API_KEY = process.env.RAPIDAPI_KEY;
const API_HOST = "logipoi.p.rapidapi.com";
const BASE_URL = `https://${API_HOST}`;

// Default position: Paris, France
const DEFAULT_LAT = 48.8566;
const DEFAULT_LON = 2.3522;

const RADIUS_KM = 20;
const FUEL_TYPE = "DIESEL";
const TOP_N = 5;

// ---------------------------------------------------------------------------
// Argument parsing
// ---------------------------------------------------------------------------

const args = process.argv.slice(2);
const lat = args[0] ? parseFloat(args[0]) : DEFAULT_LAT;
const lon = args[1] ? parseFloat(args[1]) : DEFAULT_LON;

if (isNaN(lat) || isNaN(lon)) {
  console.error("Error: lat and lon must be valid numbers.");
  console.error("Usage: node find-cheapest-fuel.js [lat] [lon]");
  process.exit(1);
}

if (!API_KEY) {
  console.error("Error: RAPIDAPI_KEY environment variable is not set.");
  console.error("Get your free key at: https://rapidapi.com/logipoi/api/logipoi");
  process.exit(1);
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Compute approximate distance in km between two GPS points using the
 * Haversine formula. Good enough for short distances (< 500 km).
 */
function haversineKm(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

/**
 * Extract the DIESEL price from a gas station's fuelPrices array.
 * Returns null if not found.
 */
function getDieselPrice(station) {
  if (!station.fuelPrices || !Array.isArray(station.fuelPrices)) {
    return null;
  }
  const entry = station.fuelPrices.find(
    (fp) => fp.fuelType === FUEL_TYPE && fp.price != null
  );
  return entry ? entry.price : null;
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  console.log(`Searching for cheapest ${FUEL_TYPE} within ${RADIUS_KM} km of (${lat}, ${lon})...\n`);

  // Build the request URL
  const url = new URL(`${BASE_URL}/v1/gas-stations/nearby`);
  url.searchParams.set("lat", lat);
  url.searchParams.set("lon", lon);
  url.searchParams.set("radius", RADIUS_KM);
  url.searchParams.set("fuelType", FUEL_TYPE);
  url.searchParams.set("size", 100); // fetch a large batch to get the best price spread

  let response;
  try {
    response = await fetch(url.toString(), {
      method: "GET",
      headers: {
        "X-RapidAPI-Key": API_KEY,
        "X-RapidAPI-Host": API_HOST,
      },
    });
  } catch (err) {
    console.error("Network error:", err.message);
    process.exit(1);
  }

  if (!response.ok) {
    const body = await response.text();
    console.error(`API error ${response.status}: ${body}`);
    process.exit(1);
  }

  const data = await response.json();

  // The API returns { content: [...], totalElements: N, ... } (Spring Page)
  const stations = data.content ?? data;

  if (!Array.isArray(stations) || stations.length === 0) {
    console.log("No gas stations found in this area.");
    return;
  }

  // Keep only stations that have a DIESEL price
  const withPrice = stations
    .map((s) => ({ ...s, dieselPrice: getDieselPrice(s) }))
    .filter((s) => s.dieselPrice !== null);

  if (withPrice.length === 0) {
    console.log(`No stations with a known ${FUEL_TYPE} price found.`);
    return;
  }

  // Sort by price ascending and take the top N
  const sorted = withPrice.sort((a, b) => a.dieselPrice - b.dieselPrice);
  const top = sorted.slice(0, TOP_N);

  console.log(`Top ${TOP_N} cheapest ${FUEL_TYPE} stations:\n`);
  console.log(
    `${"#".padEnd(3)} ${"Name".padEnd(35)} ${"Price (€/L)".padEnd(12)} ${"Dist (km)".padEnd(10)} Coordinates`
  );
  console.log("-".repeat(90));

  top.forEach((station, index) => {
    const name = (station.name ?? "Unknown").substring(0, 34).padEnd(35);
    const price = station.dieselPrice.toFixed(3).padEnd(12);
    const sLat = station.location?.lat ?? station.lat;
    const sLon = station.location?.lon ?? station.lon;
    const dist = haversineKm(lat, lon, sLat, sLon).toFixed(1).padEnd(10);
    const coords = `(${sLat?.toFixed(5)}, ${sLon?.toFixed(5)})`;

    console.log(`${String(index + 1).padEnd(3)} ${name} ${price} ${dist} ${coords}`);
  });

  console.log(`\nTotal stations with ${FUEL_TYPE} price in area: ${withPrice.length}`);
}

main();
