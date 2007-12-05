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
package ca.luniv.afr.entries;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ContentURI;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewInflate;
import android.view.Menu.Item;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import ca.luniv.afr.Prefs;
import ca.luniv.afr.R;
import ca.luniv.afr.Utils;
import ca.luniv.afr.Prefs.HourFormat;
import ca.luniv.afr.provider.Afr;
import ca.luniv.afr.widget.ListSectionManager;
import ca.luniv.afr.widget.SectionedListAdapter;
import ca.luniv.afr.widget.ListSectionManager.Group;
import ca.luniv.afr.widget.ListSectionManager.Range;
import ca.luniv.afr.widget.SectionedListAdapter.ListSection;

public class Entries extends ListActivity {
	private static final String[] itemsProjection = {
		Afr.Entries._ID,
		Afr.Entries.AUTHOR,
		Afr.Entries.DATE,
		Afr.Entries.TITLE,
		Afr.Entries.READ
	};
	
    public static final int SORT_DATE = Menu.FIRST;
    public static final int SORT_AUTHOR = Menu.FIRST + 1;
	
    private long feedId;
    private Cursor cursor;
    private HourFormat hourFormat;
    
	@Override
    public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		String name = (String) getIntent().getExtra(Afr.Feeds.NAME);
		if (name != null) {
			setTitle(name + " - " + getText(R.string.app_shortname));
		}
		feedId = (Long) getIntent().getExtra(Afr.Feeds._ID);
		
        getListView().setSelector(R.drawable.list_highlight_background_blue);

        // 12 or 24 hour format?
        hourFormat = HourFormat.values()[getPreferences(0).getInt(Prefs.FORMAT_HOURS, 0)];
        
		// make the list sections
		List<Range<Long>> ranges = Utils.makeDateRanges();
		
		ContentURI queryURI = Afr.Feeds.CONTENT_URI.addId(feedId).addPath("entries");
		
        cursor = managedQuery(queryURI, itemsProjection, null, null, Afr.Entries.DEFAULT_SORT_ORDER);
        
        setListAdapter(new ItemsListAdapter(this, cursor, new ListSectionManager<Long>(this, Afr.Entries.DATE, ranges, Long.class)));
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	if (v.getTag() instanceof ListSection) {
    		ListSection section = (ListSection) v.getTag();
    		section.setCollapsed(!section.isCollapsed());

			ImageView state = (ImageView) v.findViewById(R.id.list_section_header_state);
    		if (!section.isCollapsed()) {
    			state.setImageDrawable(getResources().getDrawable(R.drawable.collapse));
    		} else {
    			state.setImageDrawable(getResources().getDrawable(R.drawable.expand));
    		}
    		
    		return;
    	}
    	
