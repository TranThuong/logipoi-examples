# LogiPOI Examples

```
 _               _  ____   ___  ___ 
| |    ___  __ _(_)|  _ \ / _ \|_ _|
| |   / _ \/ _` | || |_) | | | || | 
| |__| (_) | (_| | ||  __/| |_| || | 
|_____\___/ \__, |_||_|    \___/|___|
            |___/                    
```

**Logistics POI Intelligence API for Europe**

Real-time gas stations, truck parking, and truck restrictions — sourced from OpenStreetMap + official government data (Prix Carburants FR, Tankerkoenig DE, MITECO ES).

> **Try the live demo →** [app.logipoi.com/app](https://app.logipoi.com/app) — interactive map of every POI returned by the API
>
> **Get your free API key →** [app.logipoi.com](https://app.logipoi.com) (direct signup) or [RapidAPI → LogiPOI](https://rapidapi.com/tranthuongtien/api/logipoi)

---

## Examples

| File | Language | What it does |
|------|----------|--------------|
| [`find-cheapest-fuel.js`](./find-cheapest-fuel.js) | Node.js 18+ | Find the cheapest DIESEL stations within 20 km of any GPS point |
| [`find-truck-parking.py`](./find-truck-parking.py) | Python 3 | Find secured truck parking areas within 50 km, filtered by security level |
| [`CheckTruckRestriction.java`](./CheckTruckRestriction.java) | Java 11+ | Check active truck restrictions near a city (Munich example) |

---

## Prerequisites

### Node.js (find-cheapest-fuel.js)

- Node.js 18 or higher (uses native `fetch`)
- No `npm install` required

```bash
RAPIDAPI_KEY=your_key_here node find-cheapest-fuel.js 48.8566 2.3522
```

### Python (find-truck-parking.py)

- Python 3.6+
- No pip install required (stdlib only: `urllib.request`, `json`, `sys`, `os`)

```bash
RAPIDAPI_KEY=your_key_here python find-truck-parking.py 45.7640 4.8357
```

### Java (CheckTruckRestriction.java)

- Java 11+ (uses `java.net.http.HttpClient` — no external dependencies)
- Single-file launch (Java 11+): no compile step needed

```bash
RAPIDAPI_KEY=your_key_here java CheckTruckRestriction.java
```

---

## API Reference

**Base URL**: `https://logipoi.p.rapidapi.com`

| Endpoint | Description |
|----------|-------------|
| `GET /v1/gas-stations/nearby` | Gas stations with live fuel prices |
| `GET /v1/truck-parking/nearby` | Truck parking areas with amenities |
| `GET /v1/truck-restrictions/nearby` | Truck weight/height/type restrictions |

**Common query parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| `lat` | float | Latitude (WGS 84) |
| `lon` | float | Longitude (WGS 84) |
| `radius` | int | Search radius in kilometers |
| `country` | string | ISO 3166-1 alpha-2 country code (e.g. `FR`, `DE`, `ES`) |
| `page` | int | Page index (0-based) |
| `size` | int | Results per page |

**Fuel types** (`fuelType` param for gas stations): `DIESEL`, `E10`, `SP95`, `SP98`, `ADBLUE`, `GPL`

**Required headers**:
```
X-RapidAPI-Key: YOUR_KEY
X-RapidAPI-Host: logipoi.p.rapidapi.com
```

### Direct API access (no RapidAPI account)

If you sign up for an API key directly at [app.logipoi.com](https://app.logipoi.com), point the examples at the direct base URL and swap the header:

**Base URL**: `https://api.logipoi.com`

**Required header**:
```
X-Api-Key: YOUR_KEY
```

The endpoints, parameters, and responses are identical — only the host and the auth header differ.

---

## Data Sources

LogiPOI aggregates and normalises data from:

- **[OpenStreetMap](https://www.openstreetmap.org/)** — pan-European POI coverage
- **[Prix Carburants](https://www.prix-carburants.gouv.fr/)** — official French government fuel price feed
- **[Tankerkoenig](https://creativecommons.tankerkoenig.de/)** — official German fuel price open data
- **[MITECO](https://www.miteco.gob.es/)** — Spanish Ministry of Ecological Transition fuel station registry

Data is refreshed daily. Prices are updated in near-real-time where the source allows.

---

## Get a free API key

Two paths — both yield a working FREE-tier key (no credit card required):

**Option A — Direct signup at [app.logipoi.com](https://app.logipoi.com)**
1. Open the landing page and click **Get API Key**
2. Fill in name + email and pick the **FREE** tier
3. Receive your key by email — use header `X-Api-Key: YOUR_KEY` against `https://api.logipoi.com`

**Option B — RapidAPI marketplace**
1. Go to [RapidAPI → LogiPOI](https://rapidapi.com/tranthuongtien/api/logipoi)
2. Click **Subscribe to Test**
3. Choose the **Free** plan
4. Copy your `X-RapidAPI-Key` from the dashboard — use it with the example scripts above

---

## License

Examples are released under the MIT License. See [LICENSE](./LICENSE).
