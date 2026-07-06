package page.pieters.httprequestor;

import java.util.HashMap;

public class SandboxTesting {

    public static void main(String[] args) {

        System.out.println("---START----------------------------------------------------------------------------------");
        System.out.println("Test HttpRequestor Plugin");

        try {

            HashMap<String, String> paramsGeneralGet = new HashMap<>();
            paramsGeneralGet.put("foo1", "bar1");
            paramsGeneralGet.put("foo2", "bar2");
            paramsGeneralGet.put("foo3", "bar3");
            paramsGeneralGet.put("foo4", "bar4");

            HashMap<String, String> headersGeneralGet = new HashMap<>();
            headersGeneralGet.put("Content-Type", "application/json");

            Response<HashMap<String, Object>> responseGeneralGet = new HttpRequestor<HashMap<String, Object>>() {}
                    .setBaseUrl("https://postman-echo.com")
                    .setPath("/get")
                    .addParams(paramsGeneralGet)
                    .get();

            System.out.println("HttpRequestor Sandbox: Response for General GET request");
            System.out.println(responseGeneralGet.statusCode);
            System.out.println(responseGeneralGet.body);
            System.out.println(responseGeneralGet.body.getClass());
            System.out.println(responseGeneralGet.body.get("args"));
            System.out.println(responseGeneralGet.body.get("url"));
            System.out.println(responseGeneralGet.headers);

        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println("\n---END------------------------------------------------------------------------------------");
    }
}
