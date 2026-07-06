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
