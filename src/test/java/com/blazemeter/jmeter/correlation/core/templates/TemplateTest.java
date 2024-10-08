
package com.blazemeter.jmeter.correlation.core.templates;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;

import com.blazemeter.jmeter.correlation.core.templates.Template.Builder;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class TemplateTest {

  private static final String BUILD_DESCRIPTION = "Description test";
  private static final String BUILD_COMPONENT = "Component";
  private static final String BUILD_RESPONSE_FILTERS = "responseFilters";
  private static final String BUILD_REPOSITORY_ID = "repositoryId";
  private static final String BUILD_CHANGES = "changes";
  private static final String BUILD_AUTHOR = "Author";
  private static final String BUILD_URL = "URL";
  private static final String TEMPLATE_TYPE = "correlation";
  private static final String CUSTOM_TEMPLATE_REFERENCE_ID = "custom";
  private static final String TEMPLATE_VERSION_ONE = "1.0";


  @Test
  public void shouldReturnTrueWhenBuildersAreTheSameObject() {
    Template.Builder builder = new Template.Builder();
    Builder modifiedBuilder = builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE);
    assertThat(modifiedBuilder).isEqualTo(builder);
  }

  @Test
  public void shouldReturnFalseWhenBuildersIsCompareWithObjectOfDifferentClass() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID).withVersion(TEMPLATE_VERSION_ONE);
    assertThat(builder).isNotEqualTo("aa");
  }

  @Test
  public void shouldReturnAStringWhenToString() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    String expectedString = "Builder{"
        + "description='Description test', "
        + "id='custom', "
        + "author='Author', "
        + "url='URL', "
        + "rules=[], "
        + "responseFilters='responseFilters', "
        + "components='Component', "
        + "version='1.0', "
        + "repositoryId='repositoryId', "
        + "changes='changes'}";
    assertEquals(builder.toString(), expectedString);
  }

  @Test
  public void shouldReturnTrueWhenEqualsWithEqualObjects() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertThat(builder).isEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffUrl() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl("BUILD_URL");
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffAuthor() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor("BUILD_AUTHOR")
        .withUrl(BUILD_URL);
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffDependencies() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(null)
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffChanges() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges("BUILD_CHANGES")
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffVersion() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)
        .withVersion("BUILD_VERSION")
        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffRepoId() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId("BUILD_REPOSITORY_ID")

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffFilters() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters("BUILD_RESPONSE_FILTERS")
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffComponent() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents("BUILD_COMPONENT")
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffGroups() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(null)
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffDescription() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription("BUILD_DESCRIPTION")
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)

        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnFalseWhenEqualsWithDiffName() {
    Template.Builder builder = new Template.Builder();
    builder.withId("CUSTOM_TEMPLATE_REFERENCE_NAME")
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)
        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template.Builder builder2 = new Template.Builder();
    builder2.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)
        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertThat(builder).isNotEqualTo(builder2);
  }

  @Test
  public void shouldReturnTemplateVersionWhenToTemplateVersion() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)
        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    TemplateVersion templateVersion = builder.build().toTemplateVersion();
    assertEquals(CUSTOM_TEMPLATE_REFERENCE_ID, templateVersion.getName());
    assertEquals(TEMPLATE_VERSION_ONE, templateVersion.getVersion());
  }

  @Test
  public void shouldReturnDependenciesWhenGetDependencies() {
    List<CorrelationTemplateDependency> expectedDependencies = new ArrayList<>();
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)
        .withChanges(BUILD_CHANGES)
        .withDependencies(expectedDependencies)
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertEquals(expectedDependencies, builder.build().getDependencies());
  }

  @Test
  public void shouldReturnTypeWhenGetType() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)
        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertEquals(TEMPLATE_TYPE, builder.build().getType());
  }

  @Test
  public void shouldReturnChangesWhenGetChanges() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)
        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    assertEquals(BUILD_CHANGES, builder.build().getChanges());
  }

  @Test
  public void shouldSetTypeWhenSetType() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)
        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    Template template = builder.build();
    template.setType(TEMPLATE_TYPE + "Edited");
    assertEquals(TEMPLATE_TYPE + "Edited", template.getType());
  }

  @Test
  public void shouldReturnStringWhenToString() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withGroups(new ArrayList<>())
        .withSnapshot(new BufferedImage(300, 300, TYPE_INT_RGB))
        .withComponents(BUILD_COMPONENT)
        .withResponseFilters(BUILD_RESPONSE_FILTERS)
        .withRepositoryId(BUILD_REPOSITORY_ID)
        .withChanges(BUILD_CHANGES)
        .withDependencies(new ArrayList<>())
        .withAuthor(BUILD_AUTHOR)
        .withUrl(BUILD_URL);
    String expectedString = "CorrelationTemplate{id='custom', " +
        "description='Description test', " +
        "version='1.0', " +
        "author='Author', " +
        "url='URL', " +
        "components='Component', " +
        "responseFilters='responseFilters', " +
        "groups=[], " +
        "dependencies=[], " +
        "repositoryId='repositoryId', " +
        "changes='changes', " +
        "snapshotPath='null', " +
        "isInstalled=false}";
    assertEquals(expectedString, builder.build().toString());
  }

  @Test
  public void shouldReturnHashCodeWhenHasHash() {
    Template.Builder builder = new Template.Builder();
    builder.withId(CUSTOM_TEMPLATE_REFERENCE_ID)
        .withVersion(TEMPLATE_VERSION_ONE)
        .withDescription(BUILD_DESCRIPTION)
        .withUrl(BUILD_URL);
    int expectedHash = 730345591;
    assertEquals(expectedHash, builder.hashCode());
  }

}
