//**********************************************************************
// Package
//**********************************************************************

package org.freixas.jcalendar;

//**********************************************************************
// Import list
//**********************************************************************

import java.util.EventListener;

/**
 * Listen for date changes.
 *
 * @see JCalendar
 * @see JCalendarCombo
 * @author Antonio Freixas
 */

// Copyright (C) 2003 Antonio Freixas
// All Rights Reserved.

public interface DateListener
    extends EventListener
{

//**********************************************************************
// Public Constants
//**********************************************************************

//**********************************************************************
// Public
//**********************************************************************

/**
 * This method is called each time a date in a calendar changes.
 *
 * @param e The date event information.
 */

public void
dateChanged(
    DateEvent e);

}
