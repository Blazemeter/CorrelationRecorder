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

  public void testAlphanumericIdInTheMiddleOfAPathIsACandidate() {
    Map<String, List<Appearances>> map = pathCandidates("/api/carts/cart_62704d1930ac/items");
    assertEquals(1, map.get("carts").size());
    Appearances candidate = map.get("carts").get(0);
    assertEquals("cart_62704d1930ac", candidate.getValue());
    assertEquals(Sources.REQUEST_PATH_ALPHANUMERIC_ID, candidate.getSource());
  }

  public void testUuidAtTheEndOfAPathIsACandidate() {
    assertEquals("3fa85f64-5717-4562-b3fc-2c963f66afa6",
        pathCandidates("/orders/3fa85f64-5717-4562-b3fc-2c963f66afa6?expand=items")
            .get("orders").get(0).getValue());
  }

  public void testGenericSegmentsAreNotCandidates() {
    // words, short codes and versions are parts of the route, not ids
    assertTrue(pathCandidates("/api/v2/carts/items/checkout").isEmpty());
    assertTrue(pathCandidates("/api/products/p-1001").isEmpty());
    assertTrue(pathCandidates("/assets/main.3f2a1b9c4d").isEmpty());
  }

  public void testNumericIdsKeepTheirOwnRule() {
    Map<String, List<Appearances>> map = pathCandidates("/wp-json/wp/v2/posts/1418");
    assertEquals(Sources.REQUEST_PATH, map.get("posts").get(0).getSource());
    assertEquals(1, map.size());
  }

  public void testGenericArgumentIsNamedAfterItsEntity() {
    Map<String, List<Appearances>> map = argumentCandidates("/order.html", "id",
        "ord_6301cb020b20");
    Appearances candidate = map.get("order.id").get(0);
    assertEquals("ord_6301cb020b20", candidate.getValue());
    assertEquals(Sources.REQUEST_ENTITY_ARGUMENT, candidate.getSource());
    assertFalse(map.containsKey("id"));
  }

  public void testGenericArgumentsOfDifferentEntitiesAreApart() {
    argumentCandidates("/order.html", "id", "ord_6301cb020b20");
    Map<String, List<Appearances>> map = argumentCandidates("/product.html", "id", "p-1001");
    assertEquals("ord_6301cb020b20", map.get("order.id").get(0).getValue());
    assertEquals("p-1001", map.get("product.id").get(0).getValue());
  }

  public void testGenericArgumentWithoutEntityKeepsItsName() {
    assertEquals(Sources.REQUEST_ARGUMENTS,
        argumentCandidates("/", "id", "ord_6301cb020b20").get("id").get(0).getSource());
  }

  public void testOtherArgumentsKeepTheirName() {
    assertEquals(Sources.REQUEST_ARGUMENTS, argumentCandidates("/order.html", "orderId",
        "ord_6301cb020b20").get("orderId").get(0).getSource());
  }

  private Map<String, List<Appearances>> argumentCandidates(String path, String name,
      String value) {
    HTTPSamplerBase sampler = new HTTPSamplerProxy();
    sampler.setDomain("localhost");
    sampler.setPath(path);
    sampler.setMethod("GET");
    sampler.addArgument(name, value);
    recordingExtraction.extractParametersFromHttpSampler(sampler);
    return recordingExtraction.getAppearanceMap();
  }

  private Map<String, List<Appearances>> pathCandidates(String path) {
    HTTPSamplerBase sampler = new HTTPSamplerProxy();
    sampler.setDomain("localhost");
    sampler.setPath(path);
    sampler.setMethod("GET");
    recordingExtraction.extractParametersFromHttpSampler(sampler);
    return recordingExtraction.getAppearanceMap();
  }

  public void testIsAlphanumericId() {
    assertTrue(RecordingExtraction.isAlphanumericId("ord_6301cb020b20"));
    assertFalse(RecordingExtraction.isAlphanumericId("checkout"));
    assertFalse(RecordingExtraction.isAlphanumericId("12345678"));
    assertFalse(RecordingExtraction.isAlphanumericId("a1b2c3"));
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