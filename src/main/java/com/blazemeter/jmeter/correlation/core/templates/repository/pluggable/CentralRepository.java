package com.blazemeter.jmeter.correlation.core.templates.repository.pluggable;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.CENTRAL_REPOSITORY_NAME;

import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateVersions;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.repository.PluggableRepository;
import com.blazemeter.jmeter.correlation.core.templates.repository.RemoteRepository;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CentralRepository extends RemoteRepository implements PluggableRepository {
  //this value will change for real repository on SIP-170
  private static final String CENTRAL_REPOSITORY_URL = "https://raw.githubusercontent"
      + ".com/Blazemeter/CorrelationsRecorderTemplates/master/central/central-repository.json";

  @Override
  public String getName() {
    return CENTRAL_REPOSITORY_NAME;
  }

  @Override
  public String getEndPoint() {
    return CENTRAL_REPOSITORY_URL;
  }

  @Override
  public boolean autoLoad() {
    return true;
  }

  @Override
  public boolean disableConfig() {
    return true;
  }

  @Override
  public CorrelationTemplatesRepository getRepository() {
    return null;
  }

  @Override
  public Map<String, CorrelationTemplateVersions> getTemplateVersions() {
    return null;
  }

  @Override
  public List<Template> getTemplates(List<TemplateVersion> filter) {
    return null;
  }

  @Override
  public Map<Template, TemplateProperties> getTemplatesAndProperties() {
    return new HashMap<>();
  }

  @Override
  public Map<Template, TemplateProperties> getTemplatesAndProperties(List<TemplateVersion> filter) {
    return new HashMap<>();
  }

  private static Map<String, String> getDefaultProperties() {
    Map<String, String> props = new HashMap<>();
    props.put(RepositoryGeneralConst.TEMPLATE_PROPERTY_DISALLOW_TO_USE, "false");
    props.put(RepositoryGeneralConst.TEMPLATE_PROPERTY_NOT_ALLOW_EXPORT, "false");
    return props;
  }
}
