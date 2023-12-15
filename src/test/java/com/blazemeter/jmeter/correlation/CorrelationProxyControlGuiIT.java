package com.blazemeter.jmeter.correlation;

import static org.assertj.swing.fixture.Containers.showInFrame;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.RulesContainer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import us.abstracta.jmeter.javadsl.core.engines.JmeterEnvironment;

@RunWith(SwingTestRunner.class)
public class CorrelationProxyControlGuiIT {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Mock
  private RulesContainer container;
  private CorrelationProxyControl model;
  private CorrelationProxyControl modelUpdated;
  private CorrelationProxyControlGui gui;
  private FrameFixture frame;

  @Before
  public void setUp() throws IOException {
    JmeterEnvironment env = new JmeterEnvironment();
    List<CorrelationRule> baseRules = Collections.singletonList(
        new CorrelationRule("refVar1", new RegexCorrelationExtractor<>(),
            new RegexCorrelationReplacement<>()));
    RulesGroup.Builder builder = new RulesGroup.Builder();
    builder.withRules(baseRules);
    model = new CorrelationProxyControlBuilder()
        .withLocalConfigurationPath(tempFolder.getRoot().getPath())
        .build();
    model.setCorrelationGroups(Collections.singletonList(builder.build()));
    gui = new CorrelationProxyControlGui(model, container);
    frame = showInFrame(gui);
  }

  @After
  public void tearDown() {
    if (frame != null) {
      frame.cleanUp();
    }
  }

  @Test
  public void shouldUpdateModelWhenConfigureWithNewModel() {
    CorrelationRule firstRule = new CorrelationRule("refVar1", null, null);
    CorrelationRule secondRule = new CorrelationRule("refVar1", new RegexCorrelationExtractor<>(),
        null);
    List<CorrelationRule> rules = Arrays.asList(firstRule, secondRule);
    List<RulesGroup> groups = Collections
        .singletonList(new RulesGroup.Builder().withRules(rules).build());
    modelUpdated = new CorrelationProxyControlBuilder()
        .withLocalConfigurationPath(tempFolder.getRoot().getPath())
        .build();
    modelUpdated.setCorrelationGroups(groups);

    gui.configure(modelUpdated);

    softly.assertThat(gui.getCorrelationProxyControl()).isEqualTo(modelUpdated);
    softly.assertThat(gui.getCorrelationProxyControl().getGroups()).isEqualTo(groups);
  }
}
