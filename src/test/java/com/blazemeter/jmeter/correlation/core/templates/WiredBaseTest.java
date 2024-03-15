package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class WiredBaseTest {
    protected final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

    protected static final String WIRED_HOST = "localhost";
    protected static final String TEST_REPOSITORY_URL = "/test-repository.json";
    protected static final String FIRST_1_0_TEMPLATE_URL = "/first-1.0-template.json";
    protected static final String FIRST_1_0_SNAPSHOT_URL = "/first-1.0-snapshot.png";
    protected static final String FIRST_1_1_TEMPLATE_URL = "/first-1.1-template.json";
    protected static final String FIRST_1_1_SNAPSHOT_URL = "/first-1.1-snapshot.png";
    protected static final String SECOND_1_0_TEMPLATE_URL = "/second-1.0-template.json";
    protected static final String SECOND_1_0_SNAPSHOT_URL = "/second-1.0-snapshot.png";

    protected void startWiredMock(){
        startWiredMock("");
    }
    protected void startWiredMock(String scenario){
        wireMockServer.start();
        configureFor(WIRED_HOST, wireMockServer.port());
        if (scenario != null && !scenario.isEmpty()){
            wireMockServer.startRecording(scenario);
        }
    }

    protected void stopWiredMock(){
        if (wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    protected MappingBuilder prepareURL(String URL) throws IOException {
        return mockingResponse(URL, 200, TestUtils.getFileContent(URL, getClass()), "get");
    }

    public MappingBuilder mockingResponse(String URL, int status, String body, String method) {
        MappingBuilder routeMapping;
        if (method.equals("head")) {
            routeMapping = head(urlEqualTo(URL));
        } else {
            routeMapping = get(urlEqualTo(URL));
        }

        wireMockServer.stubFor(routeMapping.willReturn(aResponse()
                .withStatus(status)
                .withBody(body)
                .withHeader("Cache-Control", "no-cache")
                .withHeader("Content-Type", "application/json")));
        return routeMapping;
    }

    protected void skipURL(String URL) {
        stubFor(get(urlEqualTo(URL)).willReturn(aResponse()
                .withStatus(304)
        ));
    }

    protected void mockRequestsToRepoFiles() throws IOException {
        prepareURL(TEST_REPOSITORY_URL);
        prepareURL(FIRST_1_0_TEMPLATE_URL);
        skipURL(FIRST_1_0_SNAPSHOT_URL);
        prepareURL(FIRST_1_1_TEMPLATE_URL);
        skipURL(FIRST_1_1_SNAPSHOT_URL);
        prepareURL(SECOND_1_0_TEMPLATE_URL);
        skipURL(SECOND_1_0_SNAPSHOT_URL);
    }

    protected String getBaseURL() {
        return "http://" + WIRED_HOST + ":" + wireMockServer.port();
    }


    protected Integer getWiredPort(){
        return wireMockServer.port();
    }
}
