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

import java.net.URI;
import java.net.URISyntaxException;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import ca.luniv.afr.R;

public class AddFeedDialog extends Dialog {	
	public static interface OnFeedUrlSetListener {
		public void feedUrlSet(EditText view, String url);
	}
	
	private OnFeedUrlSetListener feedUrlSetListener;
	private EditText uriField;
	
	public AddFeedDialog(Context context, OnFeedUrlSetListener callback) {
		super(context);
		
		feedUrlSetListener = callback;
		
		setCancelable(true);
		setTitle(context.getString(R.string.add_feed_dialog_title));
		setContentView(R.layout.add_feed_dialog);
		
		uriField = (EditText) findViewById(R.id.feed_uri);
		uriField.append("http://");
		uriField.setKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_NEWLINE) {
					String text = isUriValid();
					if (text != null) {
						feedUrlSetListener.feedUrlSet(uriField, text);
						dismiss();
						return true;
					}
				}
				
				return false;
			}
		});

		View view = (View) findViewById(android.R.id.button1);
		view.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View button) {
				cancel();
			}
		});
		
		view = (View) findViewById(android.R.id.button2);
		view.setOnClickListener(new Button.OnClickListener() {		
			public void onClick(View button) {
				String uri = isUriValid();
				if (uri != null) {
					feedUrlSetListener.feedUrlSet(uriField, uri);
					dismiss();
				}
			}
		});
	}
	
	private String isUriValid() {
		String text = uriField.getText().toString();
		if (text.length() == 0) {
			return null;
		}
		
		try {
			URI uri = new URI(text);
			if (uri.getScheme() == null) {
				uri = new URI("http", "//" + uri.getSchemeSpecificPart(), uri.getFragment());
			}
			
			return uri.toString();
		} catch (URISyntaxException e) {
			getContext().showAlert(null, "The URI is invalid", "OK", false);
			return null;
		}
	}
}
