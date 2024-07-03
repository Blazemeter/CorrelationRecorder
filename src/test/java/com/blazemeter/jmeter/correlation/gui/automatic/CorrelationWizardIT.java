package com.blazemeter.jmeter.correlation.gui.automatic;

import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.ArgumentMatchers.any;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistory;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.Repository;

import com.blazemeter.jmeter.correlation.core.templates.Template.Builder;
import java.awt.Container;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;


@RunWith(SwingTestRunner.class)
public class CorrelationWizardIT {

  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private FrameFixture frame;
  private CorrelationWizard wizard;
  @Mock
  public Function<Builder, Template> buildProvider;

  @Before
  public void setUp() throws Exception {
    wizard = new CorrelationWizard();
    wizard.setVersionsSupplier(() ->
        generateTemplatesWithVersions(Arrays.asList("id1", "id2", "id3"), "repId1"));
    CorrelationHistory.setSaveCurrentTestPlan(() -> "Recording"+ File.separator+"test");
    CorrelationHistory history = new CorrelationHistory();
    wizard.setHistory(history);
    wizard.setRepositoriesSupplier(CorrelationWizardIT::getMockedRepository);
    Mockito.when(buildProvider.apply(any())).thenReturn(new Template());
    wizard.setBuildTemplateProvider(buildProvider);
    wizard.init();
    Container contentPane = wizard.getContentPane();
    wizard.displayTemplateSelection("Test");
    frame = showInFrame(contentPane);
  }

  private static Map<String, Repository> getMockedRepository() {
    Map<String, Repository> templates = new HashMap<>();
    templates.put("repId1", new Repository("repId1"));
    return templates;
  }

  private List<Template> generateTemplatesWithVersions(List<String> strings, String repoId) {
    List<Template> versions = new ArrayList<>();
    strings.forEach(id -> {
      versions.add(generateVersion("1.0", id, repoId));
      versions.add(generateVersion("2.0", id, repoId));
    });
    return versions;
  }

  private Template generateVersion(String version, String id, String repositoryId) {
    return new Template.Builder().withDescription("description").withId(id)
        .withVersion(version).withAuthor("author").withUrl("url")
        .withComponents("components").withResponseFilters("responseFilters")
        .withChanges("changes").withSnapshot(null).withGroups(new ArrayList<>())
        .withRepositoryId(repositoryId).withDependencies(new ArrayList<>()).build();
  }

  @After
  public void tearDown() throws Exception {
    if (frame != null) {
      frame.cleanUp();
      frame = null;
    }
  }

  @Test
  public void shouldExportSelectedTemplateVersionsWhenContinueClicked()
      throws InterruptedException {
    wizard.getTemplateSelectionPanel().loadCorrelationTemplates();
    wizard.logStep("shouldDisplayCorrelationTemplates");
    List<Template> selectedVersions = new ArrayList<>();
    int initialSize = 0;
    AtomicReference<String> jtlTracePath = new AtomicReference<>();
    wizard.getTemplateSelectionPanel()
        .setStartNonCorrelatedAnalysis((selectedTemplates, jtlTrace) -> {
          selectedVersions.addAll(selectedTemplates);
          jtlTracePath.set(jtlTrace);
        });

    templateSelectionPressContinue();
    softly.assertThat(jtlTracePath.get()).isNotNull().as("JTL Trace Path");
    softly.assertThat(selectedVersions.size()).isEqualTo(2).as("Selected versions");
    softly.assertThat(selectedVersions.size()).isNotEqualTo(initialSize)
        .as("Updated selected versions");
  }

  private void templateSelectionPressContinue() {
    frame.button("templateContinueButtonButton").click();
  }

  @Test
  public void shouldLoadTemplateVersionsWhenReloadClicked() {
    wizard.getTemplateSelectionPanel().loadCorrelationTemplates();
    List<Template> selectedVersions = new ArrayList<>();
    int initialSize = 0;
    AtomicReference<String> jtlTracePath = new AtomicReference<>();
    wizard.getTemplateSelectionPanel()
        .setStartNonCorrelatedAnalysis((selectedTemplates, jtlTrace) -> {
          selectedVersions.addAll(selectedTemplates);
          jtlTracePath.set(jtlTrace);
        });
    templateSelectionPressReloadTemplates();
    templateSelectionPressContinue();

    List<Template> versions =
        generateTemplatesWithVersions(Collections.singletonList("id1"), "repoId2");
    wizard.setVersionsSupplier(() -> versions);
    templateSelectionPressReloadTemplates();

    softly.assertThat(jtlTracePath.get()).isNotNull().as("JTL Trace Path");
    softly.assertThat(selectedVersions.size()).isEqualTo(2).as("Selected versions");
    softly.assertThat(selectedVersions.size()).isNotEqualTo(initialSize)
        .as("Updated selected versions");
  }

  private void templateSelectionPressReloadTemplates() {
    frame.button("templateReloadButtonButton").click();
  }
}
