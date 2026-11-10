package com.richardgrim.erp;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

/** Cliente HTTP real con cookies y CSRF, exclusivo de las pruebas. */
class TestSession {
    final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
    final int port;
    String csrf;
    TestSession(int port) { this.port = port; }
    HttpRequest request(String method, String path, String body) {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/" + path))
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(body));
        if(csrf != null) builder.header("X-CSRF-TOKEN", csrf);
        return builder.build();
    }
    HttpResponse<String> send(String method, String path, String body) throws Exception {
        return client.send(request(method, path, body), HttpResponse.BodyHandlers.ofString());
    }
    void refreshCsrf() throws Exception {
        var response = send("GET", "auth/csrf", "");
        assertThat(response.statusCode()).isEqualTo(200);
        csrf = JsonMapper.builder().build().readTree(response.body()).get("token").asString();
    }
    HttpResponse<String> login(String username, String password) throws Exception {
        refreshCsrf();
        String form = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
            + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
            .header("Content-Type", "application/x-www-form-urlencoded").header("X-CSRF-TOKEN", csrf)
            .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        refreshCsrf();
        return response;
    }
    static TestSession admin(int port) throws Exception {
        var session = new TestSession(port);
        assertThat(session.login("admin", "Test-admin-only-2026!").statusCode()).isEqualTo(204);
        return session;
    }
}
