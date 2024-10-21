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

  protected final String LOGGED_REQUEST_NAME = "/loggedUser-2";
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
        .append("  Total rules appliances=")
        .append(parts.size()).append(".").append(RL);

    reportBuilder.append(" Details by rule part:").append(RL);
    for (Pair<String, CorrelationRulePartTestElement<?>> part : parts) {
      CorrelationRulePartTestElement<?> element = part.getRight();
      reportBuilder.append("  Type=").append(element.getClass().getSimpleName()).append(RL)
          .append("  Reference Name=")
          .append(getRefVarName(part.getRight())).append(RL)
          .append("  Rule Part=").append(part.getRight()).append(RL);

      reportBuilder.append("\t\tCan be applied to 1 elements.").append(RL);
      reportBuilder.append("\t\t Entries: ").append(RL);
      reportBuilder.append("    - { value='")
          .append(TOKEN_VALUE)
          .append("'  at '")
          .append(getLocationFrom(part.getRight())).append("' ")
          .append("in '")
          .append(part.getLeft()).append("'}").append(RL);
    }
    return reportBuilder.toString();
  }

  private String getLocationFrom(CorrelationRulePartTestElement<?> right) {
    if (right instanceof CorrelationExtractor) {
      return ((CorrelationExtractor<?>) right).getTarget().toString();
    }
    return "Correlation Analysis";
  }

  private String getRefVarName(CorrelationRulePartTestElement<?> right) {
    if (right instanceof CorrelationExtractor) {
      return ((CorrelationExtractor<?>) right).getVariableName();
    } else if (right instanceof CorrelationReplacement) {
      return ((CorrelationReplacement<?>) right).getVariableName();
    }
    return "Unsupported CorrelationPartTestElement";
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
