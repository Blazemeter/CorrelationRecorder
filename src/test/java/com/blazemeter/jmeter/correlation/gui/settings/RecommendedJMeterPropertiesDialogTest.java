package com.blazemeter.jmeter.correlation.gui.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.apache.jmeter.util.JMeterUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RecommendedJMeterPropertiesDialogTest {

  private static final String REDIRECT = "proxy.redirect.disabling";
  private static final String DEFLATE = "httpclient4.deflate_relax_mode";

  private final FakeApplier applier = new FakeApplier();
  private RecommendedJMeterPropertiesDialog dialog;

  @Before
  public void setup() throws IOException {
    JMeterTestUtils.setupUpdatedJMeter();
    removeProperties();
  }

  @After
  public void tearDown() {
    if (dialog != null) {
      dialog.dispose();
    }
    removeProperties();
  }

  private static void removeProperties() {
    JMeterUtils.getJMeterProperties().remove(REDIRECT);
    JMeterUtils.getJMeterProperties().remove(DEFLATE);
  }

  private void openDialog() {
    dialog = new RecommendedJMeterPropertiesDialog(null,
        RecommendedJMeterProperties.REQUIREMENTS, applier);
  }

  @Test
  public void shouldWarnWithTheEffectOfEachPropertyWithoutTheExpectedValue() {
    JMeterUtils.setProperty(DEFLATE, "true");
    openDialog();

    assertThat(find("effect." + REDIRECT)).isNotNull();
    assertThat(find("effect." + DEFLATE)).isNull();
    assertThat(((JLabel) find("status." + REDIRECT)).getToolTipText())
        .isEqualTo("Does not have the expected value");
    assertThat(((JLabel) find("status." + DEFLATE)).getToolTipText())
        .isEqualTo("Has the expected value");
    assertThat(((JLabel) find("summary")).getText()).contains("1 of 2 JMeter properties");
    assertThat(find("applyButton").isEnabled()).isTrue();
  }

  @Test
  public void shouldMarkEveryPropertyValidAndNotOfferToApplyWhenAllAreExpected() {
    JMeterUtils.setProperty(REDIRECT, "false");
    JMeterUtils.setProperty(DEFLATE, "true");
    openDialog();

    assertThat(find("effect." + REDIRECT)).isNull();
    assertThat(find("effect." + DEFLATE)).isNull();
    assertThat(((JLabel) find("summary")).getText())
        .contains("All JMeter properties have the values");
    assertThat(find("applyButton").isEnabled()).isFalse();
  }

  @Test
  public void shouldApplyTheUnmetPropertiesWhenConfirmed() {
    JMeterUtils.setProperty(DEFLATE, "true");
    openDialog();
    applier.confirms = true;

    ((JButton) find("applyButton")).doClick();

    assertThat(applier.confirmed).extracting(JMeterPropertyRequirement::getName)
        .containsExactly(REDIRECT);
    assertThat(applier.applied).extracting(JMeterPropertyRequirement::getName)
        .containsExactly(REDIRECT);
  }

  @Test
  public void shouldNotApplyWhenTheUserCancels() {
    openDialog();
    applier.confirms = false;

    ((JButton) find("applyButton")).doClick();

    assertThat(applier.confirmed).hasSize(2);
    assertThat(applier.applied).isNull();
  }

  private Component find(String name) {
    return find(dialog.getContentPane(), name);
  }

  private static Component find(Container container, String name) {
    for (Component component : container.getComponents()) {
      if (name.equals(component.getName())) {
        return component;
      }
      if (component instanceof Container) {
        Component found = find((Container) component, name);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static class FakeApplier implements RecommendedJMeterPropertiesDialog.Applier {

    private boolean confirms;
    private List<JMeterPropertyRequirement> confirmed;
    private List<JMeterPropertyRequirement> applied;

    @Override
    public boolean confirm(Component parent, List<JMeterPropertyRequirement> unmet) {
      confirmed = new ArrayList<>(unmet);
      return confirms;
    }

    @Override
    public void apply(Component parent, List<JMeterPropertyRequirement> unmet) {
      applied = new ArrayList<>(unmet);
    }
  }
}
