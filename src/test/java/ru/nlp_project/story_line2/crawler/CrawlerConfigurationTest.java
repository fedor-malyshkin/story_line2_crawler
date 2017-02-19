package ru.nlp_project.story_line2.crawler;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.dropwizard.jackson.Jackson;

public class CrawlerConfigurationTest {

	private Path configDir;

	@Before
	public void setUp() throws Exception {
		configDir = Files.createTempDirectory("crawler-config");
		FileUtils.forceDeleteOnExit(configDir.toFile());
		FileUtils.copyDirectory(new File("src/test/resources/ru/nlp_project/story_line2/crawler"),
				configDir.toFile(), new SuffixFileFilter("yml"));

	}

	@Test
	public void testNewInstance() throws IOException {
		ObjectMapper mapper = Jackson.newObjectMapper(new YAMLFactory()); // create once, reuse
		CrawlerConfiguration value = mapper.readValue(
				new File(configDir.toFile() + File.separator + "CrawlerConfigurationTest.yml"),
				CrawlerConfiguration.class);
		assertEquals(1, value.crawlerPerSite);
		assertEquals(1, value.parseSites.size());
	}

}
