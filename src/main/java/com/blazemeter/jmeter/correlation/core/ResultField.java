package com.blazemeter.jmeter.correlation.core;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.Document;
import org.apache.jmeter.util.JMeterUtils;

public enum ResultField {
  URL(JMeterUtils.getResString("regex_src_url"), RegexExtractor.USE_URL,
      SampleResult::getUrlAsString),
  RESPONSE_HEADERS(JMeterUtils.getResString("regex_src_hdrs"), RegexExtractor.USE_HDRS,
      SampleResult::getResponseHeaders),
  REQUEST_HEADERS(JMeterUtils.getResString("regex_src_hdrs_req"), RegexExtractor.USE_REQUEST_HDRS,
      SampleResult::getRequestHeaders),
  RESPONSE_CODE(JMeterUtils.getResString("assertion_code_resp"), RegexExtractor.USE_CODE,
      SampleResult::getResponseCode),
  RESPONSE_MESSAGE(JMeterUtils.getResString("assertion_message_resp"), RegexExtractor.USE_MESSAGE,
      SampleResult::getResponseMessage),
  BODY_UNESCAPED(JMeterUtils.getResString("regex_src_body_unescaped"),
      RegexExtractor.USE_BODY_UNESCAPED,
      r -> StringEscapeUtils.unescapeHtml(r.getResponseDataAsString())),
  BODY_AS_A_DOCUMENT(JMeterUtils.getResString("regex_src_body_as_document"),
      RegexExtractor.USE_BODY_AS_DOCUMENT, r -> Document.getTextFromDocument(r.getResponseData())),
  BODY(JMeterUtils.getResString("regex_src_body"), RegexExtractor.USE_BODY,
      SampleResult::getResponseDataAsString);

  private final String description;
  private final Function<SampleResult, String> getFieldFunction;
  private String code;

  ResultField(String description, String code, Function<SampleResult, String> getFieldFunction) {
    this.description = description;
    this.code = code;
    this.getFieldFunction = getFieldFunction;
  }

  public static Map<String, String> getNamesToCodesMapping() {

    return Arrays.stream(ResultField.values())
        .collect(Collectors.toMap(ResultField::name, ResultField::getCode));
  }

  public String getField(SampleResult r) {
    return getFieldFunction.apply(r);
  }

  public String getCode() {
    return code;
  }

  public String toString() {
    return description;
  }

}
