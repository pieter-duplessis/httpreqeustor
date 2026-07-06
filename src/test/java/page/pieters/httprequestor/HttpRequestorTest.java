package page.pieters.httprequestor;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests spin up a local {@link HttpServer} (built into the JDK, no extra
 * test dependency required) and drive real requests through it, the same way
 * OkHttp's MockWebServer or Unirest's test suite exercise their clients
 * end-to-end rather than mocking the transport.
 */
class HttpRequestorTest {

    private static HttpServer server;
    private static String baseUrl;

    private static volatile String lastMethod;
    private static volatile String lastPath;
    private static volatile String lastQuery;
    private static volatile Headers lastHeaders;
    private static volatile String lastRequestBody;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/echo", HttpRequestorTest::handleEcho);
        server.createContext("/status/404", exchange -> respond(exchange, 404, "{\"error\":\"not found\"}"));
        server.createContext("/status/500", exchange -> respond(exchange, 500, "{\"error\":\"server error\"}"));
        server.createContext("/no-content", exchange -> respond(exchange, 204, null));
        server.createContext("/slow", HttpRequestorTest::handleSlow);

        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @AfterEach
    void resetCapturedRequest() {
        lastMethod = null;
        lastPath = null;
        lastQuery = null;
        lastHeaders = null;
        lastRequestBody = null;
    }

    private static void handleEcho(HttpExchange exchange) throws IOException {
        captureRequest(exchange);
        respond(exchange, 200, "{\"message\":\"ok\"}");
    }

    private static void handleSlow(HttpExchange exchange) throws IOException {
        captureRequest(exchange);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        respond(exchange, 200, "{\"message\":\"slow\"}");
    }

