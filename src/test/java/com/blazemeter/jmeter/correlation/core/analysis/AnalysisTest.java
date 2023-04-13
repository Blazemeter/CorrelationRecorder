package com.blazemeter.jmeter.correlation.core.analysis;

import static com.blazemeter.jmeter.correlation.TestUtils.findTestFile;

import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.save.SaveService;

public class AnalysisTest {
  protected final String LOG_REQUEST_NAME = "/login-1";
  protected final String LOGGED_REQUEST_NAME = "/loggedUser-2";
  protected final String CALL_REQUEST_NAME = "/callToAction-3";
  protected final String VALUE_NAME = "token";
  protected final String TOKEN_VALUE = "abc123";

  protected final CorrelationExtractor<?> extractor
      = new RegexCorrelationExtractor<>(VALUE_NAME + "=(.*?)&");
  protected final CorrelationReplacement<?> replacement
      = new RegexCorrelationReplacement<>(VALUE_NAME + "=(.+)");

  protected String getExpectedReport(CorrelationRulePartTestElement<?> part) {
    List<Pair<String, CorrelationRulePartTestElement<?>>> parts = Collections
        .singletonList(Pair.of(LOGGED_REQUEST_NAME, part));
    return getExpectedReport(parts);
  }

  protected String getExpectedReport(
      List<Pair<String, CorrelationRulePartTestElement<?>>> parts) {
    StringBuilder reportBuilder = new StringBuilder();
    String RL = System.lineSeparator();
    reportBuilder.append("Correlations Report:").append(RL)
        .append("Total rules appliances=")
        .append(parts.size()).append(".").append(RL);

    reportBuilder.append(" Details by rule part:").append(RL);
    for (Pair<String, CorrelationRulePartTestElement<?>> part : parts) {
      CorrelationRulePartTestElement<?> element = part.getRight();
      reportBuilder.append("  Type='").append(element.getClass().getSimpleName())
          .append("'. Rule Part=")
          .append(element.toString()).append(RL);
      reportBuilder.append("  Can be applied to 1 elements.").append(RL);
      reportBuilder.append("  Entries: ").append(RL);
      reportBuilder.append("    - {value='").append(TOKEN_VALUE).append("' ")
          .append(getAction(element))
          .append(" at 'Correlation Analysis' in '")
          .append(part.getLeft()).append("' with variable name 'token'}").append(RL);
    }
    return reportBuilder.toString();
  }

  protected String getExpectedAnalysisReport(
      List<Pair<String, CorrelationRulePartTestElement<?>>> parts) {
    StringBuilder reportBuilder = new StringBuilder();
    String RL = System.lineSeparator();
    reportBuilder.append("Correlations Report:").append(RL)
        .append("Total rules appliances=")
        .append(parts.size()).append(".").append(RL);

    reportBuilder.append(" Details by rule part:").append(RL);
    for (Pair<String, CorrelationRulePartTestElement<?>> part : parts) {
      CorrelationRulePartTestElement<?> element = part.getRight();
      reportBuilder.append("  Type='").append(element.getClass().getSimpleName())
          .append("'. Rule Part=")
          .append(element.toString()).append(RL);
      reportBuilder.append("  Can be applied to 1 elements.").append(RL);
      reportBuilder.append("  Entries: ").append(RL);
      reportBuilder.append("    - {value='").append(TOKEN_VALUE).append("' ")
          .append(getAction(element))
          .append(" at 'Correlation Analysis' in '")
          .append(part.getLeft()).append("' with variable name 'token'}").append(RL);
    }
    return reportBuilder.toString();
  }

  private static String getAction(CorrelationRulePartTestElement<?> part) {
    if (part instanceof CorrelationExtractor) {
      return "extracted";
    }

    return "replaced";
  }

  protected HTTPSamplerBase createLoggedUserRequest() throws IOException {
    return createSampler("loggedUserRequest.jmx");
  }

  protected HTTPSamplerBase createSampler(String testElementFilename) throws IOException {
    File file = findTestFile(testElementFilename);
    HTTPSamplerBase sampler = (HTTPSamplerBase) SaveService.loadTree(file).getArray()[0];
    sampler.setRunningVersion(true);
    return sampler;
  }
}
