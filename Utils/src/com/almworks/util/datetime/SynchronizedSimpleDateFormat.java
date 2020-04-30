package com.almworks.util.datetime;

import java.text.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SynchronizedSimpleDateFormat extends SimpleDateFormat {
  public SynchronizedSimpleDateFormat() {
  }

  public SynchronizedSimpleDateFormat(String pattern) {
    super(pattern);
  }

  public SynchronizedSimpleDateFormat(String pattern, DateFormatSymbols formatSymbols) {
    super(pattern, formatSymbols);
  }

  public SynchronizedSimpleDateFormat(String pattern, Locale locale) {
    super(pattern, locale);
  }

  public synchronized void applyLocalizedPattern(String pattern) {
    super.applyLocalizedPattern(pattern);
  }

  public synchronized void applyPattern(String pattern) {
    super.applyPattern(pattern);
  }

  public synchronized Object clone() {
    return super.clone();
  }

  public synchronized boolean equals(Object obj) {
    return super.equals(obj);
  }

  public synchronized StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
    return super.format(date, toAppendTo, pos);
  }

  public synchronized AttributedCharacterIterator formatToCharacterIterator(Object obj) {
    return super.formatToCharacterIterator(obj);
  }

  public synchronized Date get2DigitYearStart() {
    return super.get2DigitYearStart();
  }

  public synchronized DateFormatSymbols getDateFormatSymbols() {
    return super.getDateFormatSymbols();
  }

  public synchronized int hashCode() {
    return super.hashCode();
  }

  public synchronized Date parse(String text, ParsePosition pos) {
    return super.parse(text, pos);
  }

  public synchronized void set2DigitYearStart(Date startDate) {
    super.set2DigitYearStart(startDate);
  }

  public synchronized void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
    super.setDateFormatSymbols(newFormatSymbols);
  }

  public synchronized String toLocalizedPattern() {
    return super.toLocalizedPattern();
  }

  public synchronized String toPattern() {
    return super.toPattern();
  }

  public synchronized Calendar getCalendar() {
    return super.getCalendar();
  }

  public synchronized NumberFormat getNumberFormat() {
    return super.getNumberFormat();
  }

  public synchronized TimeZone getTimeZone() {
    return super.getTimeZone();
  }

  public synchronized boolean isLenient() {
    return super.isLenient();
  }

  public synchronized Date parse(String source) throws ParseException {
    return super.parse(source);
  }

  public synchronized Object parseObject(String source, ParsePosition pos) {
    return super.parseObject(source, pos);
  }

  public synchronized void setCalendar(Calendar newCalendar) {
    super.setCalendar(newCalendar);
  }

  public synchronized void setLenient(boolean lenient) {
    super.setLenient(lenient);
  }

  public synchronized void setNumberFormat(NumberFormat newNumberFormat) {
    super.setNumberFormat(newNumberFormat);
  }

  public synchronized void setTimeZone(TimeZone zone) {
    super.setTimeZone(zone);
  }

  public synchronized Object parseObject(String source) throws ParseException {
    return super.parseObject(source);
  }
}
