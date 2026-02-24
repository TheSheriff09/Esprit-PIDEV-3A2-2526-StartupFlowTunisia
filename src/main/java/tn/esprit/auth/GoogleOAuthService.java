package tn.esprit.auth;

import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GoogleOAuthService {

    // From your Google JSON:
    private static final String CLIENT_ID =
            "287147228893-56nakqa23vmmek9ccgtgdpqucb67mhel.apps.googleusercontent.com";

    // Put it in ENV (recommended): GOOGLE_OAUTH_CLIENT_SECRET
    private static final String CLIENT_SECRET_ENV = "GOOGLE_OAUTH_CLIENT_SECRET";

    // Endpoints (Google OAuth / OIDC)
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";

    // Basic login scopes
    private static final String SCOPE = "openid email profile";

    public static class GoogleUserInfo {
        private final String email;
        private final String name;
        private final String picture;

        public GoogleUserInfo(String email, String name, String picture) {
            this.email = email;
            this.name = name;
            this.picture = picture;
        }

        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getPicture() { return picture; }
    }

    public GoogleUserInfo loginAndGetUserInfo() throws Exception {

        String clientSecret = System.getenv(CLIENT_SECRET_ENV);
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new IllegalStateException("Missing env var: " + CLIENT_SECRET_ENV);
        }
        System.out.println("CLIENT_ID = " + CLIENT_ID);
        System.out.println("SECRET PRESENT = " + (clientSecret != null));
        System.out.println("SECRET LENGTH = " + (clientSecret == null ? 0 : clientSecret.length()));
        // 1) Start local server on loopback
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String redirectUri = "http://127.0.0.1:" + port + "/callback";

        String expectedState = UUID.randomUUID().toString();
        AtomicReference<String> codeRef = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getRawQuery(); // code=...&state=...
            String code = getQueryParam(query, "code");
            String state = getQueryParam(query, "state");

            String msg;
            if (code == null) {
                msg = "No code received. You can close this tab.";
            } else if (state == null || !expectedState.equals(state)) {
                msg = "State mismatch. You can close this tab.";
            } else {
                codeRef.set(code);
                msg = "Login success. You can close this tab and go back to the app.";
            }

            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            latch.countDown();
        });

        server.start();

        // 2) Open browser to Google consent
        String authUrl = AUTH_ENDPOINT
                + "?client_id=" + enc(CLIENT_ID)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope=" + enc(SCOPE)
                + "&state=" + enc(expectedState)
                + "&access_type=offline"
                + "&prompt=consent";

        if (!Desktop.isDesktopSupported()) {
            server.stop(0);
            throw new IllegalStateException("Desktop browsing not supported on this OS");
        }
        Desktop.getDesktop().browse(URI.create(authUrl));

        // 3) Wait for callback (max 2 minutes)
        boolean ok = latch.await(120, TimeUnit.SECONDS);
        server.stop(0);

        if (!ok || codeRef.get() == null) {
            throw new RuntimeException("Google login timed out or failed.");
        }

        // 4) Exchange code for tokens
        String tokenJson = exchangeCodeForTokens(codeRef.get(), redirectUri, clientSecret);
        String accessToken = JsonTiny.getString(tokenJson, "access_token");
        if (accessToken == null) {
            throw new RuntimeException("No access_token returned. Response: " + tokenJson);
        }

        // 5) Call userinfo endpoint (OIDC)
        String userJson = fetchUserInfo(accessToken);
        String email = JsonTiny.getString(userJson, "email");
        String name = JsonTiny.getString(userJson, "name");
        String picture = JsonTiny.getString(userJson, "picture");

        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Google userinfo missing email. Response: " + userJson);
        }
        if (name == null) name = "";

        return new GoogleUserInfo(email, name, picture);
    }

    private String exchangeCodeForTokens(String code, String redirectUri, String clientSecret) throws Exception {
        String body = "client_id=" + enc(CLIENT_ID)
                + "&client_secret=" + enc(clientSecret)
                + "&code=" + enc(code)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + enc(redirectUri);

        HttpRequest req = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    private String fetchUserInfo(String accessToken) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(USERINFO_ENDPOINT))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String getQueryParam(String rawQuery, String key) {
        if (rawQuery == null) return null;

        String[] parts = rawQuery.split("&");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String k = URLDecoder.decode(p.substring(0, eq), StandardCharsets.UTF_8);
            if (!key.equals(k)) continue;
            return URLDecoder.decode(p.substring(eq + 1), StandardCharsets.UTF_8);
        }
        return null;
    }
}