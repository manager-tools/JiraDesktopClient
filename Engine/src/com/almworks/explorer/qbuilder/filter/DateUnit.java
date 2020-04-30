package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.misc.TimeService;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPCompare;
import com.almworks.util.L;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Function;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Const;
import org.almworks.util.Failure;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @author dyoma
 */
public abstract class DateUnit {
  public static final DateUnit DAY = new Day();
  public static final DateUnit WEEK = new Week();
  public static final DateUnit MONTH = new Month();
  public static final AListModel<DateUnit> ALL_UNITS = FixedListModel.create(DAY, WEEK, MONTH);
  public static final CanvasRenderer<DateUnit> RENDERER = new CanvasRenderer<DateUnit>() {
    public void renderStateOn(CellState state, Canvas canvas, DateUnit unit) {
      if (unit != null)
        canvas.appendText(unit.getDisplayName(2) + " ago");
    }
  };

  public abstract void subtract(Calendar calendar, int units, boolean laterBound);

  public abstract int getDifference(Calendar later, Calendar earlier);

  protected abstract String getDisplayName(int numberOfUnits);
  
  public static int getDayDifference(Calendar later, Calendar earlier) {
    Date laterDate =
      new Date(later.get(Calendar.YEAR) - 1900, later.get(Calendar.MONTH), later.get(Calendar.DAY_OF_MONTH));
    Date earlierDate =
      new Date(earlier.get(Calendar.YEAR) - 1900, earlier.get(Calendar.MONTH), earlier.get(Calendar.DAY_OF_MONTH));
    long days = (laterDate.getTime() - earlierDate.getTime()) / Const.DAY;
    assert days < Integer.MAX_VALUE : days;
    return (int) days;
  }

  @NotNull
  public String getDisplayString(String relative) {
    int integer;
    try {
      integer = Integer.parseInt(relative);
    } catch (NumberFormatException e) {
      return relative + " " + getDisplayName(2);
    }
    return integer + " " + getDisplayName(integer);
  }

  public String toString() {
    return "DateUnit(" + getDisplayName(1) + ")";
  }

  @Nullable
  public static DateUnit parseUnit(char c) {
    switch (c) {
    case 'd':
      return DAY;
    case 'w':
      return WEEK;
    case 'm':
      return MONTH;
    default:
      return null;
    }
  }

  private static void correctDayTime(Calendar calendar, boolean laterBound) {
    long time = calendar.getTimeInMillis();
    calendar.setTimeInMillis(laterBound ? DateUtil.toDayEnd(time) : DateUtil.toDayStart(time));
  }


  public static abstract class DateValue {
    protected static final DateFormat SERIALIZATION_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);

    public abstract Date getValue(Calendar calendar);

    public abstract String getString();

    public abstract String getUserString();

    public boolean before(@NotNull Date date) {
      Date value = getUptoDateValue();
      return DateUtil.isSameDay(value, date) || value.before(date);
    }

    public Date getUptoDateValue() {
      return getValue(Calendar.getInstance());
    }

    public boolean after(@NotNull Date date) {
      Date value = getUptoDateValue();
      return DateUtil.isSameDay(value, date) || value.after(date);
    }

    public abstract BoolExpr<DP> afterExpr(DBAttribute<Date> attr);

    public abstract BoolExpr<DP> afterExprDay(DBAttribute<Integer> attr);

    public abstract BoolExpr<DP> beforeExpr(DBAttribute<Date> attr);

    public abstract BoolExpr<DP> beforeExprDay(DBAttribute<Integer> attr);

