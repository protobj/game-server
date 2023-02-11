package org.protobj.mock.javafx;

import java.util.ArrayList;
import java.util.List;

public class MockServerOption {
    public static List<MockServerOption> mockServerOptions = new ArrayList<>();

    static {
        MockServerOption mockServerOption = new MockServerOption();
        mockServerOption.name = "chenqiang";
        mockServerOption.serverIds = new ArrayList<String>() {{
            add("99");
        }};
        mockServerOption.gateways = new ArrayList<String>() {{
            add("172.16.30.13:8199");
        }};
        mockServerOptions.add(mockServerOption);
    }

    private String name;
    private List<String> gateways;
    private List<String> serverIds;

    public String getName() {
        return name;
    }

    public List<String> getGateways() {
        return gateways;
    }

    public List<String> getServerIds() {
        return serverIds;
    }
}
