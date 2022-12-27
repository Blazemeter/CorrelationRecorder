package com.blazemeter.jmeter.correlation;

import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.InvalidRulePartElementException;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.proxy.ComparableCookie;
import com.blazemeter.jmeter.correlation.core.proxy.CorrelationProxy;
import com.blazemeter.jmeter.correlation.core.proxy.Jsr223PreProcessorFactory;
import com.blazemeter.jmeter.correlation.core.proxy.PendingProxy;
import com.blazemeter.jmeter.correlation.core.proxy.ReflectionUtils;
import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistry;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRegistry;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion.Builder;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.blazemeter.jmeter.correlation.gui.CorrelationRulesTestElement;
import com.blazemeter.jmeter.correlation.gui.RulesGroupTestElement;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.modifiers.JSR223PreProcessor;
import org.apache.jmeter.protocol.http.control.RecordingController;
import org.apache.jmeter.protocol.http.proxy.Daemon;
import org.apache.jmeter.protocol.http.proxy.ProxyControl;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.util.HTTPConstants;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.NullProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationProxyControl extends ProxyControl implements
    CorrelationTemplatesRegistryHandler, CorrelationTemplatesRepositoriesRegistryHandler {
  private static final int MINIMUM_VERSION_ORDER_FIX = 54;
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationProxyControl.class);
  //To allows Backward Compatibility
  private static final String CORRELATION_RULES = "CorrelationProxyControl.rules";
  private static final String CORRELATION_GROUPS = "CorrelationProxyControl.groups";
  private static final String CORRELATION_COMPONENTS = "CorrelationProxyControl.components";
  private static final String RESPONSE_FILTER = "CorrelationProxyControl.responseFilter";
  private static final String TEMPLATE_PATH = "CorrelationProxyControl.templatePath";
  private static final String RECORDER_NAME = "bzm - Correlation Recorder";
  // we use reflection to be able to call these non visible methods and not have to re implement
  // them.
  private static final Method FIND_FIRST_NODE_OF_TYPE = getProxyControlMethod("findFirstNodeOfType",
      Class.class);
  private static final Method INIT_KEY_STORE_METHOD = getProxyControlMethod("initKeyStore");
  private static final Method NOTIFY_TEST_LISTENERS_OF_START_METHOD = getProxyControlMethod(
      "notifyTestListenersOfStart");
  private static final Method FILTER_URL_METHOD = getProxyControlMethod("filterUrl",
      HTTPSamplerBase.class);
  private static final Method FILTER_CONTENT_TYPE_METHOD = getProxyControlMethod(
      "filterContentType", SampleResult.class);
  private static final Field SERVER_FIELD = getProxyControlField("server");
  private static final Field SAMPLE_GAP_FIELD = getProxyControlField("sampleGap");
  // this is used to deliver samples in order, check CorrelationProxy.
  private final LinkedHashMap<Object, PendingProxy> pendingProxies = new LinkedHashMap<>();
  private final Set<ComparableCookie> lastComparableCookies = new LinkedHashSet<>();
  private transient CorrelationComponentsRegistry componentsRegistry;
  private transient CorrelationTemplatesRepositoriesConfiguration templateRepositoryConfig;
  private transient LocalConfiguration localConfiguration;
  private transient CorrelationEngine correlationEngine;
  private transient CorrelationTemplatesRegistry correlationTemplatesRegistry;
  private JMeterTreeNode target = null;

  @SuppressWarnings("checkstyle:RedundantModifier")
  public CorrelationProxyControl() {
    correlationEngine = new CorrelationEngine();
    componentsRegistry = CorrelationComponentsRegistry.getInstance();
    localConfiguration = new LocalConfiguration(getTemplateDirectoryPath());
    correlationTemplatesRegistry = new LocalCorrelationTemplatesRegistry(localConfiguration);
    templateRepositoryConfig =
        new CorrelationTemplatesRepositoriesConfiguration(localConfiguration);
    setName(RECORDER_NAME);
  }

  @VisibleForTesting
  public CorrelationProxyControl(
      CorrelationComponentsRegistry componentsRegistry,
      CorrelationTemplatesRepositoriesConfiguration templateRepositoryConfig,
      LocalConfiguration localConfiguration,
      CorrelationEngine correlationEngine,
      CorrelationTemplatesRegistry correlationTemplatesRegistry) {
    this.componentsRegistry = componentsRegistry;
    this.templateRepositoryConfig =
        templateRepositoryConfig;
    this.localConfiguration = localConfiguration;
    this.correlationEngine = correlationEngine;
    this.correlationTemplatesRegistry = correlationTemplatesRegistry;
  }

  private static Method getProxyControlMethod(String methodName, Class<?>... paramTypes) {
    return ReflectionUtils.getMethod(ProxyControl.class, methodName, paramTypes);
  }

  private static Field getProxyControlField(String fieldName) {
    return ReflectionUtils.getField(ProxyControl.class, fieldName);
  }

  private static String getTemplateDirectoryPath() {
    return JMeterUtils.getPropDefault(TEMPLATE_PATH, JMeterUtils.getJMeterHome());
  }

  private static List<CorrelationRule> getRulesFromListOfGroups(List<RulesGroup> groups) {
    return groups.stream().map(RulesGroup::getRules).flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private static boolean requireJMeterRequestsOrderFix() {
    /*
    * From JMeter's 5.4, a fix was make to handle requests that 
    * took long. We implemented custom methods that fixed this issue
    * for the versions previous to 5.4.
    */
    int version = Integer.parseInt(
        JMeterUtils.getJMeterVersion().substring(0, 3).replace(".", ""));

    return version >= MINIMUM_VERSION_ORDER_FIX;
  }
  
  @Override
  public synchronized void startProxy() throws IOException {
    lastComparableCookies.clear();
    correlationEngine.reset();
    pendingProxies.clear();
    
    if (requireJMeterRequestsOrderFix()) {
      super.startProxy();
      return;      
    }

    try {
      initKeyStore();
    } catch (GeneralSecurityException e) {
      LOG.error("Could not initialise key store", e);
      throw new IOException("Could not create keystore", e);
    } catch (IOException e) {
      LOG.error("Could not initialise key store", e);
      throw e;
    }
    notifyTestListenersOfStart();
    try {
      Daemon server = new Daemon(getPort(), this, CorrelationProxy.class);
      setServer(server);
      if (getProxyPauseHTTPSample().isEmpty()) {
        setSampleGap(JMeterUtils.getPropDefault("proxy.pause", 5000));
      } else {
        setSampleGap(Long.parseLong(getProxyPauseHTTPSample().trim()));
      }
      server.start();
      if (GuiPackage.getInstance() != null) {
        GuiPackage.getInstance().register(server);
      }
    } catch (IOException e) {
      LOG.error("Could not create HTTP(S) Test Script Recorder Proxy daemon", e);
      throw e;
    }
  }

  /*
    We need to re implement this method to be able to use another proxy class.
    Check CorrelationProxy
  */
  private void initKeyStore() throws GeneralSecurityException, IOException {
    try {
      INIT_KEY_STORE_METHOD.invoke(this);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof GeneralSecurityException) {
        throw (GeneralSecurityException) cause;
      } else if (cause instanceof IOException) {
        throw (IOException) cause;
      } else if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw (Error) cause;
      }
    }
  }

  private void notifyTestListenersOfStart() {
    try {
      NOTIFY_TEST_LISTENERS_OF_START_METHOD.invoke(this);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw (Error) cause;
      }
    }
  }

  private void setServer(Daemon server) {
    try {
      SERVER_FIELD.set(this, server);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private void setSampleGap(long sampleGap) {
    try {
      SAMPLE_GAP_FIELD.set(this, sampleGap);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public CorrelationRulesTestElement getCorrelationRulesTestElement() {
    return (CorrelationRulesTestElement) getProperty(CORRELATION_RULES).getObjectValue();
  }

  @Override
  public synchronized void deliverSampler(HTTPSamplerBase sampler, TestElement[] testElements,
      SampleResult result) {

    if (!requireJMeterRequestsOrderFix()) {
      pendingProxies.get(Thread.currentThread()).update(sampler, testElements, result);
      return;
    }

    if (sampler != null) {
      List<TestElement> children = new ArrayList<>(Arrays.asList(testElements));
      correlationEngine.process(sampler, children, result, this.getContentTypeInclude());
      testElements = children.toArray(new TestElement[0]);
    }
    super.deliverSampler(sampler, testElements, result);
  }

  public synchronized void startedProxy(Thread proxy) {
    pendingProxies.put(proxy, new PendingProxy(getTarget()));
  }

  public synchronized void endedProxy(Thread proxy) {
    PendingProxy pendingProxy = pendingProxies.get(proxy);
    /*
    this may happen if proxy had an issue parsing request or some other case where getOutputStream
    is not invoked for used clientSocket
     */
    if (pendingProxy == null) {
      return;
    }
    /*
     when result is null then the request is not recorded. This is to keep logic from JMeter
     recorder.
     */
    if (pendingProxy.getResult() == null) {
      pendingProxies.remove(proxy);
    } else {
      pendingProxy.setComplete(true);
    }
    deliverPendingCompletedRequests();
  }

  private void deliverPendingCompletedRequests() {
    Iterator<PendingProxy> it = pendingProxies.values().iterator();
    while (it.hasNext()) {
      PendingProxy proxy = it.next();
      if (!proxy.isComplete()) {
        return;
      }
      deliverCompletedProxy(proxy);
      it.remove();
    }
  }

  private void deliverCompletedProxy(PendingProxy proxy) {
    if (proxy.getSampler() != null && filter(proxy.getSampler(), proxy.getResult())) {
      this.target = proxy.getTarget();
      List<TestElement> children = new ArrayList<>(Arrays.asList(proxy.getTestElements()));
      addMissingCookiesChildren(proxy.getResult(), children);
      correlationEngine.process(proxy.getSampler(), children, proxy.getResult(),
          this.getContentTypeInclude());
      proxy.setTestElements(children.toArray(new TestElement[0]));
    }
    super.deliverSampler(proxy.getSampler(), proxy.getTestElements(), proxy.getResult());
  }

  private boolean filter(HTTPSamplerBase sampler, SampleResult result) {
    try {
      return ((Boolean) FILTER_URL_METHOD.invoke(this, sampler)) &&
          ((Boolean) FILTER_CONTENT_TYPE_METHOD.invoke(this, result));
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw (Error) cause;
      }
    }
  }

  private void addMissingCookiesChildren(SampleResult result, List<TestElement> children) {
    List<ComparableCookie> newComparableCookies = extractNewRequestCookies(result);
    for (ComparableCookie comparableCookie : newComparableCookies) {
      children.add(buildCookiePreProcessor(comparableCookie));
      addCookie(comparableCookie);
    }
    addResponseHeaders(result);
  }

  private List<ComparableCookie> extractNewRequestCookies(SampleResult result) {
    String cookieHeader = ((HTTPSampleResult) result).getCookies();
    if (cookieHeader == null || cookieHeader.trim().isEmpty()) {
      return Collections.emptyList();
    }
    /*
     we can't use CookieManager, CookieSpec or even HttpCookie since they treat the different
     cookies just as attributes, and then is not possible to get the values.
     */
    URL url = result.getURL();
    return Arrays.stream(cookieHeader.split("; "))
        .map(s -> {
          int separatorPos = s.indexOf("=");
          return new ComparableCookie(s.substring(0, separatorPos), s.substring(separatorPos + 1),
              url.getHost());
        })
        .filter(c -> !lastComparableCookies.contains(c))
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  protected void addCookie(ComparableCookie comparableCookie) {
    lastComparableCookies.stream()
        .filter(
            c -> comparableCookie.getName().equals(c.getName()) && comparableCookie.getDomain()
                .equals(c.getDomain()))
        .findAny()
        .ifPresent(lastComparableCookies::remove);
    lastComparableCookies.add(comparableCookie);
  }

  private JSR223PreProcessor buildCookiePreProcessor(ComparableCookie comparableCookie) {
    return Jsr223PreProcessorFactory.fromNameAndScript("Set cookie - " + comparableCookie.getName(),
        String.format("import org.apache.jmeter.protocol.http.control.*\n\n"
                + "// we use this instead of sampler.url to avoid premature resolution of url "
                + "parameters and allow other pre processors to affect rest of url\n"
                + "def url = sampler.path.startsWith('http://') || "
                + "sampler.path.startsWith('https://') ? new URL(sampler.path) : "
                + "new URL(sampler.protocol + '://' + sampler.domain)\n"
                + "sampler.getCookieManager().add(new Cookie('%s', '%s', url.host, '', "
                + "sampler.isSecure(url), 0))",
            comparableCookie.getName(), comparableCookie.getName()));
  }

  private void addResponseHeaders(SampleResult result) {
    URL url = result.getURL();
    Arrays.stream(result.getResponseHeaders().split("\n"))
        .filter(h -> h.toLowerCase().startsWith(HTTPConstants.HEADER_SET_COOKIE))
        .map(h -> {
          int cookieStartPos = h.indexOf(":") + 1;
          int cookieNameEndPos = h.indexOf("=", cookieStartPos);
          int cookieValueEndPos = h.indexOf(";", cookieNameEndPos + 1);
          return new ComparableCookie(h.substring(cookieStartPos, cookieNameEndPos).trim(),
              h.substring(cookieNameEndPos + 1, cookieValueEndPos), url.getHost());
        })
        .forEach(this::addCookie);
  }

  @Override
  public JMeterTreeNode findTargetControllerNode() {
    if (requireJMeterRequestsOrderFix()) {
      return super.findTargetControllerNode();
    }
    
    JMeterTreeNode myTarget = target;
    if (myTarget != null) {
      return myTarget;
    }
    try {
      myTarget = (JMeterTreeNode) FIND_FIRST_NODE_OF_TYPE.invoke(this, RecordingController.class);
      if (myTarget != null) {
        return myTarget;
      }
      myTarget = (JMeterTreeNode) FIND_FIRST_NODE_OF_TYPE.invoke(this, AbstractThreadGroup.class);
      if (myTarget != null) {
        return myTarget;
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw (Error) cause;
      }
    }
    LOG.error("Program error: test script recording target not found.");
    return null;
  }

  @Override
  public void onSaveTemplate(Builder builder) throws IOException, ConfigurationException {
    TemplateVersion template = builder
        .withGroups(getGroups())
        .withComponents(getCorrelationComponents())
        .withResponseFilters(getResponseFilter())
        .build();

    correlationTemplatesRegistry.save(template);
    updateLocalRepository(template);
    installTemplate(template.getRepositoryId(), template.getId(), template.getVersion());
  }

  public List<RulesGroup> getGroups() {
    JMeterProperty testElement = getProperty(CORRELATION_GROUPS);
    List<RulesGroup> groups = new ArrayList<>();
    if (testElement instanceof NullProperty) {
      //To allows Backward Compatibility
      JMeterProperty rulesListTestElement = getProperty(CORRELATION_RULES);
      if (!(rulesListTestElement instanceof NullProperty)) {
        ((CorrelationRulesTestElement) rulesListTestElement.getObjectValue()).getRules();
        List<CorrelationRule> rules = ((CorrelationRulesTestElement) rulesListTestElement
            .getObjectValue()).getRulesList();
        groups.add(new RulesGroup.Builder().withRules(rules).build());
      }
      return groups;

    }
    ((CollectionProperty) testElement)
        .forEach(g -> groups.add(((RulesGroupTestElement) g.getObjectValue()).getRulesGroup()));
    return groups;
  }

  public void setCorrelationGroups(List<RulesGroup> groups) {
    setProperty(new CollectionProperty(CORRELATION_GROUPS,
        groups.stream().map(RulesGroup::buildTestElement).collect(Collectors.toList())));
    correlationEngine.setCorrelationRules(groups, componentsRegistry);
    //To allows Backward Compatibility, this remove the old JMeter property before save the jmx
    removeProperty(CORRELATION_RULES);
  }

  public String getCorrelationComponents() {
    return getPropertyAsString(CORRELATION_COMPONENTS);
  }

  public void setCorrelationComponents(String correlationComponents) {
    setProperty(CORRELATION_COMPONENTS,
        componentsRegistry.updateActiveComponents(correlationComponents, new ArrayList<>()));
  }

  public String getResponseFilter() {
    return getPropertyAsString(RESPONSE_FILTER);
  }

  @VisibleForTesting
  public void setResponseFilter(String responseFilter) {
    setProperty(RESPONSE_FILTER, responseFilter);
  }

  private void updateLocalRepository(TemplateVersion template) {
    templateRepositoryConfig
        .updateLocalRepository(template.getId(), template.getVersion());
  }

  @Override
  public void onLoadTemplate(String repositoryOwner, String id, String templateVersion)
      throws IOException {
    Optional<TemplateVersion> correlationTemplate = correlationTemplatesRegistry
        .findByID(repositoryOwner, id, templateVersion);
    if (correlationTemplate.isPresent()) {
      TemplateVersion template = correlationTemplate.get();

      List<RulesGroup> loadedGroups = template.getGroups();
      List<CorrelationRule> rules = template.getRules();

      if (!rules.isEmpty()) {
        loadedGroups.add(new RulesGroup.Builder()
            .withId("New Group")
            .withRules(rules).build());
      }

      append(template.getComponents(), loadedGroups, template.getResponseFilters());
    } else {
      LOG.error("Template not found {}", id);
    }
  }

  private void append(String loadedComponents, List<RulesGroup> loadedGroups,
      String loadedFilters) {

    String actualComponents = getCorrelationComponents();
    setCorrelationComponents(loadedComponents.isEmpty() ? actualComponents
        : cleanRepeated(actualComponents, loadedComponents));

    String actualFilters = getResponseFilter();
    setResponseFilter(loadedFilters.isEmpty() ? actualFilters
        : cleanRepeated(actualFilters, loadedFilters));

    List<RulesGroup> actualGroups = getGroups();
    for (RulesGroup group : loadedGroups) {
      Optional<RulesGroup> repeated = actualGroups.stream()
          .filter(g -> g.getId().equals(group.getId()))
          .findFirst();

      if (repeated.isPresent()) {
        String renamedTitle = findNextAvailableTitle(group.getId(), 1);
        group.setId(renamedTitle);
      }
      actualGroups.add(group);
    }

    setCorrelationGroups(actualGroups);
  }

  private String findNextAvailableTitle(String title, int repeatedTimes) {
    String renamedTitle = title + " (" + repeatedTimes + ")";
    Optional<RulesGroup> repeated = getGroups().stream()
        .filter(g -> g.getId().equals(renamedTitle))
        .findFirst();

    if (repeated.isPresent()) {
      return findNextAvailableTitle(title, repeatedTimes + 1);
    }

    return renamedTitle;
  }

  private String cleanRepeated(String actual, String loaded) {
    if (loaded == null || loaded.isEmpty()) {
      return actual;
    }

    Set<String> cleaned = actual.isEmpty() ? new HashSet<>()
        : new HashSet<>(Arrays.asList(actual.replace("\n", "").split(",")));
    cleaned.addAll(Arrays.asList(loaded.replace("\n", "").split(",")));

    return cleaned.stream().map(String::trim).collect(Collectors.joining(","));
  }

  private List<CorrelationRule> appendCorrelationRules(List<CorrelationRule> actualRules,
      List<CorrelationRule> loadedRules) {
    if (loadedRules.isEmpty()) {
      return actualRules;
    }

    LinkedHashSet<CorrelationRule> actualRulesSet = new LinkedHashSet<>(actualRules);
    actualRulesSet.addAll(loadedRules);

    return new ArrayList<>(actualRulesSet);
  }

  public String getContentTypeInclude() {
    return getPropertyAsString(RESPONSE_FILTER);
  }

  public List<TemplateVersion> getInstalledCorrelationTemplates() {
    return correlationTemplatesRegistry.getInstalledTemplates();
  }

  public void update(String correlationComponents, List<RulesGroup> groups, String responseFilter) {
    setCorrelationComponents(correlationComponents);
    setCorrelationGroups(groups);
    setResponseFilter(responseFilter);
  }

  @Override
  public void saveRepository(String id, String url) throws IOException {
    templateRepositoryConfig.save(id, url);
  }

  @Override
  public void deleteRepository(String id) throws IOException {
    templateRepositoryConfig.deleteRepository(id);
  }

  @Override
  public List<CorrelationTemplatesRepository> getCorrelationRepositories() {
    return templateRepositoryConfig.getCorrelationRepositories();
  }

  @Override
  public String getConfigurationRoute() {
    return localConfiguration.getRootFolder();
  }

  @Override
  public List<TemplateVersion> getCorrelationTemplatesByRepositoryName(String name) {
    return templateRepositoryConfig
        .getCorrelationTemplatesByRepositoryName(name);
  }

  @Override
  public void installTemplate(String repositoryName, String id, String version)
      throws ConfigurationException {
    templateRepositoryConfig.installTemplate(repositoryName, id, version);
  }

  @Override
  public void uninstallTemplate(String repositoryName, String id, String version)
      throws ConfigurationException {
    templateRepositoryConfig.uninstallTemplate(repositoryName, id, version);
  }

  @Override
  public String getRepositoryURL(String otherName) {
    return templateRepositoryConfig.getRepositoryURL(otherName);
  }

  @Override
  public List<File> getConflictingInstalledDependencies(
      List<CorrelationTemplateDependency> dependencies) {
    return localConfiguration.findConflictingDependencies(dependencies);
  }

  @Override
  public void deleteConflicts(List<File> dependencies) {
    localConfiguration.deleteConflicts(dependencies);
  }

  @Override
  public void downloadDependencies(List<CorrelationTemplateDependency> dependencies)
      throws IOException {
    localConfiguration.downloadDependencies(dependencies);
  }

  @Override
  public boolean isLocalTemplateVersionSaved(String templateId, String templateVersion) {
    return templateRepositoryConfig
        .isLocalTemplateVersionSaved(templateId, templateVersion);
  }

  @Override
  public void resetJMeter() {
    //Left empty
  }

  @Override
  public List<String> checkURL(String id, String url) {
    return localConfiguration.checkRepositoryURL(id, url);
  }

  @Override
  public boolean refreshRepositories(String localConfigurationRoute,
      Consumer<Integer> setProgressConsumer) {
    return templateRepositoryConfig
        .refreshRepositories(localConfigurationRoute, setProgressConsumer);
  }

  @Override
  public boolean isValidDependencyURL(String url, String name, String version) {
    return localConfiguration.isValidDependencyURL(url, name, version);
  }

  private List<CorrelationRule> getCorrelationRulesFromTestElement(
      CorrelationRulesTestElement testElement) {
    if (testElement == null) {
      return new ArrayList<>();
    }

    return testElement.getRules().stream()
        .map(e -> {
          CorrelationRule correlationRule = new CorrelationRule();
          String referenceName = e.getReferenceName();
          correlationRule.setReferenceName(referenceName);
          correlationRule.setEnabled(e.isRuleEnabled());
          updateExtractorFromTestElement(e, correlationRule, referenceName);
          updateReplacementFromTestElement(e, correlationRule, referenceName);
          return correlationRule;
        })
        .collect(Collectors.toList());
  }

  private void updateExtractorFromTestElement(CorrelationRuleTestElement e,
      CorrelationRule correlationRule, String referenceName) {
    try {
      //Only when no Extractor was selected, this method returns null
      correlationRule.setCorrelationExtractor(e.getCorrelationExtractor());
    } catch (InvalidRulePartElementException exception) {
      LOG.warn("Couldn't load Correlation Extractor for Rule with {}'s refVar.", referenceName,
          exception);
    }
  }

  private void updateReplacementFromTestElement(CorrelationRuleTestElement e,
      CorrelationRule correlationRule, String referenceName) {
    try {
      //Only when no Replacement was selected, this method returns null
      correlationRule.setCorrelationReplacement(e.getCorrelationReplacement());
    } catch (InvalidRulePartElementException exception) {
      LOG.warn("Couldn't load Correlation Replacement for Rule with {}'s refVar.", referenceName,
          exception);
    }
  }

  @VisibleForTesting
  protected Set<ComparableCookie> getLastCookies() {
    return lastComparableCookies;
  }

  @VisibleForTesting
  protected LinkedHashMap<Object, PendingProxy> getPendingProxies() {
    return pendingProxies;
  }

  private void readObject(ObjectInputStream inputStream)
      throws IOException, ClassNotFoundException {
    inputStream.defaultReadObject();
    correlationEngine = new CorrelationEngine();
    componentsRegistry = CorrelationComponentsRegistry.getInstance();
    localConfiguration = new LocalConfiguration(getTemplateDirectoryPath());
    correlationTemplatesRegistry = new LocalCorrelationTemplatesRegistry(localConfiguration);
    templateRepositoryConfig =
        new CorrelationTemplatesRepositoriesConfiguration(localConfiguration);
    setName(RECORDER_NAME);
  }

}
