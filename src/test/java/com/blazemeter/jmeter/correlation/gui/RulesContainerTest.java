package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.assertj.swing.timing.Pause.pause;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate;
import java.awt.Point;
import java.util.Collections;
import java.util.HashSet;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPanelFixture;
import org.assertj.swing.timing.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RulesContainerTest {

  private static final String REFERENCE_VARIABLE_SUFFIX = "-referenceVariable";
  private static final String ADD_BUTTON = "addButton";
  private static final String DELETE_BUTTON = "deleteButton";
  private static final String UP_BUTTON = "upButton";
  private static final String DOWN_BUTTON = "downButton";
  private static final String CLEAR_BUTTON = "clearButton";
  private static final String EXPORT_BUTTON = "exportButton";
  private static final String COLLAPSIBLE_PANE = "CollapsiblePane";
  private static final String RESPONSE_FILTER_LABEL = "responseFilterLabel";
  private static final String EXTENSIONS_TEXT_AREA = "componentsList";
  private static final String CORRELATION_REPLACEMENT_COMBO_BOX_NAME_SUFFIX = 
      "-CorrelationReplacement-comboBox";
  private static final String CORRELATION_REGEX_COMBO_NAME = "Regex";
  private static final String CORRELATION_TEMPLATE_FRAME_NAME = "correlationTemplateFrame";
  private static final long TIMEOUT_MILLIS = 30000;
  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Mock
  private CorrelationProxyControl repositoryHandler;
  @Mock
  private CorrelationTemplate loadedTemplate;
  @Mock
  private Runnable modelUpdate;

  private FrameFixture frame;
  private RulesContainer rulesContainer;

  @Before
  public void setup() {
    rulesContainer = new RulesContainer(repositoryHandler, modelUpdate);
    frame = showInFrame(rulesContainer);
    frame.button(ADD_BUTTON).click();
    frame.button(ADD_BUTTON).click();
    verifySetup();
  }

  private void verifySetup() {
    pause(new Condition("setup ready") {
      @Override
      public boolean test() {
        return rulesContainer.getRules().size() == 2;
      }
    }, TIMEOUT_MILLIS);
  }

  @After
  public void tearDown() {
    frame.close();
    frame.cleanUp();
  }

  @Test
  public void shouldEnableUpButtonWhenAddMoreThanOneRow() {
    clickRuleByIndex(1);
    waitButtonIsEnabled(UP_BUTTON, true);
  }

  private void clickRuleByIndex(int ruleNumber) {
    RulePanel rule = rulesContainer.getRules().get(ruleNumber);
    frame.textBox(rule.getName() + REFERENCE_VARIABLE_SUFFIX).click();
  }

  private void waitButtonIsEnabled(String button_name, boolean enabled) {
    pause(new Condition("Down button enable") {
      @Override
      public boolean test() {
        return (enabled == frame.button(button_name).isEnabled());
      }
    }, TIMEOUT_MILLIS);
  }

  @Test
  public void shouldDownButtonNotBeenEnabledWhenSelectedRuleIsLast() {
    clickRuleByIndex(rulesContainer.getRules().size() - 1);
    waitButtonIsEnabled(DOWN_BUTTON, false);
  }

  @Test
  public void shouldEnableDownButtonWhenSelectFirstRuleAndPlaceToMove() {
    clickRuleByIndex(0);
    waitButtonIsEnabled(DOWN_BUTTON, true);
  }

  @Test
  public void shouldAddRulePanelWhenAddPressed() {
    frame.button(ADD_BUTTON).click();
    pause(new Condition("New rule added") {
      @Override
      public boolean test() {
        return rulesContainer.getRules().size() > 0;
      }
    }, TIMEOUT_MILLIS);
  }

  @Test
  public void shouldAddRuleWithAllFieldsWhenAddPressed() {
    RulePanel rulePanel = rulesContainer.getRules().get(0);
    String ruleId = rulePanel.getName();

    softly.assertThat(frame.textBox(ruleId + REFERENCE_VARIABLE_SUFFIX)).isNotNull();
    softly.assertThat(frame.comboBox(ruleId + "-CorrelationExtractor-comboBox")).isNotNull();
    softly.assertThat(frame.comboBox(ruleId + "-CorrelationReplacement-comboBox")).isNotNull();
  }

  @Test
  public void shouldMoveRuleUpWhenUpPressed() {
    RulePanel secondRule = rulesContainer.getRules().get(1);
    frame.textBox(secondRule.getReferenceVariableField().getName()).click();
    frame.button(UP_BUTTON).click();

    pause(new Condition("Move rule down when the button is pressed") {
      @Override
      public boolean test() {
        return rulesContainer.getRules().get(0).equals(secondRule);
      }
    }, TIMEOUT_MILLIS);
  }

  @Test
  public void shouldMoveRuleDownWhenDownPressed() {
    RulePanel firstRule = rulesContainer.getRules().get(0);
    clickRuleByIndex(0);
    frame.button(DOWN_BUTTON).click();

    pause(new Condition("The rule swipes") {
      @Override
      public boolean test() {
        return rulesContainer.getRules().get(1).equals(firstRule);
      }
    }, TIMEOUT_MILLIS);
  }

  @Test
  public void shouldDeleteRuleWhenDeletePressed() {
    clickRuleByIndex(1);
    int sizeBeforeDelete = rulesContainer.getRules().size();
    frame.button(DELETE_BUTTON).click();

    String description = "The rule is deleted";
    pause(new Condition(description) {
      @Override
      public boolean test() {
        return rulesContainer.getRules().size() < sizeBeforeDelete;
      }
    }, TIMEOUT_MILLIS);
  }

  @Test
  public void shouldUpButtonBeDisabledWhenMovedRuleReachTheTopOfTheList() {
    frame.button(ADD_BUTTON).click();
    clickRuleByIndex(rulesContainer.getRules().size() - 1);
    frame.button(UP_BUTTON).click();
    frame.button(UP_BUTTON).click();
    waitButtonIsEnabled(UP_BUTTON, false);
  }

  @Test
  public void shouldShowErrorMessageWhenClickSaveTemplateWithEmptyRule() {
    frame.button(EXPORT_BUTTON).click();
    requirePopUp("Rules are incomplete or empty, please fill them before continue saving.");
  }

  @Test
  public void shouldNotShowAdvanceOptionsWhenPanelIsCollapsed() {
    assertComponentIsNotPresent(EXTENSIONS_TEXT_AREA);
  }

  @Test
  public void shouldShowAdvanceOptionsWhenExpandPanel() {
    JPanelFixture panel = frame.panel(COLLAPSIBLE_PANE);
    toggleAdvancedSectionPanel(panel);
    assertThat(frame.label(RESPONSE_FILTER_LABEL).isEnabled()).isTrue();
  }

  private void toggleAdvancedSectionPanel(JPanelFixture panel) {
    frame.robot().click(panel.target(), new Point(0, 0));
  }

  @Test
  public void shouldNotShowExtensionsAreaWhenCollapsePanelAfterExpand()
      throws InterruptedException {
    JPanelFixture panel = frame.panel(COLLAPSIBLE_PANE);
    toggleAdvancedSectionPanel(panel);
    toggleAdvancedSectionPanel(panel);
    assertComponentIsNotPresent(EXTENSIONS_TEXT_AREA);
  }

  private void assertComponentIsNotPresent(String component) {
    boolean isPresent = false;
    try {
      frame.label(component);
    } catch (ComponentLookupException e) {
      isPresent = true;
    }
    assertThat(isPresent).isTrue();
  }

  private void requirePopUp(String requiredMessage) {
    frame.optionPane().requireMessage(requiredMessage);
  }

  @Test
  public void shouldAllowSaveTemplateFrameWhenSaveTemplateWithRulesWithoutExtractors() {
    selectReplacementComboByRuleIndex(0, CORRELATION_REGEX_COMBO_NAME);
    selectReplacementComboByRuleIndex(1, CORRELATION_REGEX_COMBO_NAME);
    frame.button(EXPORT_BUTTON).click();

    assertThat(frame.robot().finder().findByName(CORRELATION_TEMPLATE_FRAME_NAME).isVisible());
  }

  private void selectReplacementComboByRuleIndex(int ruleNumber, String item) {
    RulePanel rule = rulesContainer.getRules().get(ruleNumber);
    frame.comboBox(rule.getName() + CORRELATION_REPLACEMENT_COMBO_BOX_NAME_SUFFIX).selectItem(item);
  }

  @Test
  public void shouldClearRulesWhenClickClearButton() {
    frame.button(CLEAR_BUTTON).click();
    pause(new Condition("Clear the rules") {
      @Override
      public boolean test() {
        return rulesContainer.getRules().isEmpty();
      }
    }, TIMEOUT_MILLIS);
  }

  @Test
  public void shouldClearFilterWhenClickClearButton() {
    String responseFilter = "TestFilter";
    frame.textBox("responseFilterField").setText(responseFilter);
    frame.button(CLEAR_BUTTON).click();
    pause(new Condition("Clear the filters") {
      @Override
      public boolean test() {
        return rulesContainer.getResponseFilter().isEmpty();
      }
    }, TIMEOUT_MILLIS);
  }

  @Test
  public void shouldClearLastLoadedTemplateWhenClickClearButton() {
    rulesContainer.setLastLoadedTemplates(new HashSet<>(Collections.singletonList(loadedTemplate)));
    frame.button(CLEAR_BUTTON).click();
    pause(new Condition("Clear the last loaded templates") {
      @Override
      public boolean test() {
        return rulesContainer.getLoadedTemplates().isEmpty();
      }
    }, TIMEOUT_MILLIS);
  }

  @Test
  public void shouldClearComponentsWhenClickClearButton() {
    rulesContainer.setComponents("TestComponents");
    frame.button(CLEAR_BUTTON).click();
    pause(new Condition("Clear the last loaded templates") {
      @Override
      public boolean test() {
        return rulesContainer.getCorrelationComponents().isEmpty();
      }
    }, TIMEOUT_MILLIS);
  }
}
