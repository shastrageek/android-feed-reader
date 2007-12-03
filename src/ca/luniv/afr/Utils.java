/*
 * $Id$
 *
 * Copyright (C) 2007 James Gilbertson <azurite@telusplanet.net>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */
package ca.luniv.afr;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.util.DateFormat;
import ca.luniv.afr.widget.ListSectionManager.Range;
import ca.luniv.afr.widget.ListSectionManager.Range.EndpointType;

public class Utils {
	public static final String[] DEFAULT_DATE_FORMATS = {
		"h:mm aa",
		"EEE h:mm aa",
		"EEE dd/MM",
		"dd/MM/yyyy"
	};
	
	/**
	 * Format a date depending on the difference between today's date and it. 
	 * The result will be formatted depending on the date difference: today, this/last week, or older
	 * @param formats the three format strings to use (today, this/last week, older)
	 * @param milliseconds the date in milliseconds
	 * @return the date formatted according to the date difference
	 */
	public static CharSequence formatDate(String formats[], long milliseconds) {
		// we just want date, not time	
		Calendar today = Calendar.getInstance();		
		today.set(Calendar.MILLISECOND, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.HOUR_OF_DAY, 0);
		
		Calendar posted = new GregorianCalendar();
		posted.setTimeInMillis(milliseconds);
		posted.set(Calendar.MILLISECOND, 0);
		posted.set(Calendar.SECOND, 0);
		posted.set(Calendar.MINUTE, 0);
		posted.set(Calendar.HOUR_OF_DAY, 0);
		
		long daysDiff = (today.getTimeInMillis() - posted.getTimeInMillis()) / (24*60*60*1000);
		int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
		
		if (daysDiff <= 7 + dayOfWeek) {
			if (daysDiff == 0) {
    			return DateFormat.format(formats[0], milliseconds);
			} else if (daysDiff <= dayOfWeek) {
    			return DateFormat.format(formats[1], milliseconds);
			} else {
    			return DateFormat.format(formats[2], milliseconds);
			}
		} else {
			return DateFormat.format(formats[3], milliseconds);
		}
	}
	
	/**
	 * See {@link Utils#formatDate(String[], long)} for details.
	 */
	public static CharSequence formatDate(String formats[], Date date) {
		return formatDate(formats, date.getTime());
	}
	
	public static List<Range<Long>> makeDateRanges() {
		ArrayList<Range<Long>> ranges = new ArrayList<Range<Long>>();
		// today
		Calendar date = Calendar.getInstance();		
		date.set(Calendar.MILLISECOND, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MINUTE, 0);
		date.set(Calendar.HOUR_OF_DAY, 0);
		
		// we always have today!
		long today = date.getTimeInMillis();
		ranges.add(new Range<Long>("Today", true, today, EndpointType.INCLUDED, 0l, EndpointType.INFINITE));
		
		// figure out how many sections we need for 
		int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
		long yesterday = today;
		
		// yesterday is a special case
		if (dayOfWeek > 0) {
			date.roll(Calendar.DAY_OF_YEAR, -1);
			yesterday = date.getTimeInMillis();
			ranges.add(new Range<Long>("Yesterday", true, yesterday, EndpointType.INCLUDED, today, EndpointType.EXCLUDED));
			dayOfWeek--;
		}
		
		// do the rest of the days in the week
		SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
		while (dayOfWeek > 0) {
			date.roll(Calendar.DAY_OF_YEAR, -1);
			today = yesterday;
			yesterday = date.getTimeInMillis();
			ranges.add(new Range<Long>(sdf.format(date.getTime()), true, yesterday, EndpointType.INCLUDED, today, EndpointType.EXCLUDED));
			dayOfWeek--;
		}
		
		date.roll(Calendar.DAY_OF_YEAR, -7);
		ranges.add(new Range<Long>("Last week", true, date.getTimeInMillis(), EndpointType.INCLUDED, yesterday, EndpointType.EXCLUDED));
		ranges.add(new Range<Long>("Older", true, 0l, EndpointType.INFINITE, date.getTimeInMillis(), EndpointType.EXCLUDED));
		
		return ranges;		
	}
}
