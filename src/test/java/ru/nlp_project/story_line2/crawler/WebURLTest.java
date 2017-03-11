package ru.nlp_project.story_line2.crawler;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.equalTo;

import edu.uci.ics.crawler4j.url.WebURL;

public class WebURLTest {


	private WebURL testable;

	@Before
	public void setUp() {
		testable = new WebURL();
	}

	@Test
	public void testSetSingleDomain() {
		testable.setURL("http://domain.ru");
		String domain = testable.getDomain();
		assertThat(domain, equalTo("domain.ru"));
	}
	
	
	@Test
	public void testSetSingleDomainUpperCase() {
		testable.setURL("http://dOmain.ru");
		String domain = testable.getDomain();
		assertThat(domain, equalTo("dOmain.ru"));
	}
	
	@Test
	public void testSetSingleDomainWithPrefixes() {
		testable.setURL("http://www.domain.ru");
		String domain = testable.getDomain();
		assertThat(domain, equalTo("domain.ru"));
	}
	
	@Test
	public void testSetSingleDomainWithOtherProto() {
		testable.setURL("https://www.domain.ru");
		String domain = testable.getDomain();
		assertThat(domain, equalTo("domain.ru"));
	}
	
	
	@Test
	public void testSetSubDomains() {
		testable.setURL("https://www.subdomain.domain.ru");
		String domain = testable.getDomain();
		assertThat(domain, equalTo("domain.ru"));
	}


}