    public static DateValue parse(String str, boolean laterBound) throws ParseException {
      if (str.length() == 0)
        throw new ParseException("Empty string", 0);
      char firstChar = str.charAt(0);
      String relative = str.substring(1);
      DateUnit unit = parseUnit(firstChar);
      if (unit != null)
        return RelativeDate.parseRelative(unit, relative, laterBound);
      else
        return new AbsoluteDate(SERIALIZATION_FORMAT.parse(str));
    }
  }


  public static class RelativeDate extends DateValue {
    @NotNull
    private final DateUnit myUnit;
    private final int myValue;
    private final boolean myLaterBound;
    private final Function<Calendar, Date> myGetDateTime = new GetDateTimeValue();
    private final Function<Calendar, Integer> myGetDay = new GetDayValue();

    public static final int REFRESH_PERIOD = 5 * (int)Const.SECOND;

    public RelativeDate(int value, @NotNull DateUnit unit, boolean laterBound) {
      myValue = value;
      myUnit = unit;
      myLaterBound = laterBound;
    }

    public Date getValue(Calendar calendar) {
      calendar = (Calendar) calendar.clone();
      myUnit.subtract(calendar, myValue, myLaterBound);
      return calendar.getTime();
    }

    public String getString() {
      return String.valueOf(getUnitChar(myUnit)) + myValue;
    }

    public String getUserString() {
      return getString();
    }

    public static char getUnitChar(DateUnit unit) {
      if (unit == DAY)
        return 'd';
      else if (unit == WEEK)
        return 'w';
      else if (unit == MONTH)
        return 'm';
      else
        throw new Failure("Unknown unit: " + unit);
    }

    @Override
    public BoolExpr<DP> afterExpr(DBAttribute<Date> attr) {
      assert !myLaterBound;
      return new RelativeDateDP<Date>(attr, myGetDateTime).term();
    }

    @Override
    public BoolExpr<DP> afterExprDay(DBAttribute<Integer> attr) {
      assert !myLaterBound;
      return new RelativeDateDP<Integer>(attr, myGetDay).term();
    }

    @Override
    public BoolExpr<DP> beforeExpr(DBAttribute<Date> attr) {
      assert myLaterBound;
      return new RelativeDateDP<Date>(attr, myGetDateTime).term();
     }

    @Override
    public BoolExpr<DP> beforeExprDay(DBAttribute<Integer> attr) {
      assert myLaterBound;
      return new RelativeDateDP<Integer>(attr, myGetDay).term();
    }

    @NotNull
    public DateUnit getRelativeUnit() {
      return myUnit;
    }

    public int getRelative() {
      return myValue;
    }

    public String toString() {
      return "RelativeDate(" + myValue + " " + myUnit.getDisplayName(myValue) + " ago" +
        (myLaterBound ? " ,later" : "") + ")";
    }

    @NotNull
    public static RelativeDate parseRelative(@NotNull DateUnit unit, @NotNull String relative, boolean laterBound)
      throws ParseException
    {
      try {
        return new RelativeDate(Integer.parseInt(relative), unit, laterBound);
      } catch (NumberFormatException e) {
        throw new ParseException("Not an integer: " + relative, 0);
      }
    }

    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      final RelativeDate that = (RelativeDate) o;

      if (myValue != that.myValue)
        return false;
      if (myUnit != null ? !myUnit.equals(that.myUnit) : that.myUnit != null)
        return false;

      return true;
    }

    public int hashCode() {
      int result;
      result = (myUnit != null ? myUnit.hashCode() : 0);
      result = 29 * result + myValue;
      return result;
    }

    private class RelativeDateDP<D extends Comparable<D>> extends DP {
      private final DBAttribute<D> myAttr;
      private final Function<Calendar, D> myGetValue;

      private RelativeDateDP(DBAttribute<D> attr, Function<Calendar, D> getValue) {
        myAttr = attr;
        myGetValue = getValue;
      }

      @Override
      public BoolExpr<DP> resolve(DBReader reader, @Nullable final ResolutionSubscription subscription) {
        if (subscription != null) {
          pokeSubscriptionOnNextUnitStart(reader, subscription);
        }
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(reader.getTransactionTime());
        D sample = myGetValue.invoke(now);
        return myLaterBound
          ? DPCompare.lessOrEqual(myAttr, sample, false)
          : DPCompare.greaterOrEqual(myAttr, sample, false);
      }

      private void pokeSubscriptionOnNextUnitStart(DBReader reader, final ResolutionSubscription subscription) {
        Lifespan subscriptionLife = subscription.getLife();
        if (subscriptionLife.isEnded()) return;

        final Calendar nextUpdate = Calendar.getInstance();
        nextUpdate.setTimeInMillis(reader.getTransactionTime());
        myUnit.subtract(nextUpdate, -1, false);
        final long nextUpdateTime = nextUpdate.getTimeInMillis() + 5 * Const.SECOND;

        Context.require(TimeService.ROLE).notifyOn(subscriptionLife, nextUpdateTime, ThreadGate.STRAIGHT, subscription);
//        updateSubscriptionViaSwingTimer(subscription, subscriptionLife, nextUpdateTime);
      }

      private void updateSubscriptionViaSwingTimer(final ResolutionSubscription subscription, Lifespan subscriptionLife, final long nextUpdateTime) {
        final javax.swing.Timer timer = new javax.swing.Timer(REFRESH_PERIOD, null);
        timer.addActionListener(new ActionListener() {
          long lastTime = 0L;
          @Override
          public void actionPerformed(ActionEvent e) {
            long now = System.currentTimeMillis();
            if (now >= nextUpdateTime || now < lastTime && lastTime > 0L) {
              // if time was switched back, update
              subscription.onChange();
              timer.stop();
            }
            lastTime = now;
          }
        });
        timer.setRepeats(true);
        timer.setCoalesce(true);
        timer.start();
        subscriptionLife.add(new Detach() {
          @Override
          protected void doDetach() throws Exception {
            timer.stop();
          }
        });
      }

      @Override
      public boolean accept(long item, DBReader reader) {
        Log.error(this + ": usage of unresolved DP");
        return false;
      }

      @Override
      protected boolean equalDP(DP other) {
        if (other == this) return true;
        if (!(other instanceof RelativeDateDP)) return false;
        RelativeDateDP otherDP = (RelativeDateDP) other;
        if (!getDate().equals(otherDP.getDate())) return false;
        return myLaterBound == otherDP.getDate().myLaterBound;
      }

      private RelativeDate getDate() {
        return RelativeDate.this;
      }

      @Override
      protected int hashCodeDP() {
        int h = RelativeDate.this.hashCode();
        h = 29 * h + Boolean.valueOf(myLaterBound).hashCode();
        h = 29 * h + myAttr.hashCode();
        return h;
      }

      @Override
      public String toString() {
        return (myLaterBound ? "before " : "later ") + getDate();
      }
    }

    private class GetDateTimeValue implements Function<Calendar, Date> {
      @Override
      public Date invoke(Calendar c) {
        return getValue(c);
      }
    }
    
    private class GetDayValue implements Function<Calendar, Integer> {
      @Override
      public Integer invoke(Calendar c) {
        return DateUtil.toDayNumberFromInstant(getValue(c));
      }
    }
  }


  public static class AbsoluteDate extends DateValue {
    private final Date myDate;
    public static final DateFormat USER_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());

    public AbsoluteDate(Date date) {
      myDate = date;
    }

    public Date getValue(Calendar calendar) {
      return (Date) myDate.clone();
    }

    public synchronized String getString() {
      try {
        return SERIALIZATION_FORMAT.format(myDate);
      } catch (Exception e) {
        // #800 Exception: AIOOBE in DateFormat.format()
        // http://bugzilla.almworks.com/show_bug.cgi?id=800
        try {
          Log.error("date: " + (myDate == null ? "null" : myDate.getTime()) + "; format: " + (
            SERIALIZATION_FORMAT instanceof SimpleDateFormat ? ((SimpleDateFormat) SERIALIZATION_FORMAT).toPattern() :
              String.valueOf(SERIALIZATION_FORMAT)));
        } catch (Exception ee){
          //
        }
        return "";
      }
    }

    public String getUserString() {
      return USER_FORMAT.format(myDate);
    }

    @Override
    public BoolExpr<DP> afterExpr(DBAttribute<Date> attr) {
      return DPCompare.greaterOrEqual(attr, getUptoDateValue(), false);
    }

    @Override
    public BoolExpr<DP> afterExprDay(DBAttribute<Integer> attr) {
      Date date = getUptoDateValue();
      int day = DateUtil.toDayNumberFromInstant(date);
      return DPCompare.greaterOrEqual(attr, day, false);
    }

    @Override
    public BoolExpr<DP> beforeExpr(DBAttribute<Date> attr) {
      return DPCompare.lessOrEqual(attr, getUptoDateValue(), false);
    }

    @Override
    public BoolExpr<DP> beforeExprDay(DBAttribute<Integer> attr) {
      Date date = getUptoDateValue();
      int day = DateUtil.toDayNumberFromInstant(date);
      return DPCompare.lessOrEqual(attr, day, false);
    }

    @Nullable
    public static DateValue fromDate(Date value) {
      return value == null ? null : new AbsoluteDate(value);
    }

    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      final AbsoluteDate that = (AbsoluteDate) o;

      if (!myDate.equals(that.myDate))
        return false;

      return true;
    }

    public int hashCode() {
      return myDate.hashCode();
    }

    public Date getDate() {
      return myDate;
    }
  }


  private static class Day extends DateUnit {
    public void subtract(Calendar calendar, int units, boolean laterBound) {
      calendar.add(Calendar.DAY_OF_MONTH, -units);
      correctDayTime(calendar, laterBound);
    }

    public int getDifference(Calendar later, Calendar earlier) {
      return getDayDifference(later, earlier);
    }

    protected String getDisplayName(int numberOfUnits) {
      return numberOfUnits != 1 ? L.special("days") : L.special("day");
    }
  }


  private static class Week extends DateUnit {
    private static final int DAYS_IN_WEEK = 7;

    public void subtract(Calendar calendar, int units, boolean laterBound) {
      int firstDayOfWeek = calendar.getFirstDayOfWeek();
      int thisWeekDays = calendar.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek;
      if (thisWeekDays < 0)
        thisWeekDays += DAYS_IN_WEEK;
      calendar.add(Calendar.DAY_OF_MONTH, -thisWeekDays - units * DAYS_IN_WEEK);
      if (laterBound)
        calendar.add(Calendar.DAY_OF_MONTH, DAYS_IN_WEEK - 1);
      correctDayTime(calendar, laterBound);
    }

    public int getDifference(Calendar later, Calendar earlier) {
      assert later.getFirstDayOfWeek() == earlier.getFirstDayOfWeek() : later + " " + earlier;
      boolean negate = false;
      if (later.before(earlier)) {
        Calendar tmp = later;
        later = earlier;
        earlier = tmp;
        negate = true;
      }
      int weeks = getDayDifference(later, earlier) / 7;
      if (getNormalizedDayOfWeek(later) < getNormalizedDayOfWeek(earlier))
        weeks++;
      if (negate)
        weeks = -weeks;
      return weeks;
//      int firstDayOfWeek = later.getFirstDayOfWeek();
//      int laterThisWeekDays = later.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek;
//      int earlierThisWeekDays = earlier.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek;
//      int dayDifference = getDayDifference(later, earlier);
//      dayDifference += -laterThisWeekDays + earlierThisWeekDays;
//      assert dayDifference % 7 == 0 : later.getTime() + " " + earlier.getTime();
//      return dayDifference / 7;
    }

    /**
     * @return 0 - first day of week, 6 - last day of week. If weeks starts on Monday then: Monday = 0, Sunday = 6.
     */
    private static int getNormalizedDayOfWeek(Calendar calendar) {
      int firstDay = calendar.getFirstDayOfWeek();
      return (calendar.get(Calendar.DAY_OF_WEEK) - firstDay + 7) % 7;
    }

    protected String getDisplayName(int numberOfUnits) {
      return numberOfUnits != 1 ? L.special("weeks") : L.special("week");
    }
  }


  private static class Month extends DateUnit {
    public void subtract(Calendar calendar, int units, boolean laterBound) {
      calendar.set(Calendar.DAY_OF_MONTH, 1);
      calendar.add(Calendar.MONTH, -units);
      if (laterBound) {
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
      }
      correctDayTime(calendar, laterBound);
    }

    public int getDifference(Calendar later, Calendar earlier) {
      int monthDiff = later.get(Calendar.MONTH) - earlier.get(Calendar.MONTH);
      int yearDiff = later.get(Calendar.YEAR) - earlier.get(Calendar.YEAR);
      return yearDiff * 12 + monthDiff;
    }

    protected String getDisplayName(int numberOfUnits) {
      return numberOfUnits != 1 ? L.special("months") : L.special("month");
    }
  }
}
