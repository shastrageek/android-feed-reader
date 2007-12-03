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

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class SectionedListAdapter extends BaseAdapter {
	private Context context;
	private Cursor cursor;
	
	private int rowIdColumn;
	
	private int count = -1;
	
	private ListSection[] sections;
	private ListSectionManager<? extends Comparable<?>> sectionManager;
	
	private boolean dataValid;
	private boolean autoRequery;
	private boolean autoRequeryInProgress;
	private ContentObserver contentObserver;
	private DataSetObserver dataSetObserver;
	
	private class MyDataSetObserver extends DataSetObserver {		
		@Override
		public void onChanged() {
			dataValid = true;
			count = -1;
			
			if (!autoRequeryInProgress) {	
				if (sectionManager != null) {
					makeListSections();
				}
				
				notifyDataSetChanged();
			}
		}
		
		@Override
		public void onInvalidated() {
			dataValid = false;
			count = -1;
			
			notifyDataSetInvalidated();
		}
	}
	
	private class MyContentObserver extends ContentObserver {
		public MyContentObserver() {
			super(new Handler());
		}
		
		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}
		
		@Override
		public void onChange(boolean selfChange) {
			if (autoRequery) {
				autoRequeryInProgress = true;
				cursor.requery();
				autoRequeryInProgress = false;
			}

			notifyChange(selfChange);
		}
	}
	
	public final class ListSection {
		protected View header;
		protected boolean selectable;
		protected boolean collapsed;
		protected int start;
		protected int count;

		public View getHeader() {
			return header;
		}

		public void setHeader(View header) {
			this.header = header;
		}

		public boolean isSelectable() {
			return selectable;
		}

		public void setSelectable(boolean selectable) {
			this.selectable = selectable;
		}

		public boolean isCollapsed() {
			return collapsed;
		}

		public void setCollapsed(boolean collapsed) {
			this.collapsed = collapsed;
			// we need to recalculate the count
			computeCount();
			notifyChange(true);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder().append('{');
			sb.append("header: ").append(header).append(", ");
			sb.append("selectable: ").append(selectable).append(", ");
			sb.append("collapsed: ").append(collapsed).append(", ");
			sb.append("start: ").append(start).append(", ");
			sb.append("count: ").append(count).append('}');
			return sb.toString();
		}
	}
	
	public <T extends Comparable<T>> SectionedListAdapter(Context context, Cursor cursor, ListSectionManager<T> sectionManager) {
		this(context, cursor, true, sectionManager);
	}
	
	public <T extends Comparable<T>> SectionedListAdapter(Context context, Cursor cursor, boolean autoRequery, ListSectionManager<T> sectionManager) {
		super();
		
		this.context = context;
		this.cursor = cursor;
		this.autoRequery = autoRequery;
		this.sectionManager = sectionManager;
		
		if (cursor != null) {
			dataValid = true;
			rowIdColumn = cursor.getColumnIndex(BaseColumns._ID);
			count = -1;
			
			contentObserver = new MyContentObserver();
			cursor.registerContentObserver(contentObserver);
			dataSetObserver = new MyDataSetObserver();
			cursor.registerDataSetObserver(dataSetObserver);
			
			if (sectionManager != null) {
				sectionManager.setCursor(cursor);
				makeListSections();
			}
		} else {
			dataValid = false;
			rowIdColumn = -1;
			count = -1;
		}
	}
	
	public void changeCursor(Cursor cursor) {
		if (this.cursor != null) {
			cursor.unregisterContentObserver(contentObserver);
			cursor.unregisterDataSetObserver(dataSetObserver);
		}
		
		if (sectionManager != null) {
			sectionManager.setCursor(cursor);
		}

		this.cursor = cursor;
		if (cursor != null) {
			dataValid = true;
			rowIdColumn = cursor.getColumnIndex(BaseColumns._ID);
			count = -1;
			
			cursor.registerContentObserver(contentObserver);
			cursor.registerDataSetObserver(dataSetObserver);
			
			notifyDataSetChanged();
		} else {
			dataValid = false;
			rowIdColumn = -1;
			count = -1;
			
			notifyDataSetInvalidated();
		}
	}
	
	public void changeListSectionManager(ListSectionManager<? extends Comparable<?>> sectionManager, Cursor cursor) {
		this.sectionManager = sectionManager;
		this.sections = null;
		
		if (sectionManager != null) {
			sectionManager.setCursor(cursor);
		}
		
		changeCursor(cursor);
	}
	
	private void makeListSections() {
		sections = sectionManager.getListSections(this);
	}
	
	@Override
	public boolean areAllItemsSelectable() {
		return false;
	}

	@Override
	public boolean isSelectable(int position) {
		if (dataValid && cursor != null && count > 0) {
			if (sections != null) {
				int currentSection = 0;
				
				for (ListSection section : sections) {
					currentSection++;
					
					if (position == section.start) {
						return section.selectable;
					} 
					
					// if this section is collapsed, adjust the position and move to the next
					if (section.collapsed) {
						position += section.count;
						continue;
					}
					
					if (position <= section.start + section.count) {
						return true;
					}
				}
			} else {
				return true;
			}
		}
		
		return false;
	}
	
	public int getCount() {
		if (count != -1) {
			return count;
		}
		
		computeCount();
		
		return count;
	}
	
	protected void computeCount() {
		count = 0;
		if (dataValid && cursor != null) {
			if (sections != null) {
				for (ListSection section : sections) {
					count += section.collapsed ? 1 : 1 + section.count;
				}
			} else {
				count = cursor.count();
			}
		}
		
		Log.d("AFR", "count is now: " + count);
	}

	public Object getItem(int position) {		
		if (dataValid && cursor != null && count > 0) {
			if (sections != null) {
				int currentSection = 0;
				
				for (ListSection section : sections) {
					currentSection++;
					
					if (position == section.start) {
						return section;
					} 
					
					// if this section is collapsed, adjust the position and move to the next
					if (section.collapsed) {
						position += section.count;
						continue;
					}
					
					if (position <= section.start + section.count) {
						cursor.moveTo(position - currentSection);
						return cursor;
					}
				}
			} else {
				cursor.moveTo(position);
				return cursor;
			}
		}
		
		return null;
	}
	
	public long getItemId(int position) {
		Object o = getItem(position);
		if (o == cursor) {
			return cursor.getLong(rowIdColumn);
		}
		
		return 0;
	}

	@Override
	public boolean stableIds() {
		return true;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		if (!dataValid) {
			throw new IllegalStateException("this should only be called when the cursor is valid");
		}
		
		Object o = getItem(position);
		// the position is a regular list item
		if (o == cursor) {
			// we have an old view? make sure it's not a section header
			if (convertView != null && !(convertView.getTag() instanceof ListSection)) {
				bindView(context, cursor, convertView);
				return convertView;
			} else {
				return newView(context, cursor, parent);
			}
		}
		
		// the position is a section header
		return ((ListSection) o).header;
	}
	
	public abstract void bindView(Context context, Cursor cursor, View view);
	public abstract View newView(Context context, Cursor cursor, ViewGroup parent);
}
