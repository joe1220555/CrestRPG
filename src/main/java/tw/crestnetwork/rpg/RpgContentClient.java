package tw.crestnetwork.rpg;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

final class RpgContentClient {
    private final Gson gson = new Gson();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiBase;
    private final String token;
    private final String serverKey;

    RpgContentClient(String apiBase, String token, String serverKey) {
        this.apiBase = apiBase.replaceAll("/+$", "");
        this.token = token;
        this.serverKey = serverKey;
    }

    RpgManifest fetchManifest() throws Exception {
        String query = URLEncoder.encode(serverKey, StandardCharsets.UTF_8);
        HttpRequest request = request(apiBase + "/rpg/manifest?server_key=" + query).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("Manifest HTTP " + response.statusCode() + ": " + response.body());
        return RpgManifestParser.parse(response.body());
    }

    void acknowledge(RpgManifest manifest, boolean success, String error) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("server_key", serverKey);
        body.addProperty("manifest_checksum", manifest.checksum());
        if (manifest.lastRevisionId() > 0) body.addProperty("last_revision_id", manifest.lastRevisionId());
        body.addProperty("success", success);
        if (error != null && !error.isBlank()) body.addProperty("error_message", error.substring(0, Math.min(2000, error.length())));

        HttpRequest request = request(apiBase + "/rpg/acknowledge")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("Acknowledge HTTP " + response.statusCode() + ": " + response.body());
    }

    private HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token);
    }
}
