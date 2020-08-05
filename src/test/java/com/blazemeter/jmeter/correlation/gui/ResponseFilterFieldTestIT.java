package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.assertj.swing.timing.Pause.pause;

import java.awt.Dimension;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.apache.logging.log4j.util.Strings;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResponseFilterFieldTestIT {

  private static final long TIMEOUT_MILLIS = 10000;
  private static final String TEST_BUTTON = "testButton";
  private static final String firstFilter = "Filter 1";
  private static final String secondFilter = "Filter 2";
  private FrameFixture frame;
  private ResponseFilterField responseFilterField;

  @Before
  public void setUp() {
    //No need to test the interface
    responseFilterField = new ResponseFilterField("responseFilterField", new Dimension(200, 50),
        notificationText -> {
        });
    JButton clickMe = new JButton("Click me!");
    clickMe.setName(TEST_BUTTON);

    JPanel container = new JPanel();
    container.add(responseFilterField);
    container.add(clickMe);

    frame = showInFrame(container);
    frame.resizeTo(new Dimension(300, 200));
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldRemoveRepeatedWhenFocusIsLost() {
    responseFilterField
        .setText(Strings.join(Arrays.asList(firstFilter, secondFilter, secondFilter), ','));

    frame.button(TEST_BUTTON).click();
    String expectedFilters = buildExpectedFilters();
    pause(new Condition("Remove the repeated filters on loose focus") {
      @Override
      public boolean test() {
        return responseFilterField.getText().equals(expectedFilters);
      }
    }, TIMEOUT_MILLIS);
  }

  private String buildExpectedFilters() {
    return String.join(", ", Arrays.asList(secondFilter, firstFilter));
  }

  @Test
  public void shouldRemoveEmptyFiltersWhenFocusLost() {
    String spacedFilter = "   ";
    responseFilterField
        .setText(Strings.join(Arrays.asList(firstFilter, spacedFilter, secondFilter), ','));
    frame.button(TEST_BUTTON).click();
    String expectedFilters = buildExpectedFilters();
    pause(new Condition("Remove the repeated filters on loose focus") {
      @Override
      public boolean test() {
        return responseFilterField.getText().equals(expectedFilters);
      }
    }, TIMEOUT_MILLIS);
  }

}