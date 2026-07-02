package page.pieters.httprequestor;

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

    private static String buildParameterString(HashMap<String, String> parameterMap) {

        StringBuilder sb = new StringBuilder();

        for (HashMap.Entry<String, String> entry : parameterMap.entrySet()) {

            if (sb.length() == 0) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            } else {
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        return sb.toString();
    }
}
