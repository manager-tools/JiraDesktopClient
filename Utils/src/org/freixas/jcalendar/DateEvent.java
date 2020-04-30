//**********************************************************************
// Package
//**********************************************************************

package org.freixas.jcalendar;

//**********************************************************************
// Import list
//**********************************************************************

import java.util.Calendar;
import java.util.EventObject;

/**
 * This class holds information related to a date change in a
 * calendar.
 *
 * @see JCalendar
 * @see JCalendarCombo
 * @author Antonio Freixas
 */

// Copyright (C) 2003 Antonio Freixas
// All Rights Reserved.

public class DateEvent
    extends EventObject
{

//**********************************************************************
// Private Members
//**********************************************************************

private Calendar selectedDate;

//**********************************************************************
// Constructors
//**********************************************************************

/**
 * Create a date event.
 *
 * @param source The object on which the event occurred.
 * @param selectedDate The selected date.
 */

public
DateEvent(
    Object source,
    Calendar selectedDate)
{
    super(source);
    this.selectedDate = selectedDate;
}

//**********************************************************************
// Public
//**********************************************************************

/**
 * Return the selected date.
 *
 * @return The selected date.
 */

public Calendar
getSelectedDate()
{
    return selectedDate;
}

//The default equals and hashCode methods are acceptable.

/**
 * {@inheritDoc}
 */

public String
toString()
{
    return
	super.toString() + ",selectedDate=" +
	selectedDate.getTime().toString();
}

}
