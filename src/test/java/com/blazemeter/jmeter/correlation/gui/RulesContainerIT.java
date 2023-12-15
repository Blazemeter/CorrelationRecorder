package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.gui.templates.TemplateSaveFrame;
import com.blazemeter.jmeter.correlation.gui.templates.TemplatesManagerFrame;
import com.blazemeter.jmeter.correlation.siebel.SiebelCounterCorrelationReplacement;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JTextField;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(SwingTestRunner.class)
public class RulesContainerIT {

  private static final Color DISABLE_FONT_RULE_COLOR = new JTextField().getDisabledTextColor();
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Mock
  private CorrelationProxyControl model;
  @Mock
  private Runnable modelUpdate;
  @Mock
  private TemplateSaveFrame templateFrame;
  @Mock
  private TemplatesManagerFrame loadFrame;
  private FrameFixture frame;
  private RulesContainer rulesContainer;
  private JTableFixture rulesTable;

  @Before
  public void setup() {
    rulesContainer = new RulesContainer(model, modelUpdate);
    rulesContainer.setTemplateFrame(templateFrame);
    rulesContainer.setLoadFrame(loadFrame);
    frame = showInFrame(rulesContainer);
    rulesTable = frame.table("groupsTable");
    rulesTable.replaceCellWriter(new CustomCellWriter(frame.robot()));
  }

  @After
  public void tearDown() {
    frame.cleanUp();
    frame = null;
  }

  @Test
  public void shouldClearRulesWhenClickClearButton() {
    addGroup();
    clickClear();
    assertThat(rulesContainer.getRulesGroups()).asList().isEmpty();
  }

  private void addGroup() {
    frame.button("addButton").click();
  }

  private void clickClear() {
    frame.button("clearButton").click();
  }

  @Test
  public void shouldClearFilterWhenClickClearButton() {
    frame.textBox("responseFilterField").setText("TestFilter");
    clickClear();
    assertThat(rulesContainer.getResponseFilter()).isEmpty();
  }

  @Test
  public void shouldClearLastLoadedTemplateWhenClickClearButton() {
    Template template = Mockito.mock(Template.class);
    rulesContainer.updateLoadedTemplate(template);
    clickClear();
    saveTemplate();
    verify(templateFrame, never()).updateLastLoadedTemplate(any());
  }

  private void saveTemplate() {
    frame.button("exportButton").click();
  }

  @Test
  public void shouldNotSetRulesWhenConfigureWithNullTestElement() {
    prepareRepositoryHandler("", "", null);
    rulesContainer.configure(model);
    assertThat(rulesContainer.getRulesGroups())
        .isEqualTo(Collections.EMPTY_LIST);
  }

  private void prepareRepositoryHandler(String correlationComponents, String responseFilters,
                                        List<RulesGroup> groups) {
    when(model.getCorrelationComponents()).thenReturn(correlationComponents);
    when(model.getResponseFilter()).thenReturn(responseFilters);
    when(model.getGroups()).thenReturn(groups);
  }

  @Test
  public void shouldSetRulesWhenConfigure() {
    List<CorrelationRule> rules = Collections.singletonList(new CorrelationRule("refVar1",
        new RegexCorrelationExtractor<>(), new RegexCorrelationReplacement<>()));
    when(model.getGroups())
        .thenReturn(Collections.singletonList(new RulesGroup.Builder().withRules(rules).build()));
    when(model.getCorrelationComponents()).thenReturn("");
    when(model.getResponseFilter()).thenReturn("");

    rulesContainer.configure(model);
    assertThat(rulesContainer.getRulesGroups().get(0).getRules()).isEqualTo(rules);
  }

  @Test
  public void shouldUpdateValuesWhenConfigureWithDifferentModel() {
    CorrelationRule firstRule = new CorrelationRule("refVar1",
        new RegexCorrelationExtractor<>(), new RegexCorrelationReplacement<>());
    List<CorrelationRule> rules = Collections.singletonList(firstRule);
    RulesGroup.Builder builder = new RulesGroup.Builder();
    builder.withRules(rules);
    when(model.getGroups()).thenReturn(Collections.singletonList(builder.build()));
    when(model.getCorrelationComponents()).thenReturn("");
    when(model.getResponseFilter()).thenReturn("");

    rulesContainer.configure(model);

    CorrelationRule secondRule = new CorrelationRule("refVar2", null,
        new SiebelCounterCorrelationReplacement());
    secondRule.setEnabled(false);
    List<CorrelationRule> expectedRules = Arrays.asList(firstRule, secondRule);
    builder.withRules(expectedRules);
    when(model.getGroups()).thenReturn(Collections.singletonList(builder.build()));
    String expectedComponents = SiebelCounterCorrelationReplacement.class.getCanonicalName();
    when(model.getCorrelationComponents()).thenReturn(expectedComponents);
    String expectedFilter = "someText";
    when(model.getResponseFilter()).thenReturn(expectedFilter);
    rulesContainer.configure(model);

    softly.assertThat(rulesContainer.getRulesGroups().get(0).getRules()).isEqualTo(expectedRules);
    softly.assertThat(rulesContainer.getResponseFilter()).isEqualTo(expectedFilter);
    softly.assertThat(rulesContainer.getCorrelationComponents()).isEqualTo(expectedComponents);
  }
}
