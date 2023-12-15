package com.blazemeter.jmeter.correlation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import kg.apc.emulators.TestJMeterUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import us.abstracta.jmeter.javadsl.core.engines.JmeterEnvironment;

/**
 * We need this class since we can't directly invoke {@link TestJMeterUtils#createJmeterEnv()}
 * because some times we get "Caused by: java.nio.file.FileAlreadyExistsException:
 * /tmp/jpgc/ut4571494352889939980/ss.props" (both in mac and ubuntu in gitlab).
 */
public class JMeterTestUtils {

  private static boolean jmeterEnvInitialized = false;

  private JMeterTestUtils() {
  }

  public static void setupJmeterEnv() {
    if (!jmeterEnvInitialized) {
      jmeterEnvInitialized = true;
      TestJMeterUtils.createJmeterEnv();
    }
  }

  public static void setupUpdatedJMeter() throws IOException {
    JmeterEnvironment env = new JmeterEnvironment();
  }

  /**
   * This is a builder class for creating HTTPSampler objects.
   * It allows for setting the HTTP method, domain, path, name, and arguments of the HTTPSampler.
   */
  public static class HttpSamplerBuilder {
    private String method = "GET";
    private String domain = "test.com";
    private String path = "/";
    private String name = "name";
    private Map<String, String> arguments = new HashMap<>();

    /**
     * Constructor for the HttpSamplerBuilder class.
     * @param method the HTTP method to use.
     * @param domain the domain of the HTTP request.
     * @param path the path of the HTTP request.
     */
    public HttpSamplerBuilder(String method, String domain, String path) {
      this.method = method;
      this.domain = domain;
      this.path = path;
    }

    /**
     * Sets the name of the HTTPSampler.
     * @param name the name to set.
     * @return the current HttpSamplerBuilder instance.
     */
    public HttpSamplerBuilder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * Adds an argument to the HTTPSampler.
     * @param key the key of the argument.
     * @param value the value of the argument.
     * @return the current HttpSamplerBuilder instance.
     */
    public HttpSamplerBuilder withArgument(String key, String value) {
      arguments.put(key, value);
      return this;
    }

    /**
     * Builds the HTTPSampler with the set parameters.
     * @return a new HTTPSampler instance.
     */
    public HTTPSampler build() {
      HTTPSampler sampler = new HTTPSampler();
      sampler.setDomain(domain);
      sampler.setMethod(method);
      sampler.setPath(path);
      sampler.setName(name);
      for (Map.Entry<String, String> entry : arguments.entrySet()) {
        sampler.addArgument(entry.getKey(), entry.getValue());
      }
      return sampler;
    }
  }

  /**
   * This is a builder class for creating HTTPSampleResult objects.
   * It allows for setting the response body, test URL, sampler data, response code, response message,
   * response headers, request headers, and content type of the HTTPSampleResult.
   */
  public static class SampleResultBuilder {
    private String responseBody;
    private URL testUrl;
    private String samplerData = "Test_SWEACn=123&Test_Path=1";
    private String responseCode = String.valueOf(HttpStatus.SC_OK);
    private String responseMessage = "RESPONSE_MESSAGE";
    private String responseHeaders = "RESPONSE_HEADERS";
    private String requestHeaders = "REQUEST_HEADERS";
    private String contentType = ContentType.TEXT_HTML.toString();

    /**
     * Constructor for the SampleResultBuilder class.
     * @param responseBody the response body to set.
     */
    public SampleResultBuilder(String responseBody) {
      this.responseBody = responseBody;
    }

    /**
     * Sets the test URL of the HTTPSampleResult.
     * @param testUrl the test URL to set.
     * @return the current SampleResultBuilder instance.
     */
    public SampleResultBuilder setTestUrl(URL testUrl) {
      this.testUrl = testUrl;
      return this;
    }

    /**
     * Sets the sampler data of the HTTPSampleResult.
     * @param samplerData the sampler data to set.
     * @return the current SampleResultBuilder instance.
     */
    public SampleResultBuilder setSamplerData(String samplerData) {
      this.samplerData = samplerData;
      return this;
    }

    /**
     * Sets the response code of the HTTPSampleResult.
     * @param responseCode the response code to set.
     * @return the current SampleResultBuilder instance.
     */
    public SampleResultBuilder setResponseCode(String responseCode) {
      this.responseCode = responseCode;
      return this;
    }

    /**
     * Sets the response message of the HTTPSampleResult.
     * @param responseMessage the response message to set.
     * @return the current SampleResultBuilder instance.
     */
    public SampleResultBuilder setResponseMessage(String responseMessage) {
      this.responseMessage = responseMessage;
      return this;
    }

    /**
     * Sets the response headers of the HTTPSampleResult.
     * @param responseHeaders the response headers to set.
     * @return the current SampleResultBuilder instance.
     */
    public SampleResultBuilder setResponseHeaders(String responseHeaders) {
      this.responseHeaders = responseHeaders;
      return this;
    }

    /**
     * Sets the request headers of the HTTPSampleResult.
     * @param requestHeaders the request headers to set.
     * @return the current SampleResultBuilder instance.
     */
    public SampleResultBuilder setRequestHeaders(String requestHeaders) {
      this.requestHeaders = requestHeaders;
      return this;
    }

    /**
     * Sets the content type of the HTTPSampleResult.
     * @param contentType the content type to set.
     * @return the current SampleResultBuilder instance.
     */
    public SampleResultBuilder setContentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    /**
     * Builds the HTTPSampleResult with the set parameters.
     * @return a new HTTPSampleResult instance.
     * @throws MalformedURLException if the test URL is not properly formatted.
     */
    public HTTPSampleResult build() throws MalformedURLException {
      HTTPSampleResult sampleResult = new HTTPSampleResult();
      sampleResult.setURL(testUrl != null ? testUrl : new URL("http://test.com"));
      sampleResult.setSamplerData(samplerData);
      sampleResult.setResponseCode(responseCode);
      sampleResult.setResponseMessage(responseMessage);
      sampleResult.setResponseHeaders(responseHeaders);
      sampleResult.setRequestHeaders(requestHeaders);
      sampleResult.setResponseData(responseBody, HTTPSampleResult.DEFAULT_HTTP_ENCODING);
      sampleResult.setContentType(contentType);
      return sampleResult;
    }
  }

}
