package ru.nlp_project.story_line2.crawler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;

public class CrawlerToJSON {


  public static void main(String[] args) throws Exception {
    File scriptDir = new File("/var/tmp/parsers");
    FileUtils.forceMkdir(scriptDir);
    FileUtils.forceDeleteOnExit(scriptDir);
    FileUtils.copyDirectory(new File("src/main/groovy/ru/nlp_project/story_line2/crawler/parser"),
        scriptDir);
    Path configDir = Files.createTempDirectory("crawler-config");
    FileUtils.forceDeleteOnExit(configDir.toFile());
    FileUtils.copyDirectory(new File("src/main/resources/ru/nlp_project/story_line2/crawler"),
        configDir.toFile(), new SuffixFileFilter("json"));

    Crawler crawler =
        Crawler.newInstance(configDir.toFile() + File.separator + "CrawlerToJSON.json", false);
    crawler.run();
  }
}
