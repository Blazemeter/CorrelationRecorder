package com.blazemeter.jmeter.correlation.core.automatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.JMeterTestUtils.SampleResultBuilder;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultExtractionTest {

  private static final String ORIGINAL_UUID = "165ec9bd-df8b-45b3-bd3b-e42ad535bb91";
  private static final String RETRY_UUID = "a45de3b0-68ef-4b9b-acc1-51b242f71419";
  private ResultsExtraction resultExtraction;
  private String filePath;
  private static final String REPLAY_RESOURCE_PATH = "./jtl/replay/";
  private static final String RECORDING_RESOURCE_PATH = "./jtl/recording/";
  private static final String JSON_CONTENTS_PATH = "responses/json/";

  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();

  @Mock
  public ResultFileParser fileParser;
  private Map<String, List<Appearances>> appearanceMap;

  @BeforeClass
  public static void setUpClass() throws IOException {
    JMeterTestUtils.setupUpdatedJMeter();
    JMeterUtils.setProperty("correlation.configuration.min_value_length", "1");
  }

  @Before
  public void setUp() throws Exception {
    resultExtraction = new ResultsExtraction(new Configuration());

  }

  @Test
  public void shouldExtractJSONTokenAppearancesFromJTLReplayWhenExtractAppearanceMap() {
    String fileName = "replay-get-json-token-send-json-token.jtl";
    filePath = Resources.getResource(REPLAY_RESOURCE_PATH + fileName).getPath();
    appearanceMap = resultExtraction.extractAppearanceMap(filePath);

    assert appearanceMap.containsKey("token");
    List<Appearances> tokenAppearanceList = appearanceMap.get("token");
    Appearances[] expectedJSONAppearance = buildExpectedJSONAppearance(RETRY_UUID,
        ORIGINAL_UUID).toArray(new Appearances[]{});

    assertThatActualAppearancesEqualToExpected(tokenAppearanceList, expectedJSONAppearance);
  }

  private void assertThatActualAppearancesEqualToExpected(List<Appearances> actual,
      Appearances[] expected) {
    softly.assertThat(actual)
        .usingRecursiveFieldByFieldElementComparator()
        .usingElementComparatorIgnoringFields("list")
        .containsOnly(expected);
  }

  private List<Appearances> buildExpectedJSONAppearance(String responseToken,
      String requestToken) {
    Appearances responseAppearance = new Appearances(responseToken, "token", null);
    responseAppearance.setSource(Sources.RESPONSE_BODY_JSON);
    Appearances requestAppearance = new Appearances(requestToken, "token", null);
    requestAppearance.setSource(Sources.REQUEST_BODY_JSON);
    return Arrays.asList(responseAppearance, requestAppearance);
  }

  @Test
  public void shouldExtractJSONTokenAppearancesFromJTLRecordingWhenExtractAppearanceMap() {
    String fileName = "recording-get-json-token-send-json-token.jtl";
    filePath = Resources.getResource(RECORDING_RESOURCE_PATH + fileName).getPath();
    appearanceMap = resultExtraction.extractAppearanceMap(filePath);

    assert appearanceMap.containsKey("token");
    List<Appearances> tokenAppearanceList = appearanceMap.get("token");
    assertThatActualAppearancesEqualToExpected(tokenAppearanceList,
        buildExpectedJSONAppearance(ORIGINAL_UUID,
            ORIGINAL_UUID).toArray(new Appearances[]{}));
  }

  @Test
  public void shouldRetrieveAppearancesWhenJSONObjectListResponse() throws Exception {
    Pair<String, String> resource = getResourcePathContent("single-object.json");
    initResponseBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(1);
    Appearances[] expectedAppearance = {buildAppearance("randomToken1", "token",
        Sources.RESPONSE_BODY_JSON)};
    List<Appearances> actualAppearance = appearanceMap.get("token");
    assertThatActualAppearancesEqualToExpected(actualAppearance, expectedAppearance);
  }

  private void initResponseBodyMockTest(Pair<String, String> resource)
      throws MalformedURLException {
    List<SampleResult> results = new ArrayList<>();
    results.add(new SampleResultBuilder(resource.getRight()).build());
    when(fileParser.loadFromFile(any(), anyBoolean())).thenReturn(results);
    resultExtraction.setResultFileParser(fileParser);
  }

  private Pair<String, String> getResourcePathContent(String fileName) throws IOException {
    URL resourceUri = Resources.getResource(JSON_CONTENTS_PATH + fileName);
    return Pair.of(resourceUri.getPath(), Resources.toString(resourceUri,
        StandardCharsets.UTF_8));
  }

  private Appearances buildAppearance(String value, String name, String source) {
    Appearances expectedAppearance = new Appearances(value, name, null);
    expectedAppearance.setSource(source);
    return expectedAppearance;
  }

  @Test
  public void shouldRetrieveAppearancesWhenJSONObjectListInnerObjectResponse() throws Exception {
    Pair<String, String> resource = getResourcePathContent("object-list-inner-object.json");
    initResponseBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(1);
    Appearances[] expectedAppearance = {
        buildAppearance("randomToken1", "token", Sources.RESPONSE_BODY_JSON),
        buildAppearance("randomToken2", "token", Sources.RESPONSE_BODY_JSON),
        buildAppearance("randomToken3", "token", Sources.RESPONSE_BODY_JSON)};
    List<Appearances> actualAppearance = appearanceMap.get("token");
    assertThatActualAppearancesEqualToExpected(actualAppearance, expectedAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJSONObjectListWithDuplicatedValuesResponse()
      throws Exception {

    Pair<String, String> resource = getResourcePathContent("object-list-with-duplicated-values"
        + ".json");
    initResponseBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(1);
    Appearances[] expectedAppearance = {
        buildAppearance("randomToken1", "token", Sources.RESPONSE_BODY_JSON),
        buildAppearance("randomToken1", "token", Sources.RESPONSE_BODY_JSON),
        buildAppearance("randomToken1", "token", Sources.RESPONSE_BODY_JSON)};
    List<Appearances> actualAppearance = appearanceMap.get("token");
    assertThatActualAppearancesEqualToExpected(actualAppearance, expectedAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJsonObjectWithInnerListResponse() throws Exception {

    Pair<String, String> resource = getResourcePathContent("object-with-inner-list.json");
    initResponseBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(2);
    List<Appearances> actualTokenAppearance = appearanceMap.get("token");
    List<Appearances> actualSessionIdAppearance = appearanceMap.get("sessionID");

    Appearances[] expectedTokenAppearance = {
        buildAppearance("randomToken1", "token", Sources.RESPONSE_BODY_JSON),
        buildAppearance("randomToken2", "token", Sources.RESPONSE_BODY_JSON),
        buildAppearance("randomToken3", "token", Sources.RESPONSE_BODY_JSON)};
    Appearances[] expectedSessionIdAppearance = {
        buildAppearance("10", "sessionID", Sources.RESPONSE_BODY_JSON_NUMERIC),
        buildAppearance("20", "sessionID", Sources.RESPONSE_BODY_JSON_NUMERIC),
        buildAppearance("30", "sessionID", Sources.RESPONSE_BODY_JSON_NUMERIC)};

    assertThatActualAppearancesEqualToExpected(actualTokenAppearance, expectedTokenAppearance);
    assertThatActualAppearancesEqualToExpected(actualSessionIdAppearance,
        expectedSessionIdAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJsonObjectWithIntegerKeyResponse() throws Exception {
    Pair<String, String> resource = getResourcePathContent("object-with-integer-key.json");
    initResponseBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(1);
    List<Appearances> actualAppearance = appearanceMap.get("15");

    Appearances[] expectedAppearance = {
        buildAppearance("randomToken1", "15", Sources.RESPONSE_BODY_JSON)};
    assertThatActualAppearancesEqualToExpected(actualAppearance, expectedAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJsonObjectWithSpecialCharactersResponse()
      throws Exception {
    Pair<String, String> resource = getResourcePathContent("object-with-special-characters.json");
    initResponseBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(3);
    List<Appearances> actualTestAppearance = appearanceMap.get("test");
    List<Appearances> actualInternalTokenAppearance = appearanceMap.get("..internal.token=:");
    List<Appearances> actualComAppearance = appearanceMap.get("com");

    Appearances[] expectedTestAppearance = {
        buildAppearance("a-value", "test", Sources.RESPONSE_BODY_JSON)};
    Appearances[] expectedInternalAppearance = {
        buildAppearance("randomToken1", "..internal.token=:", Sources.RESPONSE_BODY_JSON)};
    Appearances[] expectedComAppearance = {
        buildAppearance("a-value", "com", Sources.RESPONSE_BODY_JSON)};

    assertThatActualAppearancesEqualToExpected(actualTestAppearance, expectedTestAppearance);
    assertThatActualAppearancesEqualToExpected(actualInternalTokenAppearance,
        expectedInternalAppearance);
    assertThatActualAppearancesEqualToExpected(actualComAppearance, expectedComAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJsonSingleObjectResponse() throws Exception {
    Pair<String, String> resource = getResourcePathContent("single-object.json");
    initResponseBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());
    softly.assertThat(appearanceMap).size().isEqualTo(1);

    Appearances[] expectedAppearance = {
        buildAppearance("randomToken1", "token", Sources.RESPONSE_BODY_JSON)};
    List<Appearances> actualAppearance = appearanceMap.get("token");

    assertThatActualAppearancesEqualToExpected(actualAppearance, expectedAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJSONObjectListRequest() throws Exception {
    Pair<String, String> resource = getResourcePathContent("single-object.json");
    initRequestBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(1);
    Appearances[] expectedAppearance = {buildAppearance("randomToken1", "token",
        Sources.REQUEST_BODY_JSON)};
    List<Appearances> actualAppearance = appearanceMap.get("token");
    assertThatActualAppearancesEqualToExpected(actualAppearance, expectedAppearance);
  }

  private void initRequestBodyMockTest(Pair<String, String> resource)
      throws MalformedURLException {
    List<SampleResult> results = new ArrayList<>();
    results.add(new SampleResultBuilder("").setQueryString(resource.getRight()).build());
    when(fileParser.loadFromFile(any(), anyBoolean())).thenReturn(results);
    resultExtraction.setResultFileParser(fileParser);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJSONObjectListInnerObjectRequest() throws Exception {
    Pair<String, String> resource = getResourcePathContent("object-list-inner-object.json");
    initRequestBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(1);
    Appearances[] expectedAppearance = {
        buildAppearance("randomToken1", "token", Sources.REQUEST_BODY_JSON),
        buildAppearance("randomToken2", "token", Sources.REQUEST_BODY_JSON),
        buildAppearance("randomToken3", "token", Sources.REQUEST_BODY_JSON)};
    List<Appearances> actualAppearance = appearanceMap.get("token");
    assertThatActualAppearancesEqualToExpected(actualAppearance, expectedAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJSONObjectListWithDuplicatedValuesRequest()
      throws Exception {

    Pair<String, String> resource = getResourcePathContent("object-list-with-duplicated-values"
        + ".json");
    initRequestBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(1);
    Appearances[] expectedAppearance = {
        buildAppearance("randomToken1", "token", Sources.REQUEST_BODY_JSON),
        buildAppearance("randomToken1", "token", Sources.REQUEST_BODY_JSON),
        buildAppearance("randomToken1", "token", Sources.REQUEST_BODY_JSON)};
    List<Appearances> actualAppearance = appearanceMap.get("token");
    assertThatActualAppearancesEqualToExpected(actualAppearance, expectedAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJsonObjectWithInnerListRequest() throws Exception {

    Pair<String, String> resource = getResourcePathContent("object-with-inner-list.json");
    initRequestBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(2);
    List<Appearances> actualTokenAppearance = appearanceMap.get("token");
    List<Appearances> actualSessionIdAppearance = appearanceMap.get("sessionID");

    Appearances[] expectedTokenAppearance = {
        buildAppearance("randomToken1", "token", Sources.REQUEST_BODY_JSON),
        buildAppearance("randomToken2", "token", Sources.REQUEST_BODY_JSON),
        buildAppearance("randomToken3", "token", Sources.REQUEST_BODY_JSON)};
    Appearances[] expectedSessionIdAppearance = {
        buildAppearance("10", "sessionID", Sources.REQUEST_BODY_JSON_NUMERIC),
        buildAppearance("20", "sessionID", Sources.REQUEST_BODY_JSON_NUMERIC),
        buildAppearance("30", "sessionID", Sources.REQUEST_BODY_JSON_NUMERIC)};

    assertThatActualAppearancesEqualToExpected(actualTokenAppearance, expectedTokenAppearance);
    assertThatActualAppearancesEqualToExpected(actualSessionIdAppearance,
        expectedSessionIdAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJsonObjectWithIntegerKeyRequest() throws Exception {
    Pair<String, String> resource = getResourcePathContent("object-with-integer-key.json");
    initRequestBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(1);
    List<Appearances> actualAppearance = appearanceMap.get("15");

    Appearances[] expectedAppearance = {
        buildAppearance("randomToken1", "15", Sources.REQUEST_BODY_JSON),};
    assertThatActualAppearancesEqualToExpected(actualAppearance, expectedAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJsonObjectWithSpecialCharactersRequest()
      throws Exception {
    Pair<String, String> resource = getResourcePathContent("object-with-special-characters.json");

    List<SampleResult> results = new ArrayList<>();
    results.add(new SampleResultBuilder("").setHttpMethod("POST").setQueryString(resource.getRight()).build());
    when(fileParser.loadFromFile(any(), anyBoolean())).thenReturn(results);
    resultExtraction.setResultFileParser(fileParser);

    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());

    softly.assertThat(appearanceMap).size().isEqualTo(3);
    List<Appearances> actualTestAppearance = appearanceMap.get("test");
    List<Appearances> actualInternalTokenAppearance = appearanceMap.get("..internal.token=:");
    List<Appearances> actualComAppearance = appearanceMap.get("com");

    Appearances[] expectedTestAppearance = {
        buildAppearance("a-value", "test", Sources.REQUEST_BODY_JSON)};
    Appearances[] expectedInternalAppearance = {
        buildAppearance("randomToken1", "..internal.token=:", Sources.REQUEST_BODY_JSON)};
    Appearances[] expectedComAppearance = {
        buildAppearance("a-value", "com", Sources.REQUEST_BODY_JSON)};

    assertThatActualAppearancesEqualToExpected(actualTestAppearance, expectedTestAppearance);
    assertThatActualAppearancesEqualToExpected(actualInternalTokenAppearance,
        expectedInternalAppearance);
    assertThatActualAppearancesEqualToExpected(actualComAppearance, expectedComAppearance);
  }

  @Test
  public void shouldRetrieveAppearancesWhenJsonSingleObjectRequest() throws Exception {
    Pair<String, String> resource = getResourcePathContent("single-object.json");
    initRequestBodyMockTest(resource);
    appearanceMap = resultExtraction.extractAppearanceMap(resource.getLeft());
    softly.assertThat(appearanceMap).size().isEqualTo(1);

    Appearances[] expectedAppearance = {
        buildAppearance("randomToken1", "token", Sources.REQUEST_BODY_JSON)};
    List<Appearances> actualAppearance = appearanceMap.get("token");

    assertThatActualAppearancesEqualToExpected(actualAppearance, expectedAppearance);
  }
}