    	Intent intent = new Intent(this, EntryViewer.class);
    	intent.putExtra(Afr.Feeds._ID, id);
    	startSubActivity(intent, 0);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);
        final boolean haveItems = cursor.count() > 0;

    	menu.removeGroup(Menu.SELECTED_ALTERNATIVE);
        if (haveItems) {
            menu.add(Menu.SELECTED_ALTERNATIVE, SORT_AUTHOR, "Sort by author");
            menu.add(Menu.SELECTED_ALTERNATIVE, SORT_DATE, "Sort by date");
        }
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(Item item) {
		ContentURI queryURI = Afr.Feeds.CONTENT_URI.addId(feedId).addPath("entries");
		
    	switch (item.getId()) {
    	case SORT_DATE: {
    		List<Range<Long>> ranges = Utils.makeDateRanges();

            cursor = managedQuery(queryURI, itemsProjection, null, null, Afr.Entries.DEFAULT_SORT_ORDER);
            
            setListAdapter(new ItemsListAdapter(this, cursor, 
            		new ListSectionManager<Long>(this, Afr.Entries.DATE, ranges, Long.class)));
    	}
    	break;
    	case SORT_AUTHOR: {
    		List<Group<String>> groups = makeAuthorGroups();

            cursor = managedQuery(queryURI, itemsProjection, null, null, 
            		"lower(" + Afr.EntriesColumns.AUTHOR + ") ASC, " + Afr.EntriesColumns.DATE + " DESC");
            
            setListAdapter(new ItemsListAdapter(this, cursor, 
            		new ListSectionManager<String>(this, Afr.Entries.AUTHOR, groups, String.class)));
    	}
    	break;
    	}
    	
        return super.onOptionsItemSelected(item);
    }
    
    private List<Group<String>> makeAuthorGroups() {
    	ArrayList<Group<String>> groups = new ArrayList<Group<String>>();
		
		ContentURI queryURI = Afr.Feeds.CONTENT_URI.addId(feedId).addPath("entries/authors");
    	
		Cursor cursor = managedQuery(queryURI, new String[] { Afr.AuthorsColumns.AUTHOR }, null, 
				"lower(" + Afr.EntriesColumns.AUTHOR + ") ASC");
		
    	cursor.first();
    	for (cursor.first(); !cursor.isAfterLast(); cursor.next()) {
    		groups.add(new Group<String>(cursor.getString(0), true, cursor.getString(0)));
    	}
    	cursor.close();
    	
    	return groups;
    }
    
    public class ItemsListAdapter extends SectionedListAdapter {
    	public <T extends Comparable<T>> ItemsListAdapter(Context context, Cursor cursor, ListSectionManager<T> sectionManager) {
    		super(context, cursor, sectionManager);
    	}

    	@Override
    	public void bindView(Context context, Cursor cursor, View view) {
    		boolean read = cursor.getInt(4) != 0;
    		
    		view.setPadding(10, 0, 10, 0);
    		
    		// Description/title
    		TextView t = (TextView) view.findViewById(R.id.item_description);
    		t.setText(cursor.getString(3));
    		if (read) {
    			t.setTextColor(0xffaaaaaa);
    		} else {
    			t.setTextColor(Color.WHITE);
    		}
    		
    		// Date
    		t = (TextView) view.findViewById(R.id.item_time);
    		switch (hourFormat) {
			case Hours_12:
	    		t.setText(Utils.formatDate(Utils.DEFAULT_12H_DATE_FORMATS, cursor.getLong(2)));
				break;
			case Hours_24:
	    		t.setText(Utils.formatDate(Utils.DEFAULT_24H_DATE_FORMATS, cursor.getLong(2)));
				break;
			}
    		
    		if (read) {
    			t.setTextColor(0xffaaaaaa);
    			t.setTypeface(Typeface.DEFAULT);
    		} else {
    			t.setTextColor(Color.WHITE);
    			t.setTypeface(Typeface.DEFAULT_BOLD);
    		}

    		// Author    		    		
    		t = (TextView) view.findViewById(R.id.item_author);
    		t.setText(cursor.getString(1));
    		if (read) {
    			t.setTextColor(0xffaaaaaa);
    			t.setTypeface(Typeface.DEFAULT);
    		} else {
    			t.setTextColor(Color.WHITE);
    			t.setTypeface(Typeface.DEFAULT_BOLD);
    		}
    	}

    	@Override
    	public View newView(Context context, Cursor cursor, ViewGroup parent) {		
    		ViewInflate inflater = (ViewInflate) context.getSystemService(Context.INFLATE_SERVICE);
    		View view = inflater.inflate(R.layout.entry_row, null, null);
    		
    		FrameLayout container = new FrameLayout(context) {
        		@Override
        		protected void dispatchDraw(Canvas canvas) {
        			Rect r = new Rect();
        			Paint p = new Paint();
        			
        			p.setStyle(Paint.Style.STROKE);
        			p.setStrokeWidth(0);
        			p.setColor(Color.DKGRAY);
        			
        			getDrawingRect(r);
        			canvas.drawLine(r.left, r.bottom - 1, r.right, r.bottom - 1, p);
        		
        			super.dispatchDraw(canvas);
        		}
    		};
    		container.addView(view, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    		
    		// let bindView do the work of filling it out
    		bindView(context, cursor, container);
    		
    		return container;
    	}
    }
}