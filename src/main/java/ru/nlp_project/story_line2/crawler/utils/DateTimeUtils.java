package ru.nlp_project.story_line2.crawler.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeUtils {
	private static ZoneId defaultZoneId;
	private static DateTimeFormatter formatter;

	{
		defaultZoneId = ZoneId.systemDefault();
		formatter = DateTimeFormatter.ISO_INSTANT.withZone(defaultZoneId);
	}



	public static ZoneId converToZoneId(String caption) {
		return ZoneId.of(caption);
	}

	public static ZonedDateTime toUTC(Date date, ZoneId zoneId) {
		Instant instant = date.toInstant();
		return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	public static ZonedDateTime now() {
		return ZonedDateTime.now();
	}

	public static String toString(Date date, ZoneId zoneId) {
		Instant instant = date.toInstant();
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
		return formatter.format(zonedDateTime);
	}

	public static String toString(ZonedDateTime date) {
		return formatter.format(date);
	}

	@Deprecated
	public static Date parseToObsoleteDate(String text) {
		// 1970-01-01T00:00:01Z
		ZonedDateTime zdt = ZonedDateTime.parse(text, formatter);
		return Date.from(zdt.toInstant());
	}

	public static ZonedDateTime parseToZonedDateTime(String text) {
		// 1970-01-01T00:00:01Z
		return ZonedDateTime.parse(text, formatter);
	}


}
