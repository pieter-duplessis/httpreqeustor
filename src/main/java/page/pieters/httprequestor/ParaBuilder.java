package page.pieters.httprequestor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * ParaBuilder - Parameter Builder for http requests.
 */
public class ParaBuilder {

    public static String buildString(HashMap<String, String> parameterMap) {

        return buildParameterString(parameterMap);
    }

    public static String combineUrl(String baseUrl, String path, HashMap<String, String> parameterMap) {

        if (parameterMap.isEmpty())
            return baseUrl + path;
        return baseUrl + path + "?" + buildParameterString(parameterMap);
    }

    public static String buildFormUrlEncodedString(HashMap<String, Object> fields) {

        StringBuilder sb = new StringBuilder();

        for (HashMap.Entry<String, Object> entry : fields.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
        }

        return sb.toString();
    }

    private static String buildParameterString(HashMap<String, String> parameterMap) {

        StringBuilder sb = new StringBuilder();
        int currentIndex = 0;
        int lastIndex = parameterMap.size() - 1;

        for (HashMap.Entry<String, String> entry : parameterMap.entrySet()) {

            if (currentIndex == lastIndex) {
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            } else {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }

            currentIndex++;
        }

        return sb.toString();
    }
}
