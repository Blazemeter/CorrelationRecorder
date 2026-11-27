package com.blazemeter.jmeter.correlation.core.suggestions.method;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractionSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ReplacementSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.Sources;
import com.blazemeter.jmeter.correlation.core.extractors.JsonCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.Test;

public class SuggestionUnificationTest {

  private static final String ORDER_ID = "ord_6301cb020b20";
  private final Configuration configuration = new Configuration();
  private final SampleResult checkout = result("/api/carts/cart_62704d1930ac/checkout-133");

  @Test
  public void shouldJoinTheNamesThatTakeOneValueFromOneResponse() {
    CorrelationSuggestion orders = suggestion("orders", checkout, ORDER_ID,
        Sources.REQUEST_PATH_ALPHANUMERIC_ID, "/api/orders-142", "/api/orders-144");
    CorrelationSuggestion orderId = suggestion("order.id", checkout, ORDER_ID,
        Sources.REQUEST_ENTITY_ARGUMENT, "/order.html-134");

    List<CorrelationSuggestion> unified = unify(orders, orderId);

    assertThat(unified).containsExactly(orders);
    assertThat(orders.getAliases()).containsExactly("order.id", "orderId");
    assertThat(orders.getExtractionSuggestions()).hasSize(1);
    assertThat(orders.getUsedOnNames())
        .containsExactly("/api/orders-142", "/api/orders-144", "/order.html-134");
    assertThat(orders.toCorrelationRules().keySet().stream()
        .map(CorrelationRule::getReferenceName).distinct().collect(Collectors.toList()))
        .containsExactly("orders");
  }

  @Test
  public void shouldNameTheVariableAfterAnEntityBeforeASpecificOrAGenericName() {
    CorrelationSuggestion generic = suggestion("id", checkout, ORDER_ID,
        Sources.REQUEST_ARGUMENTS, "/a-1", "/a-2", "/a-3");
    CorrelationSuggestion specific = suggestion("orderId", checkout, ORDER_ID,
        Sources.REQUEST_ARGUMENTS, "/b-1", "/b-2");
    CorrelationSuggestion entity = suggestion("order.id", checkout, ORDER_ID,
        Sources.REQUEST_ENTITY_ARGUMENT, "/order.html-134");

    assertThat(unify(generic, specific, entity)).containsExactly(entity);
    assertThat(entity.getAliases()).containsExactly("orderId", "id");
  }

  @Test
  public void shouldKeepApartTheSameValueFromDifferentResponses() {
    CorrelationSuggestion orders = suggestion("orders", checkout, ORDER_ID,
        Sources.REQUEST_PATH_ALPHANUMERIC_ID, "/api/orders-142");
    CorrelationSuggestion orderId = suggestion("order.id", result("/api/orders-144"), ORDER_ID,
        Sources.REQUEST_ENTITY_ARGUMENT, "/order.html-134");

    assertThat(unify(orders, orderId)).containsExactly(orders, orderId);
    assertThat(orders.getAliases()).containsExactly("orderId");
  }

  @Test
  public void shouldKeepApartMultivaluedSuggestionsThatShareOnlyOneValue() {
    CorrelationSuggestion sort = suggestion("sort", checkout, "HasTCD",
        Sources.REQUEST_BODY_JSON, "/xas/-26");
    addExtraction(sort, checkout, "asc");
    CorrelationSuggestion attributes = suggestion("attributes", checkout, "HasTCD",
        Sources.REQUEST_BODY_JSON, "/xas/-27");
    addExtraction(attributes, checkout, "PPSName");

    assertThat(unify(sort, attributes)).containsExactly(sort, attributes);
  }

  @Test
  public void shouldKeepApartDifferentValuesFromOneResponse() {
    CorrelationSuggestion orders = suggestion("orders", checkout, ORDER_ID,
        Sources.REQUEST_PATH_ALPHANUMERIC_ID, "/api/orders-142");
    CorrelationSuggestion carts = suggestion("carts", checkout, "cart_62704d1930ac",
        Sources.REQUEST_PATH_ALPHANUMERIC_ID, "/api/carts-104");

    assertThat(unify(orders, carts)).containsExactly(orders, carts);
  }

  @Test
  public void shouldJoinANameUsedForSomeOfTheValuesOfAnother() {
    CorrelationSuggestion orders = suggestion("orders", checkout, ORDER_ID,
        Sources.REQUEST_PATH_ALPHANUMERIC_ID, "/api/orders-142");
    addExtraction(orders, result("/api/orders-150"), "ord_7a1f00c2d3e4");
    CorrelationSuggestion orderId = suggestion("order.id", checkout, ORDER_ID,
        Sources.REQUEST_ENTITY_ARGUMENT, "/order.html-134");

    assertThat(unify(orders, orderId)).containsExactly(orders);
  }

  @Test
  public void shouldNameAJsonValueAfterItsKey() {
    assertThat(SuggestionUnification.jsonPathName("$.orderId", configuration))
        .isEqualTo("orderId");
    assertThat(SuggestionUnification.jsonPathName("$.order.id", configuration))
        .isEqualTo("order.id");
    assertThat(SuggestionUnification.jsonPathName("$.objects[0].attributes.Name.value",
        configuration)).isEqualTo("Name.value");
    assertThat(SuggestionUnification.jsonPathName(
        "$.changes.387872517997166652.MendixVersion.value", configuration))
        .isEqualTo("MendixVersion.value");
    assertThat(SuggestionUnification.jsonPathName("$[0]", configuration)).isEmpty();
  }

  private List<CorrelationSuggestion> unify(CorrelationSuggestion... suggestions) {
    return SuggestionUnification.unify(Arrays.asList(suggestions), configuration);
  }

  private static CorrelationSuggestion suggestion(String name, SampleResult source, String value,
      String replacementSource, String... usedOn) {
    CorrelationSuggestion suggestion = new CorrelationSuggestion(name, value);
    addExtraction(suggestion, source, value);
    for (String usage : usedOn) {
      HTTPSamplerProxy sampler = new HTTPSamplerProxy();
      sampler.setName(usage);
      ReplacementSuggestion replacement = new ReplacementSuggestion(
          new RegexCorrelationReplacement<>(name + "=([^&]+)"), sampler);
      replacement.setSource(replacementSource);
      replacement.setValue(value);
      replacement.setName(name);
      suggestion.addReplacementSuggestion(replacement);
      suggestion.addUsage(sampler);
    }
    return suggestion;
  }

  private static void addExtraction(CorrelationSuggestion suggestion, SampleResult source,
      String value) {
    JsonCorrelationExtractor<?> extractor = new JsonCorrelationExtractor<>("$.orderId");
    ExtractionSuggestion extraction = new ExtractionSuggestion(extractor, source);
    extraction.setValue(value);
    extraction.setName(suggestion.getParamName());
    suggestion.addExtractionSuggestion(extraction);
    suggestion.addAppearances(source);
  }

  private static SampleResult result(String label) {
    SampleResult result = new SampleResult();
    result.setSampleLabel(label);
    return result;
  }
}
