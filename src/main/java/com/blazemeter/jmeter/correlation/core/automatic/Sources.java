package com.blazemeter.jmeter.correlation.core.automatic;

public class Sources {

  public static final String REQUEST_BODY_JSON = "Request Body JSON";
  public static final String REQUEST_BODY_JSON_NUMERIC = "Request Body JSON (Numeric)";
  public static final String RESPONSE_BODY_JSON_NUMERIC = "Response Body JSON (Numeric)";
  public static final String REQUEST_HEADER_FIELDS = "Header Request (Fields)";
  public static final String REQUEST_PATH = "Request Path";
  public static final String REQUEST_PATH_NUMBER_FOLLOWED_BY_QUESTION_MARK
      = "Request Path Followed By Question Mark";
  public static final String REQUEST_PATH_NUMBER_FOLLOWED_BY_SLASH
      = "Request Path Followed By Slash";
  public static final String REQUEST_ARGUMENTS = "HTTP arguments";
  public static final String REQUEST_URL = "URL";
  public static final String REQUEST_QUERY = "Request Query";

  public static final String RESPONSE = "Response";
  public static final String RESPONSE_BODY_JSON = "Response Body JSON";
  public static final String REQUEST = "Header";

  public static boolean isRequestSource(String source) {
    return REQUEST_BODY_JSON.equals(source)
        || REQUEST_BODY_JSON_NUMERIC.equals(source)
        || REQUEST_HEADER_FIELDS.equals(source)
        || REQUEST_PATH.equals(source)
        || REQUEST_PATH_NUMBER_FOLLOWED_BY_QUESTION_MARK.equals(source)
        || REQUEST_PATH_NUMBER_FOLLOWED_BY_SLASH.equals(source)
        || REQUEST_ARGUMENTS.equals(source)
        || REQUEST_URL.equals(source)
        || REQUEST_QUERY.equals(source);
  }
}
