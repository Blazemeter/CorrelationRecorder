package com.blazemeter.jmeter.correlation.regression;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMock extends RecordingJtlVisitor {

  private static final Logger LOG = LoggerFactory.getLogger(ClientMock.class);
  // Time needed to wait to avoid ordering issue in recorded samplers due to parallel processing of requests and response to client being sent before processing ends
  private static final Proxy PROXY = new Proxy(java.net.Proxy.Type.HTTP,
      new InetSocketAddress("localhost", 8888));

  private final FileInputStream resultsInputStream;
  private int requestIndex = 1;

  private ClientMock(FileInputStream resultsInputStream) {
    this.resultsInputStream = resultsInputStream;
  }

  public static ClientMock fromJtl(Path recordingJtl) throws FileNotFoundException {
    return new ClientMock(new FileInputStream(recordingJtl.toFile()));
  }

  public void run() throws IOException {
    loadSampleResults(resultsInputStream);
  }

  protected void visit(HTTPSampleResult sample, String localUrl) {
    try {
      String label = sample.getSampleLabel();
      String method = sample.getHTTPMethod();
      String url = "http://" + MOCKED_DOMAIN + localUrl;
      requestIndex++;
      LOG.info("{}/{}: {} {} ...", requestIndex, label.substring(label.lastIndexOf('/') + 1), method, url);
      List<Header> headers = extractHeaders(sample);
      makeHttpRequest(method, url, headers, sample.getQueryString());
    } catch (IOException e) {
      LOG.error("Problem making request", e);
    }
  }

  private List<Header> extractHeaders(HTTPSampleResult sample) {
    List<Header> headers = Arrays.stream(sample.getRequestHeaders().split("\n"))
        .map(l -> new Header(changeDomains2MockedDomain(l)))
        .collect(Collectors.toList());
    String cookies = sample.getCookies();
    if (cookies != null && !cookies.isEmpty()) {
      headers.add(new Header("Cookie: " + cookies));
    }
    return headers;
  }

  private void makeHttpRequest(String method, String url, List<Header> headers, String body)
      throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(PROXY);
    try {
      headers.forEach(h -> connection.setRequestProperty(h.name, h.value));
      connection.setRequestMethod(method);
      if ("POST".equals(method) || "PUT".equals(method)) {
        connection.setDoOutput(true);
        try (Writer writer = new OutputStreamWriter(connection.getOutputStream())) {
          writer.write(body);
        }
      }
      consumeResponse(connection);
      connection.getContent();
    } catch (ConnectException e) {
      LOG.error("Error while attempting to connect to the proxy", e);
    } finally {
      connection.disconnect();
    }
  }

  // we read the response to avoid unexpected brokenPipe in proxy, and emulate better a client
  private void consumeResponse(HttpURLConnection connection) throws IOException {
    try (InputStreamReader in = new InputStreamReader(connection.getInputStream());
        BufferedReader br = new BufferedReader(in)) {
      while (br.readLine() != null) {
      }
    } catch (IOException e) {
      LOG.error("Error reading response", e);
    }
  }
}
