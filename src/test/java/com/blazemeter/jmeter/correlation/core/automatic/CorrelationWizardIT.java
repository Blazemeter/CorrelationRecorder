package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateVersions;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.gui.automatic.CorrelationWizard;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

//@RunWith(SwingTestRunner.class)
public class CorrelationWizardIT {

  private CorrelationWizard wizard;

  @Mock
  private CorrelationHistory history;

  @Before
  public void setUp() throws Exception {
    JMeterTestUtils.setupJmeterEnv();
    history = new CorrelationHistory();
    wizard = new CorrelationWizard();
    wizard.setHistory(history);
    wizard.setVersionsSupplier(() -> new ArrayList<>());
    wizard.setAddRuleConsumer((rule) -> {
    });
    wizard.init();

    String path = "/Users/abstracta/testing/jmeter-siebel-plugin/jmeters/apache-jmeter-5.5/";
    LocalConfiguration configuration = new LocalConfiguration(path);
    CorrelationTemplatesRepositoriesConfiguration config =
        new CorrelationTemplatesRepositoriesConfiguration(configuration);
    wizard.setRepositoriesConfiguration(config);
    wizard.setPreferredSize(new Dimension(800, 600));
    wizard.setVisible(true);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void shouldDisplaySelectTemplate() throws InterruptedException {
    System.out.println("Hello World!");
    wizard.displayTemplateSelection("");
    wizard.pack();
    wizard.setVisible(true);
    Thread.sleep(10000);
    System.out.println("Bye World!");
  }

  public static void main(String[] args) {
    CorrelationWizard wizard;
    CorrelationHistory history;

    JMeterTestUtils.setupJmeterEnv();
    String path = "/Users/abstracta/testing/jmeter-siebel-plugin/jmeters/apache-jmeter-5.5/";
    LocalConfiguration configuration = new LocalConfiguration(path, true);
    configuration.setupRepositoryManagers();
    /*
    *
    *     LocalConfiguration localConfiguration = new LocalConfiguration(folder.getRoot().getPath(), true);
    localConfiguration.setupRepositoryManagers();
    local = new LocalCorrelationTemplatesRepositoriesRegistry(localConfiguration);
    String localRepository = Paths.get(new File(getClass().getResource("/").getFile()).toPath().
        toAbsolutePath().toString(), BASE_REPOSITORY_NAME).toAbsolutePath().toString();

    local.save(EXTERNAL_REPOSITORY_NAME, localRepository);
    prepareExpectedLocalRepository();
    * */

    List<CorrelationTemplatesRepository> repositories = configuration.getRepositories();
    for (CorrelationTemplatesRepository repository : repositories) {
      System.out.println(repository.getName());
      RepositoryManager manager = configuration.getRepositoryManager(repository.getName());
      Map<String, CorrelationTemplateVersions> versions = manager.getTemplateVersions();
    }

    history = new CorrelationHistory();
    wizard = new CorrelationWizard();
    wizard.setHistory(history);
    wizard.setVersionsSupplier(() -> new ArrayList<>());
    wizard.setAddRuleConsumer((rule) -> {
    });
    wizard.init();


    CorrelationTemplatesRepositoriesConfiguration config =
        new CorrelationTemplatesRepositoriesConfiguration(configuration);
    wizard.setRepositoriesConfiguration(config);
    wizard.setPreferredSize(new Dimension(800, 600));
    wizard.displayTemplateSelection("");
    wizard.pack();
    wizard.setVisible(true);

    System.out.println("Hello World!");
    System.out.println("Bye World!");
  }
}
