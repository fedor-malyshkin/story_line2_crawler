package ru.nlp_project.story_line2.crawler;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CrawlerTest {

	private CrawlerConfiguration configuration;
	private Crawler testable;

	@Before
	public void setup() throws Exception {
		// configuration
		/*
		Path configDir = Files.createTempDirectory("crawler-config");
		FileUtils.forceDeleteOnExit(configDir.toFile());
		FileUtils.copyDirectory(new File("src/test/resources/ru/nlp_project/story_line2/crawler"),
				configDir.toFile(), new SuffixFileFilter("yml"));
		FileUtils.copyDirectory(
				new File("src/main/groovy/ru/nlp_project/story_line2/crawler/parser"),
				configDir.toFile(), new SuffixFileFilter("groovy"));

		ObjectMapper mapper = Jackson.newObjectMapper(new YAMLFactory()); // create once, reuse
		configuration =
				mapper.readValue(new File(configDir.toFile() + File.separator + "CrawlerTest.yml"),
						CrawlerConfiguration.class);
		configuration.scriptDir = configDir.toFile().toString();
		configuration.storageDir = configDir.toFile().toString();
		*/
	}

	@Test
	@Ignore
	public void testRun() throws Exception {
		testable = Crawler.newInstance(configuration);
		testable.start();
	}

}
