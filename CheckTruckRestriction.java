/**
 * CheckTruckRestriction.java
 *
 * Check active truck weight/height/type restrictions near a given city.
 * Default example: Munich, Germany (lat=48.1351, lon=11.5820).
 *
 * Usage (Java 11+ single-file launch — no compile step):
 *   RAPIDAPI_KEY=your_key java CheckTruckRestriction.java
 *
 * Requirements:
 *   - Java 11+ (uses java.net.http.HttpClient and java.net.http.HttpRequest)
 *   - No external dependencies
 *
 * Get your free API key at: https://rapidapi.com/logipoi/api/logipoi
 */

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class CheckTruckRestriction {

    // ---------------------------------------------------------------------------
    // API configuration
    // ---------------------------------------------------------------------------

    private static final String API_HOST = "logipoi.p.rapidapi.com";
    private static final String BASE_URL  = "https://" + API_HOST;

    // ---------------------------------------------------------------------------
    // Search parameters — Munich city centre
    // ---------------------------------------------------------------------------

    private static final double LAT       = 48.1351;
    private static final double LON       = 11.5820;
    private static final int    RADIUS_KM = 10;
    private static final int    PAGE_SIZE = 20;

    // ---------------------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {

        // Read the API key from the environment
        String apiKey = System.getenv("RAPIDAPI_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Error: RAPIDAPI_KEY environment variable is not set.");
            System.err.println("Get your free key at: https://rapidapi.com/logipoi/api/logipoi");
            System.exit(1);
        }

        System.out.printf("Checking truck restrictions within %d km of Munich (%.4f, %.4f)...%n%n",
                RADIUS_KM, LAT, LON);

        // Build the request URL with query parameters
        String queryString = "lat=" + LAT
                + "&lon=" + LON
                + "&radius=" + RADIUS_KM
                + "&size=" + PAGE_SIZE;

        URI uri = URI.create(BASE_URL + "/v1/truck-restrictions/nearby?" + queryString);

        // Create an HTTP client with a reasonable timeout
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(15))
                .header("X-RapidAPI-Key", apiKey)
                .header("X-RapidAPI-Host", API_HOST)
                .GET()
                .build();

        // Send the request and get the response body as a String
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.printf("API error %d: %s%n", response.statusCode(), response.body());
            System.exit(1);
        }

        // Parse and display restrictions using plain String manipulation.
        // We avoid external JSON libraries to keep this example dependency-free.
        String body = response.body();
        parseAndPrintRestrictions(body);
    }

    // ---------------------------------------------------------------------------
    // Minimal JSON parsing helpers (no external dependencies)
    // ---------------------------------------------------------------------------

    /**
     * Parse the API response body and print each restriction found.
     *
     * The response is a Spring Page object:
     *   { "content": [ {...}, {...} ], "totalElements": N, ... }
     *
     * We use simple String extraction — sufficient for flat JSON fields.
     * For production code, use Jackson or Gson instead.
     */
    private static void parseAndPrintRestrictions(String json) {
        // Extract the content array
        int contentStart = json.indexOf("\"content\"");
        if (contentStart == -1) {
            System.out.println("Unexpected response format — 'content' key not found.");
            System.out.println("Raw response: " + json.substring(0, Math.min(500, json.length())));
            return;
        }

        // Find the opening bracket of the content array
        int arrayStart = json.indexOf('[', contentStart);
        int arrayEnd   = findMatchingBracket(json, arrayStart);

        if (arrayStart == -1 || arrayEnd == -1) {
            System.out.println("Could not parse content array.");
            return;
        }

        String contentArray = json.substring(arrayStart + 1, arrayEnd).trim();

        if (contentArray.isEmpty()) {
            System.out.println("No truck restrictions found in this area.");
            return;
        }

        // Split on top-level objects (each starts with '{')
        java.util.List<String> objects = splitTopLevelObjects(contentArray);

        System.out.printf("Found %d restriction(s):%n%n", objects.size());

        int index = 1;
        for (String obj : objects) {
            System.out.printf("%d. Restriction%n", index++);
            System.out.println("   Type          : " + extractField(obj, "restrictionType"));
            System.out.println("   Max weight    : " + extractField(obj, "maxWeightTon") + " t");
            System.out.println("   Max height    : " + extractField(obj, "maxHeightM") + " m");
            System.out.println("   Max length    : " + extractField(obj, "maxLengthM") + " m");
            System.out.println("   Road/location : " + extractField(obj, "roadName"));
            System.out.println("   Country       : " + extractField(obj, "country"));
            System.out.println("   Start time    : " + extractField(obj, "startTime"));
            System.out.println("   End time      : " + extractField(obj, "endTime"));
            System.out.println();
        }

        // Extract total element count from outer wrapper
        String totalElements = extractField(json, "totalElements");
        if (!totalElements.equals("N/A")) {
            System.out.println("Total restrictions in area (all pages): " + totalElements);
        }
    }

    /**
     * Extract the string or numeric value of a JSON field by key.
     * Returns "N/A" when the key is not found.
     *
     * Supports both quoted strings ("key":"value") and raw values ("key":123 / null).
     */
    private static String extractField(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx == -1) {
            return "N/A";
        }

        // Move past the key and the colon
        int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx == -1) {
            return "N/A";
        }

        // Skip whitespace after colon
        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return "N/A";
        }

        char firstChar = json.charAt(valueStart);

        if (firstChar == '"') {
            // Quoted string — find the closing quote (handle escaped quotes)
            int end = valueStart + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') {
                    break;
                }
                end++;
            }
            return json.substring(valueStart + 1, end);
        }

        if (firstChar == 'n') {
            // null literal
            return "N/A";
        }

        // Numeric or boolean — read until delimiter
        int end = valueStart;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                break;
            }
            end++;
        }
        return json.substring(valueStart, end);
    }

    /**
     * Find the index of the closing bracket ']' that matches the opening bracket
     * at position {@code openIdx}, respecting nested objects and arrays.
     */
    private static int findMatchingBracket(String json, int openIdx) {
        if (openIdx == -1 || openIdx >= json.length()) {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        for (int i = openIdx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '[' || c == '{') {
                    depth++;
                } else if (c == ']' || c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Split a JSON array body (content between the outer '[' and ']') into
     * a list of individual top-level object strings.
     */
    private static java.util.List<String> splitTopLevelObjects(String arrayContent) {
        java.util.List<String> result = new java.util.ArrayList<>();
        int i = 0;
        while (i < arrayContent.length()) {
            // Skip whitespace and commas between objects
            char c = arrayContent.charAt(i);
            if (c == '{') {
                int end = findMatchingBracket(arrayContent, i);
                if (end == -1) {
                    break;
                }
                result.add(arrayContent.substring(i, end + 1));
                i = end + 1;
            } else {
                i++;
            }
        }
        return result;
    }
}
