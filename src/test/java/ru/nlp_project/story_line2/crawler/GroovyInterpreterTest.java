package ru.nlp_project.story_line2.crawler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.*;

import edu.uci.ics.crawler4j.url.WebURL;
import ru.nlp_project.story_line2.crawler.impl.GroovyInterpreterImpl;

public class GroovyInterpreterTest {

	private static File srcDir =
			new File("src/test/groovy/ru/nlp_project/story_line2/crawler/parser");
	private static Path scriptDir;
	private IGroovyInterpreter testable;
	private CrawlerConfiguration configuration;

	@Before
	public void setUp() throws Exception {
		scriptDir = Files.createTempDirectory("crawler");
		configuration = new CrawlerConfiguration();
		configuration.scriptDir = scriptDir.toString();
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.forceDelete(scriptDir.toFile());
	}

	@Test(expected = IllegalStateException.class)
	public void testInitializeWithNonExistingScriptDir() {
		configuration.scriptDir = "/tmp/NON_EXISTING_DIR";
		testable = GroovyInterpreterImpl.newInstance(configuration);
	}

	@Test(expected = IllegalStateException.class)
	public void testInitializeWithNonEmptyScriptDir() {
		testable = GroovyInterpreterImpl.newInstance(configuration);
	}

	@Test(expected = IllegalStateException.class)
	public void testGetSourceFromScriptClass_NoSource() throws IOException {
		IOFileFilter filter = FileFilterUtils.prefixFileFilter("non_existing_source");
		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
	}


	@Test
	public void testCallExtractData() throws IOException {
		IOFileFilter filter = FileFilterUtils.prefixFileFilter("working_script");
		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
		WebURL webURL = new WebURL();
		webURL.setURL("http://working_script");
		testable.extractData("working_script", webURL, "");
	}

	@Test
	public void testCallShouldVisit() throws IOException {
		IOFileFilter filter = FileFilterUtils.prefixFileFilter("working_script");
		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
		WebURL webURL = new WebURL();
		webURL.setURL("http://working_script");
		testable.shouldVisit("working_script", webURL);
	}


	@Test
	@Ignore
	public void testAutoRefreshGroovyCode() throws IOException {
		IOFileFilter filter = FileFilterUtils.prefixFileFilter("working_script");
		FileUtils.copyDirectory(srcDir, scriptDir.toFile(), filter, false);
		testable = GroovyInterpreterImpl.newInstance(configuration);
		WebURL webURL = new WebURL();
		webURL.setURL("http://working_script");
		Map<String, Object> extractData = testable.extractData("working_script", webURL, "");
		org.junit.Assert.assertThat(extractData.get("title"), equalTo("title"));		
		
		File fileSource = new File(srcDir, "new_working_script.groovy");
		File dstSource = new File(scriptDir.toFile(), "working_script.groovy");
		
		FileUtils.copyFile(fileSource, dstSource);
		extractData = testable.extractData("working_script", webURL, "");
		org.junit.Assert.assertThat(extractData.get("title"), equalTo("title2"));
	}
}