    private static void captureRequest(HttpExchange exchange) throws IOException {
        lastMethod = exchange.getRequestMethod();
        lastPath = exchange.getRequestURI().getPath();
        lastQuery = exchange.getRequestURI().getQuery();
        lastHeaders = exchange.getRequestHeaders();
        lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        if (jsonBody == null) {
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
            return;
        }
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Nested
    class GetRequests {

        @Test
        void returnsStatusCodeAndParsedJsonBody() {
            Response<HashMap<String, Object>> response = new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .get();

            assertNotNull(response);
            assertEquals(200, response.statusCode);
            assertEquals("ok", response.body.get("message"));
            assertEquals("GET", lastMethod);
            assertEquals("/echo", lastPath);
        }

        @Test
        void sendsSingleQueryParameter() {
            new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .addParam("foo", "bar")
                    .get();

            assertEquals("foo=bar", lastQuery);
        }

        @Test
        void sendsMultipleQueryParametersJoinedWithAmpersand() {
            HashMap<String, String> params = new HashMap<>();
            params.put("a", "1");
            params.put("b", "2");
            params.put("c", "3");

            new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .addParams(params)
                    .get();

            assertNotNull(lastQuery);
            for (String pair : new String[] {"a=1", "b=2", "c=3"}) {
                assertTrue(lastQuery.contains(pair), "Expected query '" + lastQuery + "' to contain " + pair);
            }
            assertEquals(2, lastQuery.chars().filter(c -> c == '&').count(),
                    "Expected exactly two '&' separators joining three params, got: " + lastQuery);
        }

        @Test
        void exposesResponseHeaders() {
            Response<HashMap<String, Object>> response = new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .get();

            assertNotNull(response.headers);
            assertTrue(response.headers.containsKey("Content-Type"));
        }
    }

    @Nested
    class RequestHeaders {

        @Test
        void customRequestHeaderIsSentToServer() {
            new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .addRequestHeader("X-Custom-Header", "custom-value")
                    .get();

            assertNotNull(lastHeaders);
            assertTrue(lastHeaders.containsKey("X-Custom-Header"),
                    "Expected server to receive X-Custom-Header, got headers: " + lastHeaders);
            assertEquals("custom-value", lastHeaders.getFirst("X-Custom-Header"));
        }

        @Test
        void queryParametersAreNotAlsoSentAsHeaders() {
            new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .addParam("foo", "bar")
                    .get();

            assertNotNull(lastHeaders);
            assertNull(lastHeaders.getFirst("foo"),
                    "Query parameters should not leak onto the wire as request headers");
        }
    }

    @Nested
    class PostPutDeleteRequests {

        @Test
        void postSendsConfiguredJsonBody() {
            HashMap<String, Object> body = new HashMap<>();
            body.put("username", "admin");

            Response<HashMap<String, Object>> response = new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .setRequestBody(body)
                    .post();

            assertEquals("POST", lastMethod);
            assertNotNull(response);
            assertEquals(200, response.statusCode);
            assertTrue(lastRequestBody.contains("admin"),
                    "Expected request body to contain the configured payload, got: " + lastRequestBody);
        }

        @Test
        void putSendsConfiguredJsonBody() {
            HashMap<String, Object> body = new HashMap<>();
            body.put("username", "admin");

            new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .setRequestBody(body)
                    .put();

            assertEquals("PUT", lastMethod);
            assertTrue(lastRequestBody.contains("admin"),
                    "Expected request body to contain the configured payload, got: " + lastRequestBody);
        }

        @Test
        void deleteSendsNoBody() {
            Response<HashMap<String, Object>> response = new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .delete();

            assertEquals("DELETE", lastMethod);
            assertNotNull(response);
            assertEquals(200, response.statusCode);
        }
    }

    @Nested
    class RequestMethodDispatch {

        @Test
        void dispatchesGetByName() {
            new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .setRequestMethod("GET");

            assertEquals("GET", lastMethod);
        }

        @Test
        void dispatchesPostByName() {
            new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .setRequestMethod("POST");

            assertEquals("POST", lastMethod);
        }

        @Test
        void unsupportedMethodFallsBackToGet() {
            new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/echo")
                    .setRequestMethod("PATCH");

            assertEquals("GET", lastMethod);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void surfacesNonSuccessStatusCode() {
            Response<HashMap<String, Object>> response = new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/status/404")
                    .get();

            assertNotNull(response);
            assertEquals(404, response.statusCode);
        }

        @Test
        void surfacesServerErrorStatusCode() {
            Response<HashMap<String, Object>> response = new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/status/500")
                    .get();

            assertNotNull(response);
            assertEquals(500, response.statusCode);
        }

        @Test
        void connectionFailureIsHandledGracefully() {
            // Nothing listens on this port, so the connection is refused.
            Response<HashMap<String, Object>> response = new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl("http://localhost:1")
                    .setPath("/echo")
                    .setRequestTimeout(2)
                    .get();

            assertNull(response, "A connection failure is swallowed internally and surfaces as a null response");
        }
    }

    @Nested
    class Timeouts {

        @Test
        void requestExceedingTimeoutReturnsNull() {
            Response<HashMap<String, Object>> response = new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl(baseUrl)
                    .setPath("/slow")
                    .setRequestTimeout(1)
                    .get();

            assertNull(response, "A request that exceeds its timeout is swallowed internally and surfaces as a null response");
        }
    }

    @Nested
    class ParaBuilderTests {

        @Test
        void buildsSingleParameterStringWithoutTrailingAmpersand() {
            HashMap<String, String> params = new HashMap<>();
            params.put("foo", "bar");

            String result = ParaBuilder.buildString(params);

            assertEquals("foo=bar", result);
        }

        @Test
        void combineUrlWithoutParametersAppendsOnlyPath() {
            String result = ParaBuilder.combineUrl("https://example.com", "/path", new HashMap<>());

            assertEquals("https://example.com/path", result);
        }

        @Test
        void combineUrlWithParametersAppendsQueryString() {
            HashMap<String, String> params = new HashMap<>();
            params.put("foo", "bar");

            String result = ParaBuilder.combineUrl("https://example.com", "/path", params);

            assertEquals("https://example.com/path?foo=bar", result);
        }
    }
}