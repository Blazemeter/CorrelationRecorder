package com.blazemeter.jmeter.correlation.gui.automatic;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.gui.automatic.SuggestionAppliedStatus.Cell;
import java.util.Arrays;
import java.util.Collections;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.junit.Before;
import org.junit.Test;

public class SuggestionAppliedStatusTest {

  private JMeterTreeModel model;
  private JMeterTreeNode threadGroup;

  @Before
  public void setUp() throws Exception {
    JMeterTestUtils.setupUpdatedJMeter();
    model = new JMeterTreeModel();
    JMeterTreeNode plan = model.addComponent(new TestPlan(), (JMeterTreeNode) model.getRoot());
    threadGroup = model.addComponent(new ThreadGroup(), plan);
  }

  @Test
  public void shouldListPendingFirstAndCountRepeatedSamplers() {
    Cell cell = Cell.of(Arrays.asList("/a", "/b", "/a", "/c"), "/c"::equals);
    assertThat(cell.getText()).isEqualTo("pending(3)[/a ×2, /b] applied(1)[/c]");
    assertThat(cell.getTooltip()).isEqualTo("<html><b>pending (3)</b><br>&nbsp;&nbsp;/a ×2"
        + "<br>&nbsp;&nbsp;/b<br><b>applied (1)</b><br>&nbsp;&nbsp;/c<br></html>");
  }

  @Test
  public void shouldLeaveOutEmptyGroups() {
    assertThat(Cell.of(Collections.singletonList("/a"), name -> true).getText())
        .isEqualTo("applied(1)[/a]");
    assertThat(Cell.of(Collections.emptyList(), name -> true).getText()).isEmpty();
  }

  @Test
  public void shouldMarkUsesThatAlreadyReferToTheVariable() throws Exception {
    sampler("/api/carts-101", "/api/carts/${carts}/items");
    sampler("/api/orders-144", "/api/orders/ord_6301cb020b20");
    CorrelationSuggestion suggestion = new CorrelationSuggestion("carts", "cart_62704d1930ac");
    suggestion.addUsage(named("/api/carts-101"));
    suggestion.addUsage(named("/api/orders-144"));

    assertThat(new SuggestionAppliedStatus(model).usedOn(suggestion).getText())
        .isEqualTo("pending(1)[/api/orders-144] applied(1)[/api/carts-101]");
  }

  @Test
  public void shouldMarkUsesThatReferToTheVariableInTheirNameFixedOrDynamic() throws Exception {
    sampler("/api/carts/[carts#42]/items-102", "/api/carts/cart_62704d1930ac/items");
    sampler("/api/carts/${carts}/items-103", "/api/carts/cart_62704d1930ac/items");
    sampler("/api/carts-104", "/api/carts/[carts]/items");
    CorrelationSuggestion suggestion = new CorrelationSuggestion("carts", "cart_62704d1930ac");
    suggestion.addUsage(named("/api/carts/[carts#42]/items-102"));
    suggestion.addUsage(named("/api/carts/${carts}/items-103"));
    suggestion.addUsage(named("/api/carts-104"));

    assertThat(new SuggestionAppliedStatus(model).usedOn(suggestion).getText())
        .isEqualTo("pending(1)[/api/carts-104] applied(2)[/api/carts/[carts#42]/items-102,"
            + " /api/carts/${carts}/items-103]");
  }

  @Test
  public void shouldMarkUsesThatReferToAMultivaluedVariableInAHeader() throws Exception {
    JMeterTreeNode sampler = sampler("/api/carts-101", "/api/carts");
    HeaderManager headers = new HeaderManager();
    headers.add(new Header("Authorization", "Bearer ${Authorization#12}"));
    model.addComponent(headers, sampler);
    CorrelationSuggestion suggestion = new CorrelationSuggestion("Authorization", "q2EIFn");
    suggestion.addUsage(named("/api/carts-101"));

    assertThat(new SuggestionAppliedStatus(model).usedOn(suggestion).getText())
        .isEqualTo("applied(1)[/api/carts-101]");
  }

  @Test
  public void shouldMarkSourcesThatAlreadyHaveTheExtractor() throws Exception {
    JMeterTreeNode creates = sampler("/api/carts-100", "/api/carts");
    JSONPostProcessor extractor = new JSONPostProcessor();
    extractor.setName("JSON Path - carts#1");
    model.addComponent(extractor, creates);
    sampler("/api/carts-102", "/api/carts");
    CorrelationSuggestion suggestion = new CorrelationSuggestion("carts", "cart_62704d1930ac");
    suggestion.addAppearances(result("/api/carts-100"));
    suggestion.addAppearances(result("/api/carts-102"));

    assertThat(new SuggestionAppliedStatus(model).obtainedFrom(suggestion).getText())
        .isEqualTo("pending(1)[/api/carts-102] applied(1)[/api/carts-100]");
  }

  @Test
  public void shouldFindSamplersByTheirRecordedName() throws Exception {
    JMeterTreeNode renamed = sampler("Add to cart", "/api/carts/${carts}/items");
    renamed.getTestElement().setComment("ORIGINAL_NAME USED BY CR, DON'T DELETE. Prepend addition"
        + " comments if necessary.;ORIGINAL_NAME=/api/carts-103");
    CorrelationSuggestion suggestion = new CorrelationSuggestion("carts", "cart_62704d1930ac");
    suggestion.addUsage(named("/api/carts-103"));

    assertThat(new SuggestionAppliedStatus(model).usedOn(suggestion).getText())
        .isEqualTo("applied(1)[/api/carts-103]");
  }

  @Test
  public void shouldTreatEverythingAsPendingWithoutATestPlan() {
    CorrelationSuggestion suggestion = new CorrelationSuggestion("carts", "cart_62704d1930ac");
    suggestion.addUsage(named("/api/carts-101"));

    assertThat(new SuggestionAppliedStatus(null).usedOn(suggestion).getText())
        .isEqualTo("pending(1)[/api/carts-101]");
  }

  private JMeterTreeNode sampler(String name, String path) throws Exception {
    HTTPSamplerProxy sampler = new HTTPSamplerProxy();
    sampler.setName(name);
    sampler.setPath(path);
    return model.addComponent(sampler, threadGroup);
  }

  private static TestElement named(String name) {
    HTTPSamplerProxy sampler = new HTTPSamplerProxy();
    sampler.setName(name);
    return sampler;
  }

  private static SampleResult result(String label) {
    SampleResult result = new SampleResult();
    result.setSampleLabel(label);
    return result;
  }
}
