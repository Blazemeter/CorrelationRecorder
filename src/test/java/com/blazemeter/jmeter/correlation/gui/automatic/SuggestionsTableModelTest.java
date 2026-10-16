package com.blazemeter.jmeter.correlation.gui.automatic;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.gui.automatic.CorrelationSuggestionsPanel.SuggestionsTableModel;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class SuggestionsTableModelTest {

  private static final int NAME = 2;
  private static final int VALUE = 3;
  private final SuggestionsTableModel model = new SuggestionsTableModel();
  private final CorrelationSuggestion plain = new CorrelationSuggestion("carts",
      "cart_62704d1930ac");
  private final CorrelationSuggestion oneAlias = new CorrelationSuggestion("guids",
      "43628621391136691");
  private final CorrelationSuggestion twoAliases = new CorrelationSuggestion("orders",
      "ord_6301cb020b20");

  @Before
  public void setUp() {
    oneAlias.addAlias("guid");
    twoAliases.addAlias("order.id");
    twoAliases.addAlias("orderId");
    model.loadSuggestions(Arrays.asList(plain, oneAlias, twoAliases));
  }

  @Test
  public void shouldHoldOnlyTheNameInTheCell() {
    assertThat(model.getValueAt(2, NAME)).isEqualTo("orders");
  }

  @Test
  public void shouldDrawTheFirstAliasAndCutTheRest() {
    assertThat(model.getDisplayNameAt(0)).isEqualTo("carts");
    assertThat(model.getDisplayNameAt(1)).isEqualTo("guids (alias: guid)");
    assertThat(model.getDisplayNameAt(2)).isEqualTo("orders (alias: order.id, ...)");
  }

  @Test
  public void shouldListEveryAliasInTheTooltip() {
    assertThat(model.getToolTipAt(0, NAME)).isNull();
    assertThat(model.getToolTipAt(2, NAME)).isEqualTo(
        "<html><b>aliases (2)</b><br>&nbsp;&nbsp;order.id<br>&nbsp;&nbsp;orderId</html>");
  }

  @Test
  public void shouldDetailTheNameWithEveryAlias() {
    assertThat(model.getDetailAt(0, NAME)).isEqualTo("carts");
    assertThat(model.getDetailAt(2, NAME)).isEqualTo("orders\n\nAliases:\norder.id\norderId");
  }

  @Test
  public void shouldDetailOtherColumnsWithTheirText() {
    assertThat(model.getDetailAt(2, VALUE)).isEqualTo(model.getValueAt(2, VALUE));
  }
}
