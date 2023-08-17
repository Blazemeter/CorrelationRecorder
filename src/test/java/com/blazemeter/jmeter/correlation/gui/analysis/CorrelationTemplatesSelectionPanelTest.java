package com.blazemeter.jmeter.correlation.gui.analysis;

import static org.assertj.swing.fixture.Containers.showInFrame;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SwingTestRunner.class)
public class CorrelationTemplatesSelectionPanelTest {

  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private FrameFixture frame;
  private CorrelationTemplatesSelectionPanel panel;

  @Before
  public void setUp() throws Exception {
    //TODO: Add the wizard with a method that provides the list of templates
    panel = new CorrelationTemplatesSelectionPanel(null);
    frame = showInFrame(panel);
  }

  @After
  public void tearDown() throws Exception {
    frame.cleanUp();
    frame = null;
  }

  @Test
  public void shouldDisplayCorrelationTemplates() throws InterruptedException {
    Thread.sleep(100);
  }

  // This method creates a frame and add the panel to it.
  public static void main(String[] args) {
    //TODO: Add the wizard with a method that provides the list of templates
    CorrelationTemplatesSelectionPanel panel = new CorrelationTemplatesSelectionPanel(null);
    JFrame frame = new JFrame();
    frame.add(panel);
    frame.pack();
    frame.setVisible(true);
  }

  public static List<Template> dummyVersions() {
    Template.Builder builder = new Template.Builder()
        .withRepositoryId("central")
        .withDescription("This is a dummy template made for testing purposes")
        .withGroups(new ArrayList<>())
        .withAuthor("BlazeMeter");
    List<Template> versions = new ArrayList<>();
    versions.addAll(generateVersionsForProtocol("wordpress", builder, true));
    versions.addAll(generateVersionsForProtocol("magento", builder, false));

    return versions;
  }

  public static List<Template> generateVersionsForProtocol(String protocol,
                                                           Template.Builder builder,
                                                           boolean installed) {
    List<Template> versions = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      Template version = builder.withVersion("1.0." + i)
          .withChanges("Changes for the version 1.0." + i)
          .withId(protocol)
          .withDependencies(generateDependenciesForProtocol(protocol, i + 1))
          .build();
      version.setInstalled(installed);
      versions.add(version);
    }
    return versions;
  }

  public static List<CorrelationTemplateDependency> generateDependenciesForProtocol(String protocol,
                                                                                    int numDependencies) {
    List<CorrelationTemplateDependency> dependencies = new ArrayList<>();
    for (int i = 0; i < numDependencies; i++) {
      //String name, String version, String url
      dependencies.add(new CorrelationTemplateDependency("Dep " + i, "1.0." + i, "url" + i));
    }

    return dependencies;
  }
}