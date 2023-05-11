package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that stores configurations for the automatic correlation. Such as:
 * - The minimum length of a value to be considered as a candidate for correlation.
 * - The list of ignored domains.
 * - The list of ignored parameters in headers.
 * - The list of ignored type of files
 */
public class Configuration {
  private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
  private static final int DEFAULT_MIN_LENGTH = 2;
  private static final int DEFAULT_CONTEXT_LENGTH = 10;
  private static final int MAX_NUMBER_OF_APPEARANCES = 500;
  private static final boolean IGNORE_BOOLEAN_VALUES = true;
  private static final List<String> DEFAULT_IGNORED_DOMAINS = Arrays.asList("mozilla.org",
      "mozilla.net", "mozilla.com",
      "content-signature-2.cdn.mozilla.net", "push.services.mozilla.com",
      "classify-client.services.mozilla.com", "1aus5.mozilla.org");
  private static final List<String> DEFAULT_IGNORED_HEADERS = Arrays.asList(
      "Referer", "Allow",
      "Origin", "Host", "User-Agent", "If-Modified-Since", "Content-Length",
      "Location", "Accept-Ranges", "X-Pingback", "Timing-Allow-Origin",
      "Accept-Encoding", "Connection", "Accept", "Accept-Language",
      "Cache-Control", "Pragma", "Upgrade-Insecure-Requests", "vary",
      "Server", "Location", "Origin", "X-Frame-Options", "Access-Control-Allow-Origin",
      "Access-Control-Allow-Methods", "Last-Modified",
      "X-HTTP-Method-Override",
      "X-Content-Type-Options", "X-Robots-Tag", "Referrer-Policy",
      "Content-Type", "Content-Length", "Cache-Control",
      "Content-Encoding", "Content-Disposition",
      "Access-Control-Allow-Headers", "Access-Control-Expose-Headers",
      "X-Requested-With", "Transfer-Encoding", "X-Redirect-By",
      "Content-Security-Policy", "Strict-Transport-Security",
      "max-age", "max-stale", "min-fresh", "no-cache", "no-store", "no-transform",
      "only-if-cached", "stale-if-error", "Sec-CH-UA-Mobile",
      "Sec-Fetch-Mode", "Sec-Fetch-Site", "Sec-Fetch-User", "Sec-Fetch-Dest",
      "CF-ray", "X-nc", "X-XSS-Protection", "Content-Security-Policy-Report-Only"
  );
  private static final List<String> DEFAULT_IGNORED_FILES = Arrays.asList("jpg",
      "jpeg", "png", "css", "js", "woff", "txt", "svg", "ico", "pdf", "zip",
      "gzip", "tar", "gz", "rar", "7z", "exe", "msi", "woff2");
  private static final List<String> DEFAULT_IGNORED_PARAMETERS = Arrays.asList("log",
      "pwd", "password", "pass", "passwd", "action", "testcookie", "ver", "widget",
      "d", "r", "s", "ipv6", "ipv4", "remind_me_later",
      "redirect_to", "pagenow", "if-modified-since", "url", "redirect", "redirect_uri",
      "host", "expires", "date", "as", "rel",
      "link", "returl", "dur", "vary", "connection");

  private int minLength;
  private final int contextLength;
  private int maxNumberOfAppearances;
  private boolean ignoreBooleanValues;
  private final List<String> ignoredDomains;
  private final List<String> ignoredHeaders;
  private List<String> ignoredFiles;
  private List<String> ignoredParameters;

  private final List<String> requestedParameters;

  public Configuration() {
    this.minLength = JMeterUtils.getPropDefault("correlation.configuration.min_value_length",
        DEFAULT_MIN_LENGTH);
    this.contextLength = JMeterUtils.getPropDefault("correlation.configuration.context_length",
        DEFAULT_CONTEXT_LENGTH);
    this.maxNumberOfAppearances = JMeterUtils.getPropDefault("max_number_of_appearances",
        MAX_NUMBER_OF_APPEARANCES);
    this.ignoreBooleanValues
        = JMeterUtils.getPropDefault("correlation.configuration.ignore_boolean_values",
        IGNORE_BOOLEAN_VALUES);
    this.ignoredDomains = getDefaultListValues("correlation.configuration.ignored_domains",
        DEFAULT_IGNORED_DOMAINS);
    this.ignoredHeaders = getDefaultListValues("correlation.configuration.ignored_headers",
        DEFAULT_IGNORED_HEADERS);
    this.ignoredFiles = getDefaultListValues("correlation.configuration.ignored_files",
        DEFAULT_IGNORED_FILES);
    this.ignoredParameters = getDefaultListValues("correlation.configuration.ignored_keys",
        DEFAULT_IGNORED_PARAMETERS);
    requestedParameters = new ArrayList<>();
    displayConfiguration();
  }

  private void displayConfiguration() {
    LOG.trace("The following configuration were loaded for the automatic correlation process:");
    LOG.trace("Minimum length: {}", minLength);
    LOG.trace("Context length: {}", contextLength);
    LOG.trace("Max number of appearances: {}", maxNumberOfAppearances);
    LOG.trace("Ignore boolean values: {}", ignoreBooleanValues);
    LOG.trace("Ignored domains: {}", ignoredDomains);
    LOG.trace("Ignored headers: {}", ignoredHeaders);
    LOG.trace("Ignored files: {}", ignoredFiles);
    LOG.trace("Ignored parameters: {}", ignoredParameters);
  }

  private List<String> getDefaultListValues(String property, List<String> defaultValues) {
    String rawString = JMeterUtils.getPropDefault(property, "");
    return rawString.isEmpty() ? defaultValues
        : Arrays.stream(rawString.split(",")).map(String::trim).collect(Collectors.toList());
  }

  public int getMinLength() {
    return minLength;
  }

  public void setMinLength(int minLength) {
    this.minLength = minLength;
  }

  public int getContextLength() {
    return contextLength;
  }

  public int getMaxNumberOfAppearances() {
    return maxNumberOfAppearances;
  }

  public boolean shouldIgnoreBooleanValues() {
    return ignoreBooleanValues;
  }

  public void setIgnoreBooleanValues(boolean ignoreBooleanValues) {
    this.ignoreBooleanValues = ignoreBooleanValues;
  }

  public List<String> getIgnoredDomains() {
    return ignoredDomains;
  }

  public List<String> getIgnoredHeaders() {
    return ignoredHeaders;
  }

  public List<String> getIgnoredFiles() {
    return ignoredFiles;
  }

  public void setIgnoredFiles(List<String> ignoredFiles) {
    this.ignoredFiles = ignoredFiles;
  }

  public List<String> getIgnoredParameters() {
    return ignoredParameters;
  }

  public void setIgnoredParameters(List<String> ignoredParameters) {
    this.ignoredParameters = ignoredParameters;
  }

  public void addIgnoredParameters(List<String> ignoredParameters) {
    this.ignoredParameters.addAll(ignoredParameters);
  }

  public void setMaxNumberOfAppearances(int maxNumberOfAppearances) {
    this.maxNumberOfAppearances = maxNumberOfAppearances;
  }

  public void addRequestedParameters(List<String> requestedParameters) {
    this.requestedParameters.addAll(requestedParameters);
  }

  public List<String> getRequestedParameters() {
    return requestedParameters;
  }

  @Override
  public String toString() {
    return "Configuration {"
        + "minLength=" + minLength
        + ", contextLength=" + contextLength
        + ", maxNumberOfAppearances=" + maxNumberOfAppearances
        + ", ignoreBooleanValues=" + ignoreBooleanValues
        + ", ignoredDomains=" + ignoredDomains
        + ", ignoredHeaders=" + ignoredHeaders
        + ", ignoredFiles=" + ignoredFiles
        + ", ignoredParameters=" + ignoredParameters
        + ", requestedParameters=" + requestedParameters
        + '}';
  }
}
