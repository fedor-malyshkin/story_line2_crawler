package ru.nlp_project.story_line2.crawler;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Before;
import org.junit.Test;

import ru.nlp_project.story_line2.crawler.ConfigurationReader.SiteConfiguration;

public class ConfigurationReaderTest {

  private Path configDir;

  @Before
  public void setUp() throws Exception {
    configDir = Files.createTempDirectory("crawler-config");
    FileUtils.forceDeleteOnExit(configDir.toFile());
    FileUtils.copyDirectory(new File("src/test/resources/ru/nlp_project/story_line2/crawler"),
        configDir.toFile(), new SuffixFileFilter("json"));

  }

  @Test
  public void testNewInstance() throws IOException {
    ConfigurationReader config = ConfigurationReader
        .newInstance(configDir.toFile() + File.separator + "ConfigurationReaderTest.json");
    List<SiteConfiguration> sites = config.getConfigurationMain().sites;
    SiteConfiguration sc = sites.get(0);
    assertEquals("bnkomi.ru", sc.domain);
    assertEquals("http://www.bnkomi.ru", sc.seed);
  }

}
