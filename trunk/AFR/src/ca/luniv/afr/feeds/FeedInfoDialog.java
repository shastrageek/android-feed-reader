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

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import ca.luniv.afr.R;
import ca.luniv.afr.Utils;
import ca.luniv.afr.provider.dao.Feed;

public class FeedInfoDialog extends Dialog {
	private static final String[] formats = {
		"'Today at' h:mm aa",
		"EEEE 'at' h:mm aa",
		"EEEE dd/MM 'at' h:mm aa",
		"dd/MM/yyyy 'at' h:mm aa"
	};
	
	public FeedInfoDialog(Context context, long id) {
		super(context);
		
		Feed feed = new Feed(getContext().getContentResolver(), id);
		feed.load();
		
		setCancelable(true);
		setTitle(feed.getName());
		setContentView(R.layout.feed_info_dialog);
		
		TextView t = (TextView) findViewById(R.id.feed_uri);
		t.setText(feed.getUri().toString());
		
		t = (TextView) findViewById(R.id.feed_link);
		t.setText(feed.getLink().toString());
		
		t = (TextView) findViewById(R.id.feed_last_checked);
		t.setText(Utils.formatDate(formats, feed.getLastChecked()));
		
		View view = (View) findViewById(android.R.id.button1);
		view.setOnClickListener(new Button.OnClickListener() {		
			public void onClick(View button) {
				dismiss();
			}
		});
	}
}
