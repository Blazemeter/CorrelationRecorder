package com.blazemeter.jmeter.correlation.gui.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.apache.jmeter.util.JMeterUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RecommendedJMeterPropertiesTest {

  private static final String REDIRECT = "proxy.redirect.disabling";
  private static final String DEFLATE = "httpclient4.deflate_relax_mode";

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setup() throws IOException {
    JMeterTestUtils.setupUpdatedJMeter();
    removeProperties();
  }

  @After
  public void tearDown() {
    removeProperties();
  }

  private static void removeProperties() {
    JMeterUtils.getJMeterProperties().remove(REDIRECT);
    JMeterUtils.getJMeterProperties().remove(DEFLATE);
  }

  private static JMeterPropertyRequirement requirement(String name) {
    return RecommendedJMeterProperties.REQUIREMENTS.stream()
        .filter(r -> r.getName().equals(name)).findFirst()
        .orElseThrow(() -> new AssertionError("No requirement for " + name));
  }

  @Test
  public void shouldNotBeMetWhenUnsetBecauseJMeterDefaultsDiffer() {
    assertThat(RecommendedJMeterProperties.unmet()).hasSize(2);
    assertThat(requirement(REDIRECT).describeEffectiveValue()).isEqualTo("true (JMeter's default)");
    assertThat(requirement(DEFLATE).describeEffectiveValue()).isEqualTo("false (JMeter's default)");
  }

  @Test
  public void shouldBeMetWhenSetToTheExpectedValueInAnyCase() {
    JMeterUtils.setProperty(REDIRECT, "FALSE");
    JMeterUtils.setProperty(DEFLATE, " True ");
    assertThat(RecommendedJMeterProperties.unmet()).isEmpty();
  }

  @Test
  public void shouldNotBeMetWhenSetToAnotherValue() {
    JMeterUtils.setProperty(REDIRECT, "true");
    JMeterUtils.setProperty(DEFLATE, "true");
    assertThat(RecommendedJMeterProperties.unmet()).extracting(JMeterPropertyRequirement::getName)
        .containsExactly(REDIRECT);
    assertThat(requirement(REDIRECT).describeEffectiveValue()).isEqualTo("true");
  }

  @Test
  public void shouldReplaceThePreviousValuesAndKeepTheOtherLines() throws IOException {
    File file = folder.newFile("user.properties");
    write(file,
        "# my settings",
        "#proxy.redirect.disabling=true",
        "proxy.redirect.disabling = true",
        "proxy.redirect.disabling.other=1",
        "httpclient4.deflate_relax_mode:false\\",
        "    continued",
        "other.key=value");

    RecommendedJMeterProperties.persist(file, RecommendedJMeterProperties.REQUIREMENTS);

    assertThat(read(file)).containsExactly(
        "# my settings",
        "#proxy.redirect.disabling=true",
        "proxy.redirect.disabling.other=1",
        "other.key=value",
        "",
        "# Set by BlazeMeter Auto Correlation Recorder"
            + " (Tools > BlazeMeter Auto Correlation Recorder > JMeter Properties)",
        "proxy.redirect.disabling=false",
        "httpclient4.deflate_relax_mode=true");
  }

  @Test
  public void shouldLeaveTheFileTheSameWhenAppliedTwice() throws IOException {
    File file = folder.newFile("user.properties");
    write(file, "other.key=value");
    RecommendedJMeterProperties.persist(file, RecommendedJMeterProperties.REQUIREMENTS);
    List<String> once = read(file);

    RecommendedJMeterProperties.persist(file, RecommendedJMeterProperties.REQUIREMENTS);

    assertThat(read(file)).isEqualTo(once);
  }

  @Test
  public void shouldCreateTheFileWhenItDoesNotExist() throws IOException {
    File file = new File(folder.getRoot(), "bin/user.properties");

    RecommendedJMeterProperties.persist(file, RecommendedJMeterProperties.REQUIREMENTS);

    assertThat(read(file)).contains("proxy.redirect.disabling=false",
        "httpclient4.deflate_relax_mode=true");
  }

  @Test
  public void shouldSetTheExpectedValuesInTheRunningJMeter() {
    RecommendedJMeterProperties.applyToRuntime(RecommendedJMeterProperties.REQUIREMENTS);
    assertThat(RecommendedJMeterProperties.unmet()).isEmpty();
  }

  private static void write(File file, String... lines) throws IOException {
    Files.write(file.toPath(), Arrays.asList(lines), StandardCharsets.ISO_8859_1);
  }

  private static List<String> read(File file) throws IOException {
    return Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
  }
}
