package com.blazemeter.jmeter.correlation.core.automatic;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.helger.commons.annotation.VisibleForTesting;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.protocol.http.control.Cookie;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPConstants;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for extracting the appearances of the parameters from the results. The
 * appearances are extracted from the results of the JMeter test plan. The results are parsed to
 * SampleResults and then the appearances are extracted from the SampleResults.
 */
public class ResultsExtraction implements AppearancesExtraction {

  private static final Logger LOG = LoggerFactory.getLogger(ResultsExtraction.class);

  private static final List<String> DYNAMIC_COOKIES =
      Arrays.asList("PHPSESSID", "JSESSIONID", "ASPSESSIONID",
          "connect.sid");
  private final JMeterElementUtils utils;
  private final Configuration configuration;
  private Map<String, List<Appearances>> appearanceMap;

  private Map<String, HTTPSamplerProxy> requestsMap;
  private ResultFileParser resultFileParser;

  public ResultsExtraction(Configuration configuration) {
    this.configuration = configuration;
    this.utils = new JMeterElementUtils(configuration);
  }

  /**
   * Convert string to cookie.
   *
   * @param cookieStr the cookie as a string
   * @param url to extract domain and path for the cookie from
   * @return list of cookies
   */
  public static List<Cookie> stringToCookie(String cookieStr, String url) {
    List<Cookie> cookies = new ArrayList<>();
    final StringTokenizer tok = new StringTokenizer(cookieStr, "; ", true);
    while (tok.hasMoreTokens()) {
      String nextCookie = tok.nextToken();
      if (nextCookie.contains("=")) {
        String[] cookieParameters = nextCookie.split("=", 2);
        if (!DYNAMIC_COOKIES.contains(cookieParameters[0])) {
          Cookie newCookie = new Cookie();
          newCookie.setName(cookieParameters[0]);
          newCookie.setValue(cookieParameters[1]);
          URL newUrl;
          try {
            newUrl = new URL(url.trim());
            newCookie.setDomain(newUrl.getHost());
            newCookie.setPath(newUrl.getPath());
            cookies.add(newCookie);
          } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                "unqualified url " + url.trim() + ", unable to create cookies.");
          }
        }
      }
    }
    return cookies;
  }

  private static HTTPSamplerProxy parseToHttpSampler(HTTPSampleResult httpResult) {
    HTTPSamplerProxy sampler = new HTTPSamplerProxy();
    sampler.setName(httpResult.getSampleLabel());
    URL url = httpResult.getURL();
    sampler.setDomain(url.getHost());
    sampler.setPort(url.getPort());
    sampler.setPath(url.getPath());
    sampler.setMethod(httpResult.getHTTPMethod());
    sampler.setProtocol(url.getProtocol());
    sampler.setFollowRedirects(true);
    sampler.setUseKeepAlive(true);
    sampler.setDoMultipartPost(false);
    sampler.setMonitor(false);
    sampler.setEmbeddedUrlRE("");
    sampler.setConnectTimeout("");
    sampler.setResponseTimeout("");

    LinkedHashMap<String, String> headers = JMeterUtils.parseHeaders(
        httpResult.getRequestHeaders());
    String connection = headers.get("Connection");
    if (connection != null) {
      sampler.setUseKeepAlive(!"close".equalsIgnoreCase(connection));
    }

    boolean isJson = false;
    List<Pair<String, String>> parameters;
    ContentType contentType = getRequestContentType(headers);
    String type = contentType.getMimeType();
    if (type.equals(ContentType.MULTIPART_FORM_DATA.getMimeType())) {
      parameters = getParameterListFromMultiPartBody(httpResult, contentType);
      sampler.setDoMultipartPost(true);
    } else if (type.equals(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
      parameters = getParametersFromFormUrlEncodedBody(httpResult, contentType);
    } else if (type.equals(ContentType.APPLICATION_JSON.getMimeType()) || JMeterElementUtils.isJson(
        httpResult.getQueryString())) {
      isJson = true;
      parameters = getParametersFromJsonBody(httpResult, contentType);
    } else {
      parameters = getParameterListFromQuery(httpResult);
    }

    for (Pair<String, String> param : parameters) {
      String value = param.getValue();
      String decode = JMeterElementUtils.decode(value);
      if (!value.equals(decode)) {
        try {
          sampler.addEncodedArgument(param.getKey(), value);
        } catch (IllegalArgumentException e) {
          LOG.warn("Error adding argument to the sampler,  {}", httpResult.getSampleLabel(), e);
          e.printStackTrace();
        }
        continue;
      }
      sampler.addArgument(param.getKey(), value);
    }

    if (isJson) {
      sampler.setPostBodyRaw(true);
      // When Body is in Raw, the path need to use also the query string
      String query = httpResult.getURL().getQuery();
      if (!isEmpty(query)) {
        sampler.setPath(url.getPath() + "?" + query);
      }
    }

    return sampler;
  }

  private static List<Pair<String, String>> getParametersFromJsonBody(HTTPSampleResult httpResult,
      ContentType contentType) {
    String body = httpResult.getQueryString();
    List<Pair<String, String>> parameters = new ArrayList<>();
    parameters.add(Pair.of("", body));
    return parameters;
  }

  private static List<Pair<String, String>> getParameterListFromMultiPartBody(
      HTTPSampleResult httpResult, ContentType contentType) {
    List<Pair<String, String>> parameters = new ArrayList<>();
    String body = httpResult.getSamplerData();
    String[] parts = body.split("--" + contentType.getParameter("boundary"));
    for (String part : parts) {
      if (!part.contains("Content-Disposition: form-data;")) {
        continue;
      }

      String name = "";
      String value = "";
      for (String line : part.split("\\r?\\n")) {
        if (line.contains("Content-Disposition: form-data;")) {
          name = line.split("name=\"")[1].split("\"")[0];
        } else if (!line.isEmpty()) {
          value = line;
        }
      }
      Pair<String, String> parameter = Pair.of(name, value);
      parameters.add(parameter);
    }
    return parameters;
  }

  private static List<Pair<String, String>> getParametersFromFormUrlEncodedBody(
      HTTPSampleResult httpResult, ContentType contentType) {
    String line = httpResult.getQueryString();
    if (line == null || line.isEmpty()) {
      URL url = httpResult.getURL();
      line = url.getQuery() != null ? url.getQuery() : "";
      if (line.isEmpty()) {
        return new ArrayList<>();
      }
    }
    String[] pairs = line.split("\\&");
    List<Pair<String, String>> parameters = new ArrayList<>();
    for (int i = 0; i < pairs.length; i++) {
      String[] fields = pairs[i].split("=");
      try {
        Charset charset = contentType.getCharset();
        if (charset == null) {
          charset = StandardCharsets.UTF_8;
        }

        String name = URLDecoder.decode(fields[0], charset.name());
        String value = fields.length > 1 ? URLDecoder.decode(fields[1], charset.name()) : "";
        parameters.add(Pair.of(name, value));
      } catch (UnsupportedEncodingException | ArrayIndexOutOfBoundsException e) {
        // I need to check each field and corroborate if it is Decoded before split by "="
        e.printStackTrace();
      }
    }
    return parameters;
  }

  private static List<Pair<String, String>> getParameterListFromQuery(HTTPSampleResult httpResult) {
    String queryString = httpResult.getQueryString();
    if (JMeterElementUtils.isJson(queryString)) {
      return new ArrayList<>();
    }
    URL url = httpResult.getURL();
    String query = url.getQuery() != null ? url.getQuery() : "";

    List<Pair<String, String>> parameterList = new ArrayList<>();
    String[] parameters = new String[0];
    if (!query.isEmpty()) {
      parameters = query.split(JMeterElementUtils.URL_PARAM_SEPARATOR);
    }

    if (queryString != null && !queryString.isEmpty()) {
      String[] queryStringParameters = queryString.split(JMeterElementUtils.URL_PARAM_SEPARATOR);
      if (queryStringParameters.length > 0) {
        // Add the queryStringParameters to the parameters
        parameters = ArrayUtils.addAll(parameters, queryStringParameters);
      }
    }

    for (String parameter : parameters) {
      String[] parameterParts = parameter.split(JMeterElementUtils.URL_PARAM_VALUE_SEPARATOR, 0);
      if (parameterParts.length == 2) {
        parameterList.add(Pair.of(parameterParts[0], parameterParts[1]));
      }
    }
    return parameterList;
  }

  private static ContentType getRequestContentType(LinkedHashMap<String, String> headers) {
    String contentType = headers.get("Content-Type");
    //Some servers are not case-sensitive when parsing the content-type
    String contentTypeLowerCased = headers.get("content-type");
    if (contentType != null) {
      return ContentType.parse(contentType);
    }

    if (contentTypeLowerCased != null) {
      return ContentType.parse(contentTypeLowerCased);
    }

    return ContentType.DEFAULT_TEXT;
  }

  @Override
  public Map<String, List<Appearances>> extractAppearanceMap(String filepath) {
    appearanceMap = new HashMap<>();
    resultFileParser = getResultFileParser();
    List<SampleResult> results = resultFileParser.loadFromFile(new File(filepath), true);

    extractAppearancesFromResults(results);
    resultFileParser = null;
    return appearanceMap;
  }

  private ResultFileParser getResultFileParser() {
    return resultFileParser == null ? new ResultFileParser(configuration) : resultFileParser;
  }

  private void extractAppearancesFromResults(List<SampleResult> results) {
    RecordingExtraction samplersExtractor = new RecordingExtraction(configuration, appearanceMap);
    for (SampleResult result : results) {
      if (result instanceof HTTPSampleResult) {
        try {
          HTTPSampleResult httpSampleResult = (HTTPSampleResult) result;
          HTTPSamplerProxy sourceRequest = parseToHttpSampler(httpSampleResult);
          if (requestsMap != null && requestsMap.containsKey(result.getSampleLabel())) {
            sourceRequest = requestsMap.get(result.getSampleLabel());
          }
          samplersExtractor.extractParametersFromHttpSampler(sourceRequest);
          extractAppearancesFromSampleResult(httpSampleResult, sourceRequest);
        } catch (Exception ex) { //Capture any exception to avoid teardown all flow
          LOG.error("Error Extracting Parameters from result", ex);
        }
      }
    }
  }

  private void extractAppearancesFromSampleResult(HTTPSampleResult httpSampleResult,
      HTTPSamplerProxy sourceRequest) {
    extractParametersFromHeaderStrings(httpSampleResult.getResponseHeaders(), sourceRequest,
        "Response");
    extractParametersFromHeaderStrings(httpSampleResult.getRequestHeaders(), sourceRequest,
        "Request");
    String body = httpSampleResult.getResponseDataAsString();
    if (JMeterElementUtils.isJson(body)) {
      extractAppearancesFromJson(body, sourceRequest);
    }
  }

  private void extractAppearancesFromJson(String json, HTTPSamplerProxy sourceRequest) {
    try {
      if (JMeterElementUtils.isJsonArray(json)) {
        utils.extractParametersFromJsonArray(new JSONArray(json), appearanceMap, sourceRequest, "",
            Sources.RESPONSE_BODY_JSON);
        return;
      }
      utils.extractParametersFromJson(new JSONObject(json), appearanceMap, sourceRequest,
          Sources.RESPONSE_BODY_JSON);
    } catch (Exception ex) {
      LOG.error("Error parsing JSON", ex);
    }
  }

  private void extractParametersFromHeaderStrings(String headerString,
      HTTPSamplerProxy sourceRequest,
      String headerSource) {
    String[] headerLines = headerString.split("\\n", 0);
    for (String headerLine : headerLines) {
      if (headerLine.indexOf(":") > 0) {
        String headerName = headerLine.substring(0, headerLine.indexOf(":"));
        String headerValue = headerLine.substring(headerLine.indexOf(":") + 1).trim();
        if (utils.canBeFiltered(headerName, headerValue)) {
          continue;
        }
        if (equalsIgnoreCase(headerName, "Set-Cookie")) {
          registerHeaderCookie(headerValue, sourceRequest, headerSource);
          continue;
        } else if (hasParameters(headerValue)) {
          registerHeaderSubParameters(headerName, headerValue, sourceRequest, headerSource);
          continue;
        } else if (equalsIgnoreCase(headerName, HTTPConstants.HEADER_AUTHORIZATION)) {
          String token = headerValue.trim().split(" ")[1];
          utils.addToMap(appearanceMap, headerName, token, sourceRequest,
              "Header " + headerSource + " (Fields)");
          continue;
        }
        utils.addToMap(appearanceMap, headerName, headerValue, sourceRequest,
            "Header " + headerSource + " (Fields)");
      }
    }
  }

  //Header's fields might contain parameters: name/value pairs with auxiliary information
  //Source: https://datatracker.ietf.org/doc/html/rfc9110#section-5.6.6
  private boolean hasParameters(String headerValue) {
    return headerValue.contains(";") && headerValue.contains("=");
  }

  private void registerHeaderCookie(String headerValue, HTTPSamplerProxy sourceRequest,
      String headerSource) {
    String[] fields = headerValue.split(";");

    String name = "";
    String value = "";

    // The first element is the key value
    if (fields[0].contains("=")) {
      String[] fieldValues = fields[0].split("=");
      name = fieldValues[0].trim();
      value = fieldValues.length > 1 ? fieldValues[1].trim() : "";
    }
    if (utils.canBeFiltered(name, value)) {
      return;
    }

    // Set Path in the name if exist
    Map<String, String> context = new HashMap<String, String>();
    for (int i = 1; i < fields.length; i++) {
      String field = fields[i];

      if (containsIgnoreCase(field, "Path") || containsIgnoreCase(field, "Domain")) {
        String[] fieldValues = field.split("=");
        context.put(fieldValues[0].trim(), fieldValues.length > 1 ? fieldValues[1].trim() : "");
      }
    }
    if (context.size() > 0) {
      name += context.keySet().stream()
          .map(key -> key + "=" + context.get(key))
          .collect(Collectors.joining(", ", "[", "]"));
    }
    String source = "Header " + headerSource + " (Set-Cookie)";
    // The value is saved decoded because the source is a header value
    String decodedValue = JMeterElementUtils.decode(value);
    utils.addToMap(appearanceMap, name, decodedValue, sourceRequest, source);

    // And also encoded but with a post_fix
    utils.addToMap(appearanceMap, name + "_encoded", value, sourceRequest, source);

    if (containsIgnoreCase(name, "Authorization") && containsIgnoreCase(value, "OAuth")) {
      // Register the value of the authorization
      String authValue = decodedValue.replaceAll("(?i)\"OAuth\"", "").trim();
      utils.addToMap(appearanceMap, name + "_auth", authValue,
          sourceRequest, source);
      String authValueEncoded = value.replaceAll("(?i)\"OAuth\"", "").trim();
      utils.addToMap(appearanceMap, name + "_auth" + "_encoded", authValueEncoded,
          sourceRequest, source);
    }

  }

  private void registerHeaderSubParameters(String headerName, String headerValue,
      HTTPSamplerProxy sourceRequest, String headerSource) {
    String[] fields = headerValue.split(";");
    for (int i = 0; i < fields.length; i++) {
      String field = fields[i];
      String name = headerName;
      String value = field;

      if (field.contains("=")) {
        String[] fieldValues = field.split("=");
        name = fieldValues[0];
        value = fieldValues[1];
      }

      if (utils.canBeFiltered(name, value)) {
        continue;
      }

      utils.addToMap(appearanceMap, name, value, sourceRequest,
          "Header " + headerSource + " (Sub-Parameters)");
    }
  }

  public void setRequestsMap(Map<String, HTTPSamplerProxy> requestsMap) {
    this.requestsMap = requestsMap;
  }

  @VisibleForTesting
  public void setResultFileParser(ResultFileParser resultFileParser) {
    this.resultFileParser = resultFileParser;
  }
}
