package com.blazemeter.jmeter.correlation;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.RulesContainer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationProxyControlGuiIT {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  
  @Mock
  private RulesContainer container;
  
  CorrelationProxyControl model;
  CorrelationProxyControl modelUpdated;
  
  private CorrelationProxyControlGui gui;

  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private static final List<CorrelationRule> baseRules = Collections.singletonList(
      new CorrelationRule("refVar1", new RegexCorrelationExtractor<>(),
          new RegexCorrelationReplacement<>()));

  @Before
  public void setUp() {
    model = new CorrelationProxyControl(tempFolder.getRoot().getPath());
    model.setCorrelationRules(baseRules);
    gui = new CorrelationProxyControlGui(model, container);
  }

  @Test
  public void shouldUpdateModelWhenConfigureWithNewModel() {
    CorrelationRule firstRule = new CorrelationRule("refVar1", null, null);
    CorrelationRule secondRule = new CorrelationRule("refVar1", new RegexCorrelationExtractor<>(),
        null);
    List<CorrelationRule> rules = Arrays.asList(firstRule, secondRule);
   
    modelUpdated  = new CorrelationProxyControl(tempFolder.getRoot().getPath());
    modelUpdated.setCorrelationRules(rules);
    
    gui.configure(modelUpdated);
    
    softly.assertThat(gui.getCorrelationProxyControl()).isEqualTo(modelUpdated);
    softly.assertThat(gui.getCorrelationProxyControl().getRules()).isEqualTo(rules);
  }
}