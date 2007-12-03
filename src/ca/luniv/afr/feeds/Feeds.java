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
package ca.luniv.afr.feeds;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ContentURI;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewInflate;
import android.view.Menu.Item;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import ca.luniv.afr.R;
import ca.luniv.afr.entries.Entries;
import ca.luniv.afr.provider.Afr;
import ca.luniv.afr.service.FeedRetrieverService;
import ca.luniv.afr.service.IFeedRetrieverService;
import ca.luniv.afr.widget.ListSectionManager;
import ca.luniv.afr.widget.SectionedListAdapter;

public class Feeds extends ListActivity {
    public static final int NEW_FEED_ID = Menu.FIRST;
    public static final int UPDATE_FEED_ID = Menu.FIRST + 1;
    public static final int DELETE_FEED_ID = Menu.FIRST + 2;
    public static final int UPDATE_ALL_FEEDS_ID = Menu.FIRST + 3;
    public static final int FEED_INFO = Menu.FIRST + 4;
    
    public static final int DIALOG_ID = Menu.FIRST + 2;
    
    private IFeedRetrieverService feedRetrieverService;
    private ServiceConnection feedRetrieverConnection = new ServiceConnection() {
    	public void onServiceConnected(ComponentName name, IBinder service) {
    		feedRetrieverService = IFeedRetrieverService.Stub.asInterface(service);
    	}
    	public void onServiceDisconnected(ComponentName name) {
    		feedRetrieverService = null;
    	}
    };
    
    private Cursor cursor;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        // bind to our feed retriever
        bindService(new Intent(this, FeedRetrieverService.class), null, feedRetrieverConnection, Context.BIND_AUTO_CREATE);
        
        // set up the list
        cursor = managedQuery(Afr.Feeds.CONTENT_URI, listProjection, null, Afr.Feeds.DEFAULT_SORT_ORDER);
        getListView().setSelector(R.drawable.list_highlight_background_blue);
        setListAdapter(new FeedsListAdapter(this, cursor));
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
    	Intent intent = new Intent(this, Entries.class);
    	intent.putExtra(Afr.Feeds._ID, id);
    	intent.putExtra(Afr.Feeds.NAME, ((TextView) v.findViewById(R.id.feed_title)).getText());
    	startActivity(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, NEW_FEED_ID, R.string.feeds_new);
        return result;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);
        final boolean haveItems = cursor.count() > 0;

    	menu.removeGroup(Menu.SELECTED_ALTERNATIVE);
        if (haveItems) {
            menu.add(Menu.SELECTED_ALTERNATIVE, UPDATE_FEED_ID, R.string.feeds_refresh);
            menu.add(Menu.SELECTED_ALTERNATIVE, FEED_INFO, R.string.feeds_info);
            menu.add(Menu.SELECTED_ALTERNATIVE, DELETE_FEED_ID, R.string.feeds_delete);
            menu.addSeparator(Menu.SELECTED_ALTERNATIVE, 0);
            menu.add(Menu.SELECTED_ALTERNATIVE, UPDATE_ALL_FEEDS_ID, R.string.feeds_refresh_all);
        }
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(Item item) {
    	switch (item.getId()) {
    	case NEW_FEED_ID:
    		new AddFeedDialog(this, new AddFeedDialog.OnFeedUrlSetListener() {
    			public void feedUrlSet(EditText view, String uri) {
    				try {
    					feedRetrieverService.retrieveURI(uri);
    				} catch (DeadObjectException ex) {
    					// won't happen, because if we get this, we're dead anyway...
    				}
    			}
    		}).show();
    		break;
    	case DELETE_FEED_ID:
    		getContentResolver().delete(Afr.Feeds.CONTENT_URI.addId(getSelectionRowID()), null, null);
    		break;
    	case UPDATE_FEED_ID:
			try {
		    	feedRetrieverService.retrieveFeed(getSelectionRowID());
			} catch (DeadObjectException ex) {
				// won't happen, because if we get this, we're dead anyway...
			}
    		break;
    	case UPDATE_ALL_FEEDS_ID:
			try {
		    	feedRetrieverService.retrieveAllFeeds();
			} catch (DeadObjectException ex) {
				// won't happen, because if we get this, we're dead anyway...
			}
    		break;
    	case FEED_INFO:
    		new FeedInfoDialog(this, getSelectionRowID()).show();
    		break;
    	}
    	
        return super.onOptionsItemSelected(item);
    }
    
	private static final String[] listProjection = {
		Afr.Feeds._ID,
		Afr.Feeds.NAME
	};
	
	private static final String[] itemCountProjection = {
		Afr.Entries._ID,
		Afr.Entries.READ
	};
	
    private class FeedsListAdapter extends SectionedListAdapter {    	
    	public FeedsListAdapter(Context context, Cursor cursor) {
    		super(context, cursor, (ListSectionManager<Long>) null);
    	}
    	
    	@Override
    	public void bindView(Context context, Cursor cursor, View view) {
    		view.setPadding(10, 0, 10, 0);
    		
    		TextView name = (TextView) view.findViewById(R.id.feed_title);
    		name.setText(cursor.getString(1));
    		
    		// query and compute the counts
    		ContentURI queryURI = Afr.Feeds.CONTENT_URI.addId(cursor.getLong(0)).addPath("entries/filter");

    		int read = 0, unread = 0;
    		
    		Cursor c = managedQuery(queryURI.addPath("unread"), itemCountProjection, null, null);
    		unread = c.count();
    		c.close();
    		
    		c = managedQuery(queryURI.addPath("read"), itemCountProjection, null, null);
    		read = c.count();
    		c.close();
    				
    		TextView counts = (TextView) view.findViewById(R.id.feed_counts);
    		counts.setText(Integer.toString(unread) + "/" + Integer.toString(unread + read));
    		
    		// display the feed with bold typeface if unread items
    		if (unread == 0) {
    			name.setTextColor(Color.LTGRAY);
    			name.setTypeface(Typeface.DEFAULT);
    			counts.setTextColor(Color.LTGRAY);
    			counts.setTypeface(Typeface.DEFAULT);
    		} else {
    			name.setTextColor(Color.WHITE);
    			name.setTypeface(Typeface.DEFAULT_BOLD);
    			counts.setTextColor(Color.WHITE);
    			counts.setTypeface(Typeface.DEFAULT_BOLD);
    		}
    	}

    	@Override
    	public View newView(Context context, Cursor cursor, ViewGroup parent) {		
    		ViewInflate inflater = (ViewInflate) context.getSystemService(Context.INFLATE_SERVICE);
    		View view = inflater.inflate(R.layout.feed_row, null, null);
    		
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