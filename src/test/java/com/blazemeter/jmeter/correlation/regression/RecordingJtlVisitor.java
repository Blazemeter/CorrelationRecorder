package com.blazemeter.jmeter.correlation.regression;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.ResultCollectorHelper;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.visualizers.Visualizer;
import org.eclipse.jetty.http.MimeTypes.Type;

public abstract class RecordingJtlVisitor implements Visualizer {

  private static final Pattern CONTENT_TYPE_PATTERN = Pattern
      .compile("(?i)Content-Type:\\s*([^;\\n]+)");
  protected static final String MOCKED_DOMAIN = "localhost:8080";
  protected final List<String> domains = new ArrayList<>();

  public boolean isStats() {
    return false;
  }

  public void add(SampleResult sample) {
    visit(sample);
  }

  public void loadSampleResults(InputStream resultsInputStream) throws IOException {
    SaveService.loadTestResults(resultsInputStream,
        new ResultCollectorHelper(new ResultCollector(), this));
  }

  private void visit(SampleResult sample) {
    List<SampleResult> subResults = Arrays.asList(sample.getSubResults());
    if (!subResults.isEmpty()) {
      subResults.forEach(this::visit);
    } else if (sample instanceof HTTPSampleResult) {
      visitHttpResult((HTTPSampleResult) sample);
    }
  }

  private void visitHttpResult(HTTPSampleResult sample) {
    String responseCodeStr = sample.getResponseCode();
    if (!responseCodeStr.matches("\\d+")) {
      return;
    }

    Matcher contentTypeMatcher = CONTENT_TYPE_PATTERN.matcher(sample.getResponseHeaders());
    boolean hasContentType = contentTypeMatcher.find();
    if (!hasContentType
        || (!Type.TEXT_HTML.toString().equals(contentTypeMatcher.group(1))
        && !Type.APPLICATION_JSON.toString().equals(contentTypeMatcher.group(1)))) {
      return;
    }
    URL sampleUrl = sample.getURL();
    // There are some special cases where JS is returned as content type text/html which we want to skip
    if (sampleUrl.toString().endsWith(".js")) {
      return;
    }
    domains.add(sampleUrl.getAuthority());
    String localUrl = buildLocalUrl(sampleUrl);
    if (sample.isRedirect()
        && ("https://" + MOCKED_DOMAIN + localUrl).equals(sample.getRedirectLocation())) {
      return;
    }

    visit(sample, localUrl);
  }

  private String buildLocalUrl(URL sampleUrl) {
    String queryString = sampleUrl.getQuery();
    String urlRef = sampleUrl.getRef();
    return sampleUrl.getPath() + (queryString != null && !queryString.isEmpty() ? "?" + queryString
        : "") + (urlRef != null && !urlRef.isEmpty() ? "#" + urlRef : "");
  }

  protected abstract void visit(HTTPSampleResult sample, String localUrl);

  protected String changeDomains2MockedDomain(String body) {
    for (String domain : domains) {
      body = body.replace(domain, MOCKED_DOMAIN);
    }
    return body;
  }

  protected static class Header {

    protected final String name;
    protected final String value;

    protected Header(String line) {
      int headerSeparation = line.indexOf(':');
      name = line.substring(0, headerSeparation).trim();
      value = line.substring(headerSeparation + 1).trim();
    }

    public String toString() {
      return name + " : " + value;
    }

  }

}
