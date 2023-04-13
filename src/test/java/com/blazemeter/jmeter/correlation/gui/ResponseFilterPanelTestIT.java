package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.swing.fixture.Containers.showInFrame;

import com.blazemeter.jmeter.commons.SwingUtils;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.apache.logging.log4j.util.Strings;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SwingTestRunner.class)
public class ResponseFilterPanelTestIT {

  private static final String TEST_BUTTON = "testButton";
  private static final String firstFilter = "Filter 1";
  private static final String secondFilter = "Filter 2";
  private FrameFixture frame;
  private JTextComponentFixture responseFilterField;

  @Before
  public void setUp() {
    JPanel container = new JPanel();
    container.add(new ResponseFilterPanel());
    container.add(SwingUtils.createComponent(TEST_BUTTON, new JButton("Click me!")));
    frame = showInFrame(container);
    responseFilterField = frame.textBox("responseFilterField");
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldRemoveRepeatedWhenFocusIsLost() {
    responseFilterField
        .setText(Strings.join(Arrays.asList(firstFilter, secondFilter, secondFilter), ','));
    findTestButton().focus();
    responseFilterField.requireText(buildExpectedFilters());
  }

  private String buildExpectedFilters() {
    return String.join(", ", Arrays.asList(secondFilter, firstFilter));
  }

  private JButtonFixture findTestButton() {
    return frame.button(TEST_BUTTON);
  }

  @Test
  public void shouldRemoveEmptyFiltersWhenFocusLost() {
    responseFilterField
        .setText(Strings.join(Arrays.asList(firstFilter, "   ", secondFilter), ','));
    findTestButton().focus();
    responseFilterField.requireText(buildExpectedFilters());
  }

}