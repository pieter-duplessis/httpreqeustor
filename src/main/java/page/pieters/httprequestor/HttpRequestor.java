package page.pieters.httprequestor;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import tools.jackson.databind.ObjectMapper;

/**
 * HttpRequestor - Building HTTP requests
 */
public class HttpRequestor<T> {

    private static final Logger LOGGER = Logger.getLogger(HttpRequestor.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client;
    private final Builder request = HttpRequest.newBuilder();
    private final Type responseType;

    private String baseUrl;
    private String path;
    private HashMap<String, String> parameterMap = new HashMap<>();
    private HashMap<String, String> requestHeaders = new HashMap<>();
    private HashMap<String, Object> requestBody;
    private HashMap<String, Object> requestFields;
    private Integer requestTimeout = 30000;


    public HttpRequestor() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof ParameterizedType) {
            this.responseType = ((ParameterizedType) superclass).getActualTypeArguments()[0];
        } else {
            this.responseType = null;
        }
    }

    public HttpRequestor<T> setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public HttpRequestor<T> setPath(String path) {
        this.path = path;
        return this;
    }

    public HttpRequestor<T> addParam(String key, String value) {
        this.parameterMap.put(key, value);
        return this;
    }

    public HttpRequestor<T> addParams(HashMap<String, String> paramsMap) {
        this.parameterMap.putAll(paramsMap);
        return this;
    }

    public HttpRequestor<T> addRequestHeader(String key, String value) {
        this.requestHeaders.put(key, value);
        return this;
    }

    public HttpRequestor<T> addRequestHeaders(HashMap<String, String> requestHeaders) {
        this.requestHeaders.putAll(requestHeaders);
        return this;
    }

    public HttpRequestor<T> setRequestBody(HashMap<String, Object> requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public HttpRequestor<T> setRequestFields(HashMap<String, Object> requestFields) {
        this.requestFields = requestFields;
        return this;
    }

    public HttpRequestor<T> addRequestField(String key, Object value) {

        if (this.requestFields == null)
            this.requestFields = new HashMap<>();

        this.requestFields.put(key, value);
        return this;
    }

    public HttpRequestor<T> setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public Response<T> get() {
        this.request.GET();
        return sendRequest();
    }

    public Response<T> post() {
        this.request.POST(buildBodyPublisher());
        return sendRequest();
    }

    public Response<T> put() {
        this.request.PUT(buildBodyPublisher());
        return sendRequest();
    }

    public Response<T> delete() {
        this.request.DELETE();
        return sendRequest();
    }

    public Response<T> setRequestMethod(String requestMethod) {

        switch (requestMethod) {
            case "GET":
                this.request.GET();
                break;
            case "POST":
                this.request.POST(buildBodyPublisher());
                break;
            case "PUT":
                this.request.PUT(buildBodyPublisher());
                break;
            case "DELETE":
                this.request.DELETE();
                break;
            default:
                this.request.GET();
                LOGGER.log(Level.WARNING, "HttpRequestor: Request method '" + requestMethod + "' is not support, defaulting to GET request method.");
        }

        return sendRequest();
    }

    private HttpRequest.BodyPublisher buildBodyPublisher() {

        try {
            if (requestFields != null && !requestFields.isEmpty()) {
                requestHeaders.putIfAbsent("Content-Type", "application/x-www-form-urlencoded");
                return HttpRequest.BodyPublishers.ofString(ParaBuilder.buildFormUrlEncodedString(requestFields));
            }

            if (requestBody != null) {
                requestHeaders.putIfAbsent("Content-Type", "application/json");
                return HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(requestBody));
            }

            return HttpRequest.BodyPublishers.noBody();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "HttpRequestor: Failed to build request body", e);
            return HttpRequest.BodyPublishers.noBody();
        }
    }

    private Response<T> sendRequest() {

        try {

            for (HashMap.Entry<String, String> entry : requestHeaders.entrySet()) {
                this.request.header(entry.getKey(), entry.getValue());
            }

            this.request
                    .uri(URI.create(ParaBuilder.combineUrl(this.baseUrl, this.path, this.parameterMap)))
                    .timeout(Duration.ofSeconds(requestTimeout));

            return new Response<T>(this.client.send(this.request.build(), HttpResponse.BodyHandlers.ofString()), responseType);



        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "HttpRequestor: IO Exception Occurred", e1);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "HttpRequestor: Exception Occurred", e);
        }

        return null;
    }
}
