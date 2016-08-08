package ru.nlp_project.story_line2.crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Класс чтения конфигурации.
 * 
 * Предназначен для разрешения имен и выдачи InputStream'ов для файлов на которые имеются ссылки.
 * Самостоятельно читает лишь основной файл конфигурации.
 *
 * MULTITHREAD_SAFE: YES
 * 
 * @author fedor
 *
 */
public class ConfigurationReader {


  public class ConfigurationMain {
    public boolean debug;
    public boolean storeFiles;
    public String parserFile;
    public String loggingConfigFile;
    public int crawlerPerSite;
    public String scriptDir;
    public List<SiteConfiguration> sites = new ArrayList<>();
    public String storageDir;
  }



  class SiteConfiguration {
    public String domain;

    public String seed;

    public SiteConfiguration(String domain, String seed) {
      super();
      this.domain = domain;
      this.seed = seed;
    }
  }

  public static ConfigurationReader newInstance(String configFile) throws IOException {
    ConfigurationReader result = new ConfigurationReader(configFile);
    result.read();
    return result;
  }

  private String configFile;
  private ConfigurationMain configurationMain;
  private ObjectMapper objectMapper;
  private File parentFile;

  ConfigurationReader(String configFile) {
    this.configFile = configFile;
    this.configurationMain = new ConfigurationMain();
  }

  public String getAbsolutePath(String fileName) throws FileNotFoundException {
    if (null == fileName)
      throw new FileNotFoundException("Incorrect configuration file: " + fileName + "");
    File file = new File(parentFile, fileName);
    if (!file.isFile() || !file.exists())
      throw new FileNotFoundException(
          "Incorrect configuration file: " + "(" + file.getAbsolutePath() + ") " + fileName + "");
    return file.getAbsolutePath();
  }

  public ConfigurationMain getConfigurationMain() {
    return configurationMain;
  }

  public InputStream getInputStream(String fileName) throws FileNotFoundException {
    if (null == fileName)
      throw new FileNotFoundException("Incorrect configuration file: " + fileName + "");
    File file = new File(parentFile, fileName);
    if (!file.isFile() || !file.exists())
      throw new FileNotFoundException("Incorrect configuration file: " + fileName + "");
    return new FileInputStream(file);
  }

  private void read() throws IOException {
    File file = new File(this.configFile);

    if (!file.isFile() || !file.exists())
      throw new FileNotFoundException("Incorrect configuration file: " + configFile + "");
    parentFile = file.getParentFile();

    JsonFactory jsonFactory = new JsonFactory();
    jsonFactory.configure(Feature.ALLOW_COMMENTS, true);
    objectMapper = new ObjectMapper(jsonFactory);

    readMainConfig(file);
  }

  @SuppressWarnings({"rawtypes"})
  private void readMainConfig(File file) throws IOException {
    HashMap map = objectMapper.readValue(file, HashMap.class);
    configurationMain = new ConfigurationMain();
    configurationMain.debug = map.get("debug") == Boolean.TRUE ? true : false;
    configurationMain.storeFiles = map.get("store_files") == Boolean.TRUE ? true : false;
    configurationMain.parserFile = (String) map.get("parser_file");
    configurationMain.scriptDir = (String) map.get("script_dir");
    configurationMain.storageDir = (String) map.get("storage_dir");
    configurationMain.crawlerPerSite =
        (Integer) map.getOrDefault("crawler_per_site", new Integer(4));

    // logging
    configurationMain.loggingConfigFile = (String) map.get("logging_config");
    if (configurationMain.loggingConfigFile == null)
      System.setProperty("logback.configurationFile", "config/logback.xml");
    else
      System.setProperty("logback.configurationFile", configurationMain.loggingConfigFile);

    // sites
    List<Map> listSitesMap = (List) map.get("sites");
    for (Map siteMap : listSitesMap) {
      String domain = (String) siteMap.get("domain");
      String seed = (String) siteMap.get("seed");
      configurationMain.sites.add(new SiteConfiguration(domain, seed));

    }
  }

}
