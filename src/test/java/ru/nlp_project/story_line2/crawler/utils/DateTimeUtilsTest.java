package ru.nlp_project.story_line2.crawler.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.equalTo;

public class DateTimeUtilsTest {

	public Date getFixedDate() {
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		gregorianCalendar.set(2017, 03, 22, 22, 21);
		Date date = gregorianCalendar.getTime();
		date.setSeconds(0);
		return date;
	}
	
	public ZoneId getFixedZoneId() {
		return DateTimeUtils.converToZoneId("+05:00");
	}

	@Test
	public void testToUTC() {
		ZonedDateTime utc = DateTimeUtils.correctTimeZoneModern(getFixedDate(), getFixedZoneId());
		ZonedDateTime withNano = utc.withNano(0);
		assertThat(withNano.toString(), equalTo("2017-04-22T22:21+05:00"));
	}
	
	@Test
	public void testToObsoleteUTC() {
		Date date = DateTimeUtils.correctTimeZoneOld(getFixedDate(), getFixedZoneId());
		int hours = date.getHours();
		assertThat(hours, equalTo(20));
	}
	
	
	

}