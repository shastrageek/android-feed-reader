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

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.webkit.WebView;
import ca.luniv.afr.R;
import ca.luniv.afr.provider.Afr;

public class EntryViewer extends Activity {	
	private long entryId;
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.entry_view);
        
        entryId = (Long) getIntent().getExtra(Afr.EntriesColumns._ID);
        Cursor c = managedQuery(Afr.Entries.CONTENT_URI.addId(entryId), null, null, null, null);
        c.first();
        
        setTitle(c.getString(c.getColumnIndex(Afr.Entries.TITLE)) + " - " + getText(R.string.app_shortname));
        
        // mark the item as read
        ContentValues values = new ContentValues();
        values.put(Afr.Entries.READ, Boolean.TRUE);
        getContentResolver().update(Afr.Entries.CONTENT_URI.addId(entryId), values, null, null);
        
        // TODO: template or make configurable
        StringBuilder post = new StringBuilder();
        post.append("<html>");
        post.append("<head>");
        post.append("<title>").append(c.getString(c.getColumnIndex(Afr.Entries.TITLE))).append("</title>");
        post.append("<style type=\"text/css\"> body { background-color: #201c19; color: white; } a { color: orange; } </style>");
        post.append("</head>");
        post.append("<body>");
        post.append(c.getString(c.getColumnIndex(Afr.Entries.CONTENT)));
        post.append("</body>");
        post.append("</html>");
        
        WebView webView = (WebView) findViewById(R.id.entry);
        // XXX: this doesn't work for some reason
        //webView.setBackground(null);
        webView.loadData(post.toString(), "text/html", "utf-8");
        
    }
}
