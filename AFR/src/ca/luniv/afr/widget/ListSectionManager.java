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
package ca.luniv.afr.widget;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewInflate;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import ca.luniv.afr.R;
import ca.luniv.afr.widget.SectionedListAdapter.ListSection;

public class ListSectionManager<T extends Comparable<T>> {
	public abstract static class ListSectionGroup<T extends Comparable<T>> {
		protected String name;
		protected boolean collapsible;
		
		public ListSectionGroup(String name) {
			this(name, false);
		}
		
		public ListSectionGroup(String name, boolean collapsible) {
			this.name = name;
			this.collapsible = collapsible;
		}
		
		public abstract boolean inSection(T value);

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isCollapsible() {
			return collapsible;
		}

		public void setCollapsible(boolean collapsible) {
			this.collapsible = collapsible;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder('{');
			sb.append("name: ").append(name).append(", ");
			sb.append("collapsible: ").append(collapsible).append('}');
			return sb.toString();
		}
	}
	
	public static class Group<T extends Comparable<T>> extends ListSectionGroup<T> {
		public T group;
		
		public Group(String name, T group) {
			super(name);
			this.group = group;
		}
		
		public Group(String name, boolean collapsible, T group) {
			super(name, collapsible);
			this.group = group;
		}
		
		@Override
		public boolean inSection(T value) {
			return value.compareTo(group) == 0;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(super.toString());
			sb.deleteCharAt(sb.length() - 1).append(", ");		
			sb.append("group: ").append(group).append("}");
			return sb.toString();
		}
	}
	
	public static class Range<T extends Comparable<T>> extends ListSectionGroup<T> {
		public enum EndpointType {
			EXCLUDED,
			INCLUDED,
			INFINITE
		}
		
		public T start;
		public EndpointType startType;
		public T end;
		public EndpointType endType;
		
		public Range(String name, T start, EndpointType startType, T end, EndpointType endType) {
			super(name);
			this.start = start;
			this.startType = startType;
			this.end = end;
			this.endType = endType;
		}
		
		public Range(String name, boolean collapsible, T start, EndpointType startType, T end, EndpointType endType) {
			super(name, collapsible);
			this.start = start;
			this.startType = startType;
			this.end = end;
			this.endType = endType;
		}
		
		@Override
		public boolean inSection(T value) {
			int comp = value.compareTo(start);
			switch (startType) {
			case EXCLUDED:
				if (comp <= 0) {
					return false;
				}
				break;
			case INCLUDED:
				if (comp < 0) {
					return false;
				}
				break;
			case INFINITE:
				break;
			}
			
			comp = value.compareTo(end);
			switch (endType) {
			case EXCLUDED:
				if (comp >= 0) {
					return false;
				}
				break;
			case INCLUDED:
				if (comp > 0) {
					return false;
				}
				break;
			case INFINITE:
				break;
			}
			
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(super.toString());
			sb.deleteCharAt(sb.length() - 1).append(", ");			
			sb.append("start: ").append(start).append(", ");
			sb.append("startType: ").append(startType).append(", ");
			sb.append("end: ").append(end).append(", ");
			sb.append("endType: ").append(endType).append('}');
			return sb.toString();
		}
	}
	
	protected Context context;
	protected Cursor cursor;
	protected List<? extends ListSectionGroup<T>> sectionGroups;
	
	protected Class<T> type;
	protected Constructor<T> typeConstructor;
	
	protected String columnName;
	protected int column;
	protected Method columnGetMethod;
	
	public ListSectionManager(Context context, String column, List<? extends ListSectionGroup<T>> sectionGroups, Class<T> type) {
		this.context = context;
		this.columnName = column;
		this.sectionGroups = sectionGroups;
		
		// figure out the right method to use
		String method;
		Class<?> columnType;
		if (type.equals(Double.class)) {
			method = "getDouble";
			columnType = Double.TYPE;
		} else if (type.equals(Float.class)) {
			method = "getFloat";
			columnType = Float.TYPE;
		} else if (type.equals(Integer.class)) {
			method = "getInt";
			columnType = Integer.TYPE;
		} else if (type.equals(Long.class)) {
			method = "getLong";
			columnType = Long.TYPE;
		} else if (type.equals(Short.class)) {
			method = "getShort";
			columnType = Short.TYPE;
		} else if (type.equals(String.class)) {
			method = "getString";
			columnType = String.class;
		} else {
			throw new IllegalArgumentException("type is not a type supported by android.database.Cursor");
		}
		
		// get the actual method object
		try {
			typeConstructor = type.getConstructor(columnType);
			columnGetMethod = Cursor.class.getMethod(method, Integer.TYPE);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("type's associated get method not found in android.database.Cursor", e);
		}
	}
	
	public ListSection[] getListSections(SectionedListAdapter adapter) {
		if (sectionGroups == null || cursor == null) {
			return null;
		}

		cursor.first();
		ArrayList<ListSection> sections = new ArrayList<ListSection>(sectionGroups.size());

		for (ListSectionGroup<T> sectionGroup : sectionGroups) {
			ListSection section = adapter.new ListSection();
			section.start = cursor.position() + sections.size();
			
			while (!cursor.isAfterLast()) {
				if (!sectionGroup.inSection(getValueFromCursor())) {
					break;
				}
				
				section.count++;
				cursor.next();
			}
			
			if (section.count > 0) {
				section.header = makeSectionHeaderView(section, sectionGroup);
				section.header.setTag(section);
				section.selectable = sectionGroup.collapsible;
				sections.add(section);
			}
		}
		
		return sections.toArray(new ListSection[sections.size()]);
	}

	public View makeSectionHeaderView(ListSection section, ListSectionGroup<T> sectionGroup) {
		ViewInflate inflater = (ViewInflate) context.getSystemService(Context.INFLATE_SERVICE);
		View layout = inflater.inflate(R.layout.list_section_header, null, null);
		
		TextView header = (TextView) layout.findViewById(R.id.list_section_header_title);
		header.setText(sectionGroup.name);
		header.setTextColor(Color.LTGRAY);
		header.setTypeface(Typeface.DEFAULT_BOLD);
		
		ImageView state = (ImageView) layout.findViewById(R.id.list_section_header_state);
		state.setImageDrawable(context.getResources().getDrawable(R.drawable.collapse));
		
		FrameLayout container = new FrameLayout(context) {
    		@Override
    		protected void dispatchDraw(Canvas canvas) {
    			Rect r = new Rect();
    			Paint p = new Paint();
    			
    			p.setStyle(Paint.Style.STROKE);
    			p.setStrokeWidth(2f);
    			p.setColor(Color.LTGRAY);
    			
    			getDrawingRect(r);
    			canvas.drawLine(r.left, r.bottom - 1, r.right, r.bottom - 1, p);
    		
    			super.dispatchDraw(canvas);
    		}
		};
		container.addView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		return container;
	}
	
	/**
	 * Returns the current row's section column value wrapped in its appropriate Java primitive wrapper class.
	 * @return wrapped primitive value of the current row's section column
	 */
	protected T getValueFromCursor() {
		// Wee, type contortions ahoy! This is equivalent to: 
		// T value = new T(cursor.get<primitive T>(column));
		try {
			return typeConstructor.newInstance(columnGetMethod.invoke(cursor, column));
		// none of these should happen, since we are only using Java primitive wrapper classes 
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		} catch (InstantiationException e) {
		}
		
		return null;
	}
	
	public Cursor getCursor() {
		return cursor;
	}
	
	public void setCursor(Cursor cursor) {
		this.cursor = cursor;
		if (cursor != null) {
			column = cursor.getColumnIndex(columnName);
		}
	}
}