package ru.nlp_project.story_line2.crawler.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.junit.Test;

public class DateTimeUtilsTest {

  private Date getFixedDate() {
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.set(2017, Calendar.APRIL, 22, 22, 21);
    Date date = gregorianCalendar.getTime();
    date.setSeconds(0);
    return date;
  }

  private ZoneId getFixedZoneId() {
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
