package com.almworks.explorer.qbuilder.filter;

import com.almworks.util.tests.BaseTestCase;

import java.util.Calendar;

/**
 * @author dyoma
 */
public class DateUnitTests extends BaseTestCase {
  public void testWeekDifference() {
    Calendar sunday2 = createDate(2006, 9, 8);
    assertEquals(Calendar.SUNDAY, sunday2.get(Calendar.DAY_OF_WEEK));
    Calendar monday2 = createDate(2006, 9, 9);
    assertEquals(Calendar.MONDAY, monday2.get(Calendar.DAY_OF_WEEK));

    assertEquals(Calendar.MONDAY, sunday2.getFirstDayOfWeek());

    checkDifference(1, monday2, sunday2);

    Calendar saturday = createDate(2006, 9, 7);
    assertEquals(Calendar.SATURDAY, saturday.get(Calendar.DAY_OF_WEEK));
    checkDifference(0, sunday2, saturday);
    checkDifference(1, monday2, saturday);

    Calendar monday = createDate(2006, 9, 2);
    assertEquals(Calendar.MONDAY, monday.get(Calendar.DAY_OF_WEEK));
    Calendar sunday = createDate(2006, 9, 1);
    assertEquals(Calendar.SUNDAY, sunday.get(Calendar.DAY_OF_WEEK));

    checkDifference(0, monday, saturday);
    checkDifference(-1, sunday, saturday);
    checkDifference(1, monday2, monday);
    checkDifference(1, sunday2, sunday);
  }

  private void checkDifference(int diff, Calendar later, Calendar ealier) {
    assertEquals(diff, DateUnit.WEEK.getDifference(later, ealier));
    assertEquals(-diff, DateUnit.WEEK.getDifference(ealier, later));
  }

  private Calendar createDate(int year, int month, int date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setFirstDayOfWeek(Calendar.MONDAY);
    calendar.set(year, month, date);
    return calendar;
  }
}
