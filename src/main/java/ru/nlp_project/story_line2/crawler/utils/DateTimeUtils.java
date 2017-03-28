package ru.nlp_project.story_line2.crawler.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeUtils {
	private static ZoneId defaultZoneId;
	private static DateTimeFormatter formatter;

	static {
		defaultZoneId = ZoneId.systemDefault();
		formatter = DateTimeFormatter.ISO_INSTANT.withZone(defaultZoneId);
	}

	public static ZoneId converToZoneId(String caption) {
		return ZoneId.of(caption);
	}

	public static ZonedDateTime correctTimeZoneModern(Date date, ZoneId sourceZoneId) {
		Instant instant = date.toInstant(); // считает, что переходит в UTC с текущей зоной
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, defaultZoneId);
		return localDateTime.atZone(sourceZoneId);
	}

	public static Date correctTimeZoneOld(Date date, ZoneId sourceZoneId) {
		ZonedDateTime zonedUTC = correctTimeZoneModern(date, sourceZoneId);
		return Date.from(zonedUTC.toInstant());
	}

	public static ZonedDateTime now() {
		return ZonedDateTime.now();
	}

}
