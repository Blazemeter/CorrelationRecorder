package com.blazemeter.jmeter.correlation.regression;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;

public class ServerMock extends RecordingJtlVisitor implements Closeable {

  private static final List<StringReplacement> DYNAMIC_REQUEST_REPLACEMENTS = Arrays
      .asList(new StringReplacement("SWETS=(\\d+)", "SWETS=(\\\\d+)"),
          new StringReplacement("A_(\\d-\\w{8})", "[Aa]_$1"));
  private static final Pattern REGEX_CHARS_PATTERN = Pattern.compile("[\\[\\\\^$.|?*+(){}]");
  private static final List<String> IGNORED_HEADERS = Arrays
      .asList("content-length", "content-encoding", "transfer-encoding");
  private static final String SCENARIO_INITIAL_STATE = "Started";

  private final WireMockServer wireMockServer;
  private final Map<String, MappingScenario> scenarioByKey = new HashMap<>();

  private ServerMock() {
    wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .disableRequestJournal()
        .port(8080));
  }

  public static ServerMock fromJtl(Path recordingJtl) throws IOException {
    ServerMock server = new ServerMock();
    server.loadMappings(recordingJtl);
    server.start();
    return server;
  }

  private void loadMappings(Path recordingJtl) throws IOException {
    loadSampleResults(new FileInputStream(recordingJtl.toFile()));
  }

  protected void visit(HTTPSampleResult sample, String localUrl) {
    try {
      String method = sample.getHTTPMethod();
      MappingBuilder mapping;
      if (isDynamicRequest(localUrl)) {
        localUrl = buildRequestPattern(localUrl);
        mapping = WireMock.request(method, WireMock.urlMatching(localUrl));
      } else {
        mapping = WireMock.request(method, WireMock.urlEqualTo(localUrl));
      }
      String requestBody = sample.getQueryString();
      if (requestBody != null && !requestBody.isEmpty()) {
        if (isDynamicRequest(requestBody)) {
          requestBody = buildRequestPattern(requestBody);
          mapping = mapping.withRequestBody(WireMock.matching(requestBody));
        } else {
          mapping = mapping.withRequestBody(WireMock.equalTo(requestBody));
        }
      }
      mapping.willReturn(
          WireMock.aResponse()
              .withStatus(Integer.parseInt(sample.getResponseCode()))
              .withHeaders(buildMappingResponseHeaders(sample))
              .withBody(changeDomains2MockedDomain(sample.getResponseDataAsString()))
      );
      addMappingByKey(mapping, method + ":" + localUrl + ":" + requestBody);
      wireMockServer.stubFor(mapping);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Could not build mapping for request " + sample.getSampleLabel(), e);
    }
  }

  private boolean isDynamicRequest(String request) {
    return DYNAMIC_REQUEST_REPLACEMENTS.stream().anyMatch(r -> r.matches(request));
  }

  private String buildRequestPattern(String req) {
    req = escapeRegexChars(req);
    for (StringReplacement r : DYNAMIC_REQUEST_REPLACEMENTS) {
      req = r.apply(req);
    }
    return req;
  }

  private String escapeRegexChars(String val) {
    return REGEX_CHARS_PATTERN.matcher(val).replaceAll("\\\\$0");
  }

  private void addMappingByKey(MappingBuilder mapping, String mappingKey) {
    MappingScenario scenario = scenarioByKey.get(mappingKey);
    if (scenario != null) {
      scenario.lastState++;
      if (scenario.lastState == 1) {
        scenario.lastMapping.inScenario(mappingKey)
            .whenScenarioStateIs(SCENARIO_INITIAL_STATE);
      }
      String scenarioState = String.valueOf(scenario.lastState);
      scenario.lastMapping.inScenario(mappingKey)
          .willSetStateTo(scenarioState);
      mapping.inScenario(mappingKey)
          .whenScenarioStateIs(scenarioState)
          .willSetStateTo(SCENARIO_INITIAL_STATE);
    } else {
      scenario = new MappingScenario();
      scenarioByKey.put(mappingKey, scenario);
    }
    scenario.lastMapping = mapping;
  }

  private static class MappingScenario {
    private int lastState;
    private MappingBuilder lastMapping;
  }

  private HttpHeaders buildMappingResponseHeaders(HTTPSampleResult sample) {
    String responseHeadersString = sample.getResponseHeaders();
    int statusLineEnd = responseHeadersString.indexOf('\n');
    List<Header> headers = Arrays.stream(responseHeadersString.substring(statusLineEnd + 1)
        .split("\n"))
        .map(Header::new)
        .filter(h -> {
          String headerName = h.name.toLowerCase();
          return IGNORED_HEADERS.stream().noneMatch(ig -> ig.equals(headerName));
        })
        .collect(Collectors.toList());
    Map<String, Collection<String>> map = new LinkedHashMap<>();
    for (Header h : headers) {
      Collection<String> values = map.computeIfAbsent(h.name, k -> new ArrayList<>());
      values.add(changeDomains2MockedDomain(h.value));
    }
    return new HttpHeaders(map.entrySet().stream()
        .map(e -> new HttpHeader(e.getKey(), e.getValue()))
        .collect(Collectors.toList()));
  }

  private void start() {
    wireMockServer.start();
  }

  public void close() {
    wireMockServer.stop();
    System.out.println("ServerMock closed/stopped");
  }

  public void reset() {
    wireMockServer.resetScenarios();
  }

  public int getScenariosCount() {
    return scenarioByKey.size();
  }
}

