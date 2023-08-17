package com.blazemeter.jmeter.correlation.core.templates.repository;

import com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRepositoriesRegistry;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class FileRepository extends RepositoryManager {

  @Override
  public void init() {
    setTemplateRegistry(new LocalCorrelationTemplatesRepositoriesRegistry(this.getConfig()));
  }

  @Override
  public String getEndPoint() {
    return Paths.get(
        this.getConfig().getCorrelationsTemplateInstallationFolder(), getRepositoryFileName()
    ).toAbsolutePath().toString();
  }

  public String getRepositoryFileName() {
    return RepositoryUtils.getRepositoryFileName(getName());
  }

  public File getRepositoryFile() {
    return new File(
        Paths.get(
            getConfig().getCorrelationsTemplateInstallationFolder(), getRepositoryFileName()
        ).toAbsolutePath().toString()
    );
  }

  @Override
  public Collection<String> checkRepositoryURL(String url) {
    List<String> errors = new ArrayList<>();
    errors.addAll(super.checkRepositoryURL(url));
    return errors;
  }
}
