package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.junit.Test;

public class RecordingExtractionTest extends TestCase {

  private RecordingExtraction recordingExtraction;

  private Configuration configuration;
  @Override
  public void setUp() throws Exception {
    configuration = new Configuration();
    recordingExtraction = new RecordingExtraction(configuration);
  }

  @Test
  public void testStoreValue() {
    HTTPSamplerBase sampler = new HTTPSamplerProxy();
    sampler.setPath("http://localhost:8080/");
    sampler.setMethod("GET");
    sampler.setName("/xas/-24");

    String key = "object";
    String value = "[{\"guid\":\"387872517997071407\",\"attributes\":{\"ApplicationURL\":{\"readonly\":true,\"value\":null},\"ShowEnvironmentInformation\":{\"readonly\":true,\"value\":true},\"PackageBuildDateTime\":{\"readonly\":true,\"value\":null},\"ModelVersion\":{\"readonly\":true,\"value\":null},\"MendixVersion\":{\"readonly\":true,\"value\":null},\"DTAPLevelAsDecimal\":{\"readonly\":true,\"value\":\"0\"},\"CurrentEnvironmentName\":{\"readonly\":true,\"value\":null},\"CurrentEnvironmentType\":{\"readonly\":true,\"value\":null}},\"hash\":\"2DQC4yRW4RjqKFbvpZyUk54G4MXnvxJlOFB8k0PbwTw=\",\"objectType\":\"DTAP.DTAPInformation\"}]";
    recordingExtraction.storeValue(sampler, key, value);
    Map<String, List<Appearances>> appearanceMap = recordingExtraction.getAppearanceMap();
    value = "";
    System.out.println(appearanceMap);
  }

  @Test
  public void testStoreValue2() {
    HTTPSamplerBase sampler = new HTTPSamplerProxy();
    sampler.setPath("http://localhost:8080/");
    sampler.setMethod("GET");

    String key = "object";
    String value = "[{\"guid\":\"387872517997071407\",\"attributes\":{\"ApplicationURL\":{\"readonly\":true,\"value\":null},\"ShowEnvironmentInformation\":{\"readonly\":true,\"value\":true},\"PackageBuildDateTime\":{\"readonly\":true,\"value\":null},\"ModelVersion\":{\"readonly\":true,\"value\":null},\"MendixVersion\":{\"readonly\":true,\"value\":null},\"DTAPLevelAsDecimal\":{\"readonly\":true,\"value\":\"0\"},\"CurrentEnvironmentName\":{\"readonly\":true,\"value\":null},\"CurrentEnvironmentType\":{\"readonly\":true,\"value\":null}},\"hash\":\"2DQC4yRW4RjqKFbvpZyUk54G4MXnvxJlOFB8k0PbwTw=\",\"objectType\":\"DTAP.DTAPInformation\"}]";
    List<Pair<String, Object>> pairs = JMeterElementUtils.extractDataParametersFromJson(value);
    System.out.println(pairs);
  }
}