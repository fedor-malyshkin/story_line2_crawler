package ru.nlp_project.story_line2.crawler.utils;

import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

import com.mongodb.BasicDBObject;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.startsWith;

import ru.nlp_project.story_line2.crawler.model.CrawlerNewsArticle;

public class BSONUtilsTest {


	public Date getFixedDate() {
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		gregorianCalendar.set(2017, 03, 22, 22, 21);
		Date date = gregorianCalendar.getTime();
		date.setSeconds(0);
		return date;
	}

	@Test
	public void testSerializeDateTimeInUTC() {
		CrawlerNewsArticle article = new CrawlerNewsArticle(null, null, null, getFixedDate(), null,
				null, null, null);
		BasicDBObject res = BSONUtils.serialize(article);
		// Sat Apr 22 20:21:00 MSK 2017
		assertThat(res.toString(), startsWith("{ \"publication_date\" : { \"$date\" : \"2017-04-22T19:21:00"));


	}

}
