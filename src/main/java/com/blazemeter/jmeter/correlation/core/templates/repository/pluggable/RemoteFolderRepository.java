package com.blazemeter.jmeter.correlation.core.templates.repository.pluggable;

import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateVersions;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.repository.FileRepository;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteFolderRepository extends FileRepository {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteFolderRepository.class);
  private String name;
  private String url;

  public RemoteFolderRepository(String name, String url) {
    this.name = name;
    this.url = url;
  }

  @Override
  public void init() {
    super.init();
  }

  @Override
  public boolean autoLoad() {
    return false;
  }

  @Override
  public boolean disableConfig() {
    return false;
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
  public Collection<String> checkRepositoryURL(String url) {
    List<String> errors = new ArrayList<>();
    errors.addAll(super.checkRepositoryURL(url));
    if (errors.size() > 0) {
      return errors;
    }
    try {
      URL parsedURL = new URL(url.replace(" ", "/%20"));
      URLConnection uc = parsedURL.openConnection();
      if (!(uc instanceof HttpURLConnection) &&
          (!Files.exists(Paths.get(url.replace("file://", ""))))) {
        String error = "File doesn't exist";
        LOG.warn("There was an error trying to reach the repository {}'s Path={}. Error: {}",
            this.getName(), url, error);
        errors.add(
            "- We couldn't reach " + this.getName() + "'s Path " + url + ".\n   Error: "
                + error);
      }
    } catch (IOException e) {
      if (Files.exists(Paths.get(url))) {
        LOG.warn("There was and error parsing the repository {}'s Path={}.", this.getName(),
            url, e);
        errors.add(
            "- We couldn't parse " + this.getName() + "'s Path " + url + ".\n"
                + "   Path should start with protocol 'file://'\n"
                + "   Error: " + e.getMessage());
      } else {
        LOG.warn("There was and error parsing the repository {}'s URL={}.", this.getName(),
            url, e);
        errors.add(
            "- We couldn't parse " + this.getName() + "'s url " + url + ".\n"
                + "   Error: " + e.getMessage());
      }
    }
    return errors;
  }

  @Override
  public Map<Template, TemplateProperties> getTemplatesAndProperties() {
    return null;
  }

  @Override
  public Map<Template, TemplateProperties> getTemplatesAndProperties(List<TemplateVersion> filter) {
    return null;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getEndPoint() {
    return url;
  }

}
