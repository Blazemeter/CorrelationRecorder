package com.blazemeter.jmeter.correlation.core.automatic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class EntityArgumentTest {

  @Test
  public void shouldTakeTheConceptFromThePageWithoutItsExtension() {
    assertThat(EntityArgument.concept("/order.html")).isEqualTo("order");
    assertThat(EntityArgument.concept("/order.html?id=ord_6301cb020b20")).isEqualTo("order");
  }

  @Test
  public void shouldTakeTheLastConceptBeforeIdsAndVersions() {
    assertThat(EntityArgument.concept("/api/orders")).isEqualTo("orders");
    assertThat(EntityArgument.concept("/api/carts/cart_62704d1930ac")).isEqualTo("carts");
    assertThat(EntityArgument.concept("/api/products/p-1001")).isEqualTo("products");
    assertThat(EntityArgument.concept("/api/v2")).isEqualTo("api");
  }

  @Test
  public void shouldHaveNoConceptWithoutANamedSegment() {
    assertThat(EntityArgument.concept("/")).isNull();
    assertThat(EntityArgument.concept("")).isNull();
    assertThat(EntityArgument.concept(null)).isNull();
  }

  @Test
  public void shouldSplitTheNameIntoEntityAndArgument() {
    assertThat(EntityArgument.name("order", "id")).isEqualTo("order.id");
    assertThat(EntityArgument.argument("order.id")).isEqualTo("id");
    assertThat(EntityArgument.argument("order.item.id")).isEqualTo("item.id");
    assertThat(EntityArgument.argument("id")).isEqualTo("id");
  }
}
