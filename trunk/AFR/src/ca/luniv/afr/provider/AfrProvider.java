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
package ca.luniv.afr.provider;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import android.content.ContentURIParser;
import android.content.ContentValues;
import android.content.DatabaseContentProvider;
import android.content.QueryBuilder;
import android.database.Cursor;
import android.database.SQLException;
import android.net.ContentURI;
import android.text.TextUtils;
import android.util.Log;

public class AfrProvider extends DatabaseContentProvider {	
	private static final String TAG = "AfrProvider";
	private static final String DATABASE_NAME = "afr.db";
	private static final int DATABASE_VERSION = 2;

	private static enum URIPatternIds {
		FEEDS,
		FEED_ID,
		FEED_CATEGORIES,
		FEED_ENTRIES,
		FEED_ENTRIES_AUTHORS,
		FEED_ENTRIES_CATEGORIES,
		FEED_ENTRIES_FILTER_AUTHOR,
		FEED_ENTRIES_FILTER_CATEGORY,
		FEED_ENTRIES_FILTER_READ,
		FEED_ENTRIES_FILTER_UNREAD,
		FEED_ENTRY_ID,
		FEED_ENTRY_CATEGORIES,
		
		ENTRIES,
		ENTRY_ID,
		ENTRY_CATEGORIES,
		
		CATEGORIES,
		CATEGORY_ID,
		CATEGORY_FEEDS,
		CATEGORY_ENTRIES,
		
		FEED_URI,
		ENTRY_URI;
		
		public static URIPatternIds get(int ordinal) {
			return values()[ordinal];
		}
	};
	
    private static final ContentURIParser URI_MATCHER;

    private static final HashMap<String, String> feedsColumnMap;
    private static final HashMap<String, String> entriesColumnMap;
    private static final HashMap<String, String> categoriesColumnMap;
    
    static {
        URI_MATCHER = new ContentURIParser(ContentURIParser.NO_MATCH);

        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds", URIPatternIds.FEEDS.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#", URIPatternIds.FEED_ID.ordinal());

        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#/categories", URIPatternIds.FEED_CATEGORIES.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#/entries", URIPatternIds.FEED_ENTRIES.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#/entries/authors", URIPatternIds.FEED_ENTRIES_AUTHORS.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#/entries/categories", URIPatternIds.FEED_ENTRIES_CATEGORIES.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#/entries/#", URIPatternIds.FEED_ENTRY_ID.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#/entries/#/categories", URIPatternIds.FEED_ENTRY_CATEGORIES.ordinal());

        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#/entries/filter/author/*", URIPatternIds.FEED_ENTRIES_FILTER_AUTHOR.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#/entries/filter/category/*", URIPatternIds.FEED_ENTRIES_FILTER_CATEGORY.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#/entries/filter/read", URIPatternIds.FEED_ENTRIES_FILTER_READ.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/#/entries/filter/unread", URIPatternIds.FEED_ENTRIES_FILTER_UNREAD.ordinal());
        
        URI_MATCHER.addURI(Afr.AUTHORITY, "entries", URIPatternIds.ENTRIES.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "entries/#", URIPatternIds.ENTRY_ID.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "entries/#/categories", URIPatternIds.ENTRY_CATEGORIES.ordinal());
        
        URI_MATCHER.addURI(Afr.AUTHORITY, "categories", URIPatternIds.CATEGORIES.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "categories/#", URIPatternIds.CATEGORY_ID.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "categories/#/feeds", URIPatternIds.CATEGORY_FEEDS.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "categories/#/entries", URIPatternIds.CATEGORY_ENTRIES.ordinal());
        
        // used by the feed retriever service when updating feeds
        URI_MATCHER.addURI(Afr.AUTHORITY, "feeds/uri/*", URIPatternIds.FEED_URI.ordinal());
        URI_MATCHER.addURI(Afr.AUTHORITY, "entries/uri/*", URIPatternIds.ENTRY_URI.ordinal());

		feedsColumnMap = new HashMap<String, String>();
		feedsColumnMap.put(Afr.FeedsColumns._ID, "f." + Afr.FeedsColumns._ID);
		feedsColumnMap.put(Afr.FeedsColumns.URI, "f." + Afr.FeedsColumns.URI);
		feedsColumnMap.put(Afr.FeedsColumns.NAME, "f." + Afr.FeedsColumns.NAME);
		feedsColumnMap.put(Afr.FeedsColumns.LINK, "f." + Afr.FeedsColumns.LINK);
		feedsColumnMap.put(Afr.FeedsColumns.EXPIRE_AFTER, "f." + Afr.FeedsColumns.EXPIRE_AFTER);
		feedsColumnMap.put(Afr.FeedsColumns.LAST_CHECKED, "f." + Afr.FeedsColumns.LAST_CHECKED);
		feedsColumnMap.put(Afr.FeedsColumns.LAST_MODIFIED, "f." + Afr.FeedsColumns.LAST_MODIFIED);
		feedsColumnMap.put(Afr.FeedsColumns.ETAG, "f." + Afr.FeedsColumns.ETAG);
		
		entriesColumnMap = new HashMap<String, String>();
		entriesColumnMap.put(Afr.EntriesColumns._ID, "e." + Afr.EntriesColumns._ID);
		entriesColumnMap.put(Afr.EntriesColumns.FEED, "e." + Afr.EntriesColumns.FEED);
		entriesColumnMap.put(Afr.EntriesColumns.URI, "e." + Afr.EntriesColumns.URI);
		entriesColumnMap.put(Afr.EntriesColumns.TITLE, "e." + Afr.EntriesColumns.TITLE);
		entriesColumnMap.put(Afr.EntriesColumns.AUTHOR, "e." + Afr.EntriesColumns.AUTHOR);
		entriesColumnMap.put(Afr.EntriesColumns.DATE, "e." + Afr.EntriesColumns.DATE);
		entriesColumnMap.put(Afr.EntriesColumns.LINK, "e." + Afr.EntriesColumns.LINK);
		entriesColumnMap.put(Afr.EntriesColumns.CONTENT, "e." + Afr.EntriesColumns.CONTENT);
		entriesColumnMap.put(Afr.EntriesColumns.TYPE, "e." + Afr.EntriesColumns.TYPE);
		entriesColumnMap.put(Afr.EntriesColumns.READ, "e." + Afr.EntriesColumns.READ);
		
		categoriesColumnMap = new HashMap<String, String>();
		categoriesColumnMap.put(Afr.CategoriesColumns._ID, "c." + Afr.CategoriesColumns._ID);
		categoriesColumnMap.put(Afr.CategoriesColumns.NAME, "c." + Afr.CategoriesColumns.NAME);
    }
    
    public AfrProvider() {
    	super(DATABASE_NAME, DATABASE_VERSION);
    }

	@Override
	protected void bootstrapDatabase() {
		mDb.execSQL(Afr.Feeds.SQL.create);
		mDb.execSQL(Afr.Feeds.SQL.create_item_trigger);
		mDb.execSQL(Afr.Feeds.SQL.create_category_trigger);
		mDb.execSQL(Afr.Entries.SQL.create);
		mDb.execSQL(Afr.Entries.SQL.create_category_trigger);
		mDb.execSQL(Afr.Categories.SQL.create);
		mDb.execSQL(Afr.Categories.SQL.create_category_trigger);
		mDb.execSQL(Afr.CategoryLinks.SQL.create);

        Log.i(TAG, "Database created, schema version " + DATABASE_VERSION);
		mDb.setVersion(DATABASE_VERSION);
	}
	
	@Override
	protected void upgradeDatabase(int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        
        mDb.execSQL(Afr.Feeds.SQL.delete);
        mDb.execSQL(Afr.Entries.SQL.delete);
		mDb.execSQL(Afr.Categories.SQL.delete);
		mDb.execSQL(Afr.CategoryLinks.SQL.delete);
		
		bootstrapDatabase();
	}

	@Override
	protected int deleteInternal(ContentURI uri, String selection, String[] selectionArgs) {
		URIPatternIds type = URIPatternIds.get(URI_MATCHER.match(uri));
		switch (type) {
		case FEEDS:
		case CATEGORY_FEEDS:
		case FEED_ID:
		case FEED_URI:
            return deleteFeeds(uri, type, selection, selectionArgs);
		case ENTRIES:
		case ENTRY_ID:
		case ENTRY_URI:
		case FEED_ENTRY_ID:
		case CATEGORY_ENTRIES:
		case FEED_ENTRIES:
		case FEED_ENTRIES_FILTER_AUTHOR:
		case FEED_ENTRIES_FILTER_CATEGORY:
		case FEED_ENTRIES_FILTER_READ:
		case FEED_ENTRIES_FILTER_UNREAD:
            return deleteEntries(uri, type,selection, selectionArgs);
		case CATEGORIES:
		case CATEGORY_ID:
		case FEED_CATEGORIES:
		case FEED_ENTRIES_CATEGORIES:
		case FEED_ENTRY_CATEGORIES:
		case ENTRY_CATEGORIES:
            return deleteCategories(uri, type, selection, selectionArgs);
		default:
            throw new IllegalArgumentException("Unknown URI: " + uri);		
		}
	}
	
	private int deleteFeeds(ContentURI uri, URIPatternIds type, String selection, String[] selectionArgs) {
		StringBuilder where = new StringBuilder();
		
		switch (type) {
		case FEED_ID:
			where.append(Afr.FeedsColumns._ID).append('=').append(uri.getPathLeafId());
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_URI:
        	try {
    			where.append(Afr.FeedsColumns.URI).append("='").append(URLDecoder.decode(uri.getPathLeaf(), "utf-8")).append('\'');
    		} catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Couldn't decode filter URI: " + uri.getPathLeaf(), e);	
    		}
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case CATEGORY_FEEDS:
			where.append(Afr.FeedsColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT f.id "); 
			where.append("FROM feeds f, categories c, category_links cl ");
			where.append("WHERE f._id = cl.feed AND cl.category.category = c._id AND c._id=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEEDS:
			break;
		}
		
		return mDb.delete(Afr.Feeds.SQL.tableName, where.toString(), selectionArgs);
	}
	
	private int deleteEntries(ContentURI uri, URIPatternIds type, String selection, String[] selectionArgs) {
		StringBuilder where = new StringBuilder();
		
		switch (type) {
		case ENTRY_ID:
			where.append(Afr.EntriesColumns._ID).append('=').append(uri.getPathLeafId());
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRY_ID:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			where.append(" AND ").append(Afr.EntriesColumns._ID).append('=').append(uri.getPathLeafId());
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case ENTRY_URI:
        	try {
    			where.append(Afr.EntriesColumns.URI).append("='").append(URLDecoder.decode(uri.getPathLeaf(), "utf-8")).append('\'');
    		} catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Couldn't decode filter URI: " + uri.getPathLeaf(), e);	
    		}
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case CATEGORY_ENTRIES:
			where.append(Afr.EntriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT e.id "); 
			where.append("FROM entries e, categories c, category_links cl ");
			where.append("WHERE e._id = cl.entry AND cl.category = c._id AND c._id=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES_FILTER_AUTHOR:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			where.append(" AND ").append(Afr.EntriesColumns.AUTHOR).append(" LIKE '").append(uri.getPathLeaf()).append('\'');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES_FILTER_CATEGORY:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			where.append(" AND ").append(Afr.EntriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT e.id "); 
			where.append("FROM entries e, categories c, category_links cl ");
			where.append("WHERE e._id = cl.entry AND cl.category = c._id AND c._id=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES_FILTER_READ:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			where.append(" AND ").append(Afr.EntriesColumns.READ).append("=1");
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES_FILTER_UNREAD:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			where.append(" AND ").append(Afr.EntriesColumns.READ).append("=0");
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case ENTRIES:
			break;
		}
		
		return mDb.delete(Afr.Entries.SQL.tableName, where.toString(), selectionArgs);
	}

	private int deleteCategories(ContentURI uri, URIPatternIds type, String selection, String[] selectionArgs) {
		StringBuilder where = new StringBuilder();
		
		switch (type) {
		case CATEGORY_ID:
			where.append(Afr.CategoriesColumns._ID).append('=').append(uri.getPathLeafId());
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_CATEGORIES:
			where.append(Afr.CategoriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT c.id "); 
			where.append("FROM categories c, category_links cl ");
			where.append("WHERE cl.entry IS NULL AND cl.category = c._id AND cl.feed=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES_CATEGORIES:
			where.append(Afr.CategoriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT c.id "); 
			where.append("FROM categories c, category_links cl ");
			where.append("WHERE cl.entry IS NOT NULL AND cl.category = c._id AND cl.feed=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRY_CATEGORIES:
			where.append(Afr.CategoriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT c.id "); 
			where.append("FROM categories c, category_links cl ");
			where.append("WHERE cl.category = c._id AND cl.feed=").append(uri.getPathSegment(1));
			where.append(" AND cl.entry=").append(uri.getPathSegment(3)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case ENTRY_CATEGORIES:
			where.append(Afr.CategoriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT c.id "); 
			where.append("FROM categories c, category_links cl ");
			where.append("WHERE cl.category = c._id AND cl.entry=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case CATEGORIES:
			break;
		}
		
		return mDb.delete(Afr.Categories.SQL.tableName, where.toString(), selectionArgs);
	}

	@Override
	protected ContentURI insertInternal(ContentURI uri, ContentValues values) {
		URIPatternIds type = URIPatternIds.get(URI_MATCHER.match(uri));
		switch (type) {
		case FEEDS:
		case FEED_URI:
            return insertFeed(uri, type, values);
		case ENTRIES:
		case ENTRY_URI:
		case FEED_ENTRIES:
            return insertEntry(uri, type, values);
		case CATEGORIES:
		case FEED_CATEGORIES:
		case FEED_ENTRY_CATEGORIES:
		case ENTRY_CATEGORIES:
            return insertCategory(uri, type, values);
		default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	private ContentURI insertFeed(ContentURI uri, URIPatternIds type, ContentValues values) {
		if (values == null) {
			values = new ContentValues();
		}
		
		if (type != URIPatternIds.FEED_URI) {
			if (!values.containsKey(Afr.FeedsColumns.URI)) {
				throw new IllegalArgumentException("Feed URI cannot be null");
			}
		} else {
        	try {
    			values.put(Afr.FeedsColumns.URI, URLDecoder.decode(uri.getPathLeaf(), "utf-8"));
    		} catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Couldn't decode URI: " + uri.getPathLeaf(), e);	
    		}
		}
			
		if (!values.containsKey(Afr.FeedsColumns.NAME)) {
			throw new IllegalArgumentException("Feed name cannot be null");
		}
		
		if (!values.containsKey(Afr.FeedsColumns.LINK)) {
			throw new IllegalArgumentException("Feed URL cannot be null");
		}

        Long now = Long.valueOf(System.currentTimeMillis());
        values.put(Afr.FeedsColumns.LAST_CHECKED, now);
		
		long rowId = mDb.insert(Afr.Feeds.SQL.tableName, Afr.FeedsColumns.URI, values);
		if (rowId > 0) {
			ContentURI newUri = Afr.Feeds.CONTENT_URI.addId(rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}
		
		throw new SQLException("Failed to insert row into " + uri);
	}

	private ContentURI insertEntry(ContentURI uri, URIPatternIds type, ContentValues values) {
		if (values == null) {
			values = new ContentValues();
		}

		if (type != URIPatternIds.FEED_ENTRIES) {
			if (!values.containsKey(Afr.EntriesColumns.FEED)) {
				throw new IllegalArgumentException("Entry feed id cannot be null");
			}
		} else {
			values.put(Afr.EntriesColumns.FEED, uri.getPathSegment(1));
		}
		
		if (type != URIPatternIds.FEED_URI) {
			if (!values.containsKey(Afr.EntriesColumns.URI)) {
				throw new IllegalArgumentException("Entry URI cannot be null");
			}
		} else {
        	try {
    			values.put(Afr.EntriesColumns.URI, URLDecoder.decode(uri.getPathLeaf(), "utf-8"));
    		} catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Couldn't decode URI: " + uri.getPathLeaf(), e);	
    		}
		}
		
		if (!values.containsKey(Afr.EntriesColumns.CONTENT)) {
			throw new IllegalArgumentException("Entry content cannot be null");
		}
		
		if (!values.containsKey(Afr.EntriesColumns.TYPE)) {
			throw new IllegalArgumentException("Entry content type cannot be null");
		}
		
        Long now = Long.valueOf(System.currentTimeMillis());
        // if no date given, assume now
		if (!values.containsKey(Afr.EntriesColumns.DATE)) {
	        values.put(Afr.EntriesColumns.DATE, now);
		}
		
		long rowId = mDb.insert(Afr.Entries.SQL.tableName, Afr.EntriesColumns.URI, values);
		if (rowId > 0) {
			ContentURI newUri = Afr.Entries.CONTENT_URI.addId(rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
	        // notfiy observers of the item parent as well (in case they need to update counts, etc...)
	        getContext().getContentResolver().notifyChange(Afr.Feeds.CONTENT_URI.addId(values.getAsLong(Afr.EntriesColumns.FEED)), null);
			return newUri;
		}
		
		throw new SQLException("Failed to insert row into " + uri);
	}
	
	private ContentURI insertCategory(ContentURI uri, URIPatternIds type, ContentValues values) {
		if (values == null) {
			values = new ContentValues();
		}

		switch (type) {
		case CATEGORIES:
			if (!values.containsKey(Afr.CategoriesColumns.NAME)) {
				throw new IllegalArgumentException("Category name cannot be null");
			}
			
			long rowId = mDb.insert(Afr.Categories.SQL.tableName, Afr.CategoriesColumns.NAME, values);
			if (rowId > 0) {
				ContentURI newUri = Afr.Categories.CONTENT_URI.addId(rowId);
				getContext().getContentResolver().notifyChange(newUri, null);
				return newUri;
			}
			break;
		case FEED_CATEGORIES:
			if (!values.containsKey(Afr.CategoryLinksColumns.FEED)) {
				throw new IllegalArgumentException("Feed id cannot be null");
			}
			if (!values.containsKey(Afr.CategoryLinksColumns.CATEGORY)) {
				throw new IllegalArgumentException("Category id cannot be null");
			}
			
			if (mDb.insert(Afr.CategoryLinks.SQL.tableName, Afr.CategoryLinksColumns.FEED, values) != -1) {
				ContentURI newUri = Afr.Feeds.CONTENT_URI.addId(values.getAsLong(Afr.CategoryLinksColumns.FEED));
				getContext().getContentResolver().notifyChange(newUri, null);
		        getContext().getContentResolver().notifyChange(
		        		Afr.Categories.CONTENT_URI.addId(values.getAsLong(Afr.CategoryLinksColumns.CATEGORY)), null);
		        return newUri;
			}
			break;
		case FEED_ENTRY_CATEGORIES:
			if (!values.containsKey(Afr.CategoryLinksColumns.FEED)) {
				throw new IllegalArgumentException("Entry id cannot be null");
			}
			if (!values.containsKey(Afr.CategoryLinksColumns.FEED)) {
				throw new IllegalArgumentException("Feed id cannot be null");
			}
			if (!values.containsKey(Afr.CategoryLinksColumns.CATEGORY)) {
				throw new IllegalArgumentException("Category id cannot be null");
			}
			
			if (mDb.insert(Afr.CategoryLinks.SQL.tableName, Afr.CategoryLinksColumns.ENTRY, values) != -1) {
				ContentURI newUri = Afr.Feeds.CONTENT_URI.addId(values.getAsLong(Afr.CategoryLinksColumns.ENTRY));
				getContext().getContentResolver().notifyChange(newUri, null);
		        getContext().getContentResolver().notifyChange(
		        		Afr.Feeds.CONTENT_URI.addId(values.getAsLong(Afr.CategoryLinksColumns.FEED)), null);
		        getContext().getContentResolver().notifyChange(
		        		Afr.Categories.CONTENT_URI.addId(values.getAsLong(Afr.CategoryLinksColumns.CATEGORY)), null);
		        return newUri;
			}
			break;
		case ENTRY_CATEGORIES:
			if (!values.containsKey(Afr.CategoryLinksColumns.FEED)) {
				throw new IllegalArgumentException("Entry id cannot be null");
			}
			if (!values.containsKey(Afr.CategoryLinksColumns.CATEGORY)) {
				throw new IllegalArgumentException("Category id cannot be null");
			}
			
			if (mDb.insert(Afr.CategoryLinks.SQL.tableName, Afr.CategoryLinksColumns.ENTRY, values) != -1) {
				ContentURI newUri = Afr.Feeds.CONTENT_URI.addId(values.getAsLong(Afr.CategoryLinksColumns.ENTRY));
				getContext().getContentResolver().notifyChange(newUri, null);
		        getContext().getContentResolver().notifyChange(
		        		Afr.Categories.CONTENT_URI.addId(values.getAsLong(Afr.CategoryLinksColumns.CATEGORY)), null);
		        return newUri;
			}
			break;
		}
		
		throw new SQLException("Failed to insert row into " + uri);
	}
	
	@Override
	protected Cursor queryInternal(ContentURI uri, String[] projection,
			String selection, String[] selectionArgs, 
			String groupBy, String having, String sortOrder) {
		URIPatternIds type = URIPatternIds.get(URI_MATCHER.match(uri));
		switch (type) {
		case FEEDS:
		case CATEGORY_FEEDS:
		case FEED_ID:
		case FEED_URI:
            return queryFeeds(uri, type, projection, selection, selectionArgs, groupBy, having, sortOrder);
		case ENTRY_ID:
		case ENTRY_URI:
		case FEED_ENTRY_ID:
		case ENTRIES:
		case CATEGORY_ENTRIES:
		case FEED_ENTRIES:
		case FEED_ENTRIES_FILTER_AUTHOR:
		case FEED_ENTRIES_FILTER_CATEGORY:
		case FEED_ENTRIES_FILTER_READ:
		case FEED_ENTRIES_FILTER_UNREAD:
		case FEED_ENTRIES_AUTHORS:
            return queryEntries(uri, type, projection, selection, selectionArgs, groupBy, having, sortOrder);
		case CATEGORIES:
		case CATEGORY_ID:
		case FEED_CATEGORIES:
		case FEED_ENTRIES_CATEGORIES:
		case FEED_ENTRY_CATEGORIES:
		case ENTRY_CATEGORIES:
            return queryCategories(uri, type, projection, selection, selectionArgs, groupBy, having, sortOrder);
		default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}
	
	private Cursor queryFeeds(ContentURI uri, URIPatternIds type, String[] projection,
			String selection, String[] selectionArgs, 
			String groupBy, String having, String sortOrder) {
		QueryBuilder qb = new QueryBuilder();
		
		switch (type) {
		case FEEDS:
			qb.setTables(Afr.Feeds.SQL.tableName);
			break;
		case FEED_ID:
			qb.setTables(Afr.Feeds.SQL.tableName);
			qb.appendWhere(Afr.FeedsColumns._ID + "=" + uri.getPathLeafId());
			break;
		case FEED_URI:
			qb.setTables(Afr.Feeds.SQL.tableName);
        	try {
        		qb.appendWhere("uri='" + URLDecoder.decode(uri.getPathLeaf(), "utf-8")+"'");
    		} catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Couldn't decode filter URI: " + uri.getPathLeaf(), e);	
    		}
        	break;
		case CATEGORY_FEEDS:
			qb.setTables(Afr.Feeds.SQL.tableName + " f, " + Afr.Categories.SQL.tableName + " c, " + Afr.CategoryLinks.SQL.tableName + " cl");
			qb.appendWhere("f._id = cl.feed AND cl.category = " + uri.getPathSegment(1));
			qb.setDistinct(true);
			qb.setProjectionMap(feedsColumnMap);
			break;
		}
		
        if (TextUtils.isEmpty(sortOrder)) {
        	sortOrder = Afr.Feeds.DEFAULT_SORT_ORDER;
        }

        Cursor c = qb.query(mDb, projection, selection, selectionArgs, groupBy, having, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        
		return c;
	}
	
	private Cursor queryEntries(ContentURI uri, URIPatternIds type, String[] projection,
			String selection, String[] selectionArgs, 
			String groupBy, String having, String sortOrder) {
		QueryBuilder qb = new QueryBuilder();
		
		switch (type) {
		case ENTRIES:
			qb.setTables(Afr.Entries.SQL.tableName);
			break;
		case FEED_ENTRIES:
			qb.setTables(Afr.Entries.SQL.tableName);
			qb.appendWhere(Afr.EntriesColumns.FEED + '=' + uri.getPathSegment(1));
			break;
		case ENTRY_ID:
			qb.setTables(Afr.Entries.SQL.tableName);
			qb.appendWhere(Afr.EntriesColumns._ID + '=' + uri.getPathLeafId());
			break;
		case ENTRY_URI:
			qb.setTables(Afr.Entries.SQL.tableName);
        	try {
        		qb.appendWhere("uri='" + URLDecoder.decode(uri.getPathLeaf(), "utf-8")+"'");
    		} catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Couldn't decode filter URI: " + uri.getPathLeaf(), e);	
    		}
        	break;
		case FEED_ENTRY_ID:
			qb.setTables(Afr.Entries.SQL.tableName);
			qb.appendWhere(Afr.EntriesColumns.FEED + '=' + uri.getPathSegment(1));
			qb.appendWhere(" AND " + Afr.EntriesColumns._ID + '=' + uri.getPathLeafId());
			break;
		case CATEGORY_ENTRIES:
			qb.setTables(Afr.Entries.SQL.tableName + " e, " + Afr.Categories.SQL.tableName + " c, " + Afr.CategoryLinks.SQL.tableName + " cl");
			qb.appendWhere("e._id = cl.entry");
			qb.appendWhere(" AND cl.category = " + uri.getPathSegment(1));
			qb.setDistinct(true);
			qb.setProjectionMap(entriesColumnMap);
			break;
		case FEED_ENTRIES_FILTER_AUTHOR:
			qb.setTables(Afr.Entries.SQL.tableName);
			qb.appendWhere(Afr.EntriesColumns.FEED + '=' + uri.getPathSegment(1));
			qb.appendWhere(" AND " + Afr.EntriesColumns.AUTHOR + " LIKE '" + uri.getPathLeaf() + '\'');
			break;
		case FEED_ENTRIES_FILTER_CATEGORY:
			qb.setTables(Afr.Entries.SQL.tableName + " e, " + Afr.Categories.SQL.tableName + " c, " + Afr.CategoryLinks.SQL.tableName + " cl");
			qb.appendWhere("e.feed" + '=' + uri.getPathSegment(1));
			qb.appendWhere(" AND e._id = cl.entry");
			qb.appendWhere(" AND cl.category LIKE '" + uri.getPathLeaf() + '\'');
			
			qb.setDistinct(true);
			qb.setProjectionMap(entriesColumnMap);
			
			qb.setTables(Afr.Entries.SQL.tableName);
			qb.appendWhere(Afr.EntriesColumns.AUTHOR + " LIKE '" + uri.getPathLeaf() + '\'');
			break;
		case FEED_ENTRIES_FILTER_READ:
			qb.setTables(Afr.Entries.SQL.tableName);
			qb.appendWhere(Afr.EntriesColumns.FEED + '=' + uri.getPathSegment(1));
			qb.appendWhere(" AND " + Afr.EntriesColumns.READ + "=1");
			break;
		case FEED_ENTRIES_FILTER_UNREAD:
			qb.setTables(Afr.Entries.SQL.tableName);
			qb.appendWhere(Afr.EntriesColumns.FEED + '=' + uri.getPathSegment(1));
			qb.appendWhere(" AND " + Afr.EntriesColumns.READ + "=0");
			break;
		case FEED_ENTRIES_AUTHORS:
			StringBuilder query = new StringBuilder();
			query.append("SELECT author, feed, count(*) AS num_count ");
			query.append("FROM ").append(Afr.Entries.SQL.tableName).append(' ');
			query.append("WHERE feed =").append(uri.getPathSegment(1)).append(' ');
			if (!TextUtils.isEmpty(selection)) {
				query.append("AND (").append(selection).append(") ");
			}
			query.append("GROUP BY lower(author) ");
			
			Cursor c = mDb.query(query.toString(), selectionArgs);
	        c.setNotificationUri(getContext().getContentResolver(), uri);
			return c;
		}
		
        if (TextUtils.isEmpty(sortOrder)) {
        	sortOrder = Afr.Entries.DEFAULT_SORT_ORDER;
        }

        Cursor c = qb.query(mDb, projection, selection, selectionArgs, groupBy, having, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        
		return c;
	}
	
	private Cursor queryCategories(ContentURI uri, URIPatternIds type, String[] projection,
			String selection, String[] selectionArgs, 
			String groupBy, String having, String sortOrder) {
		QueryBuilder qb = new QueryBuilder();
		
		switch (type) {
		case CATEGORIES:
			qb.setTables(Afr.Categories.SQL.tableName);
			break;
		case CATEGORY_ID:
			qb.setTables(Afr.Categories.SQL.tableName);
			qb.appendWhere(Afr.CategoriesColumns._ID + '=' + uri.getPathLeafId());
			break;
		case FEED_CATEGORIES:
			qb.setTables(Afr.Categories.SQL.tableName + " c, " + Afr.CategoryLinks.SQL.tableName + " cl");
			qb.appendWhere("cl.feed = " + uri.getPathSegment(1));
			qb.appendWhere(" AND cl.entry IS NULL");
			qb.setDistinct(true);
			qb.setProjectionMap(categoriesColumnMap);
			break;
		case FEED_ENTRIES_CATEGORIES:
			qb.setTables(Afr.Categories.SQL.tableName + " c, " + Afr.CategoryLinks.SQL.tableName + " cl");
			qb.appendWhere("cl.feed = " + uri.getPathSegment(1));
			qb.appendWhere(" AND cl.entry IS NOT NULL");
			qb.setDistinct(true);
			qb.setProjectionMap(categoriesColumnMap);
			break;
		case FEED_ENTRY_CATEGORIES:
			qb.setTables(Afr.Categories.SQL.tableName + " c, " + Afr.CategoryLinks.SQL.tableName + " cl");
			qb.appendWhere("cl.feed = " + uri.getPathSegment(1));
			qb.appendWhere(" AND cl.entry = " + uri.getPathSegment(3));
			qb.setDistinct(true);
			qb.setProjectionMap(categoriesColumnMap);
			break;
		case ENTRY_CATEGORIES:
			qb.setTables(Afr.Categories.SQL.tableName + " c, " + Afr.CategoryLinks.SQL.tableName + " cl");
			qb.appendWhere("cl.entry = " + uri.getPathSegment(1));
			qb.setDistinct(true);
			qb.setProjectionMap(categoriesColumnMap);
			break;
		}
		
        if (TextUtils.isEmpty(sortOrder)) {
        	sortOrder = Afr.Categories.DEFAULT_SORT_ORDER;
        }

        Cursor c = qb.query(mDb, projection, selection, selectionArgs, groupBy, having, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        
		return c;
	}

	@Override
	protected int updateInternal(ContentURI uri, ContentValues values, String selection, String[] selectionArgs) {
		URIPatternIds type = URIPatternIds.get(URI_MATCHER.match(uri));
		switch (type) {
		case FEEDS:
		case CATEGORY_FEEDS:
		case FEED_ID:
		case FEED_URI:
            return updateFeeds(uri, type, values, selection, selectionArgs);
		case ENTRIES:
		case ENTRY_ID:
		case ENTRY_URI:
		case FEED_ENTRY_ID:
		case CATEGORY_ENTRIES:
		case FEED_ENTRIES:
		case FEED_ENTRIES_FILTER_AUTHOR:
		case FEED_ENTRIES_FILTER_CATEGORY:
		case FEED_ENTRIES_FILTER_READ:
		case FEED_ENTRIES_FILTER_UNREAD:
            return updateEntries(uri, type, values, selection, selectionArgs);
		case CATEGORIES:
		case CATEGORY_ID:
		case FEED_CATEGORIES:
		case FEED_ENTRIES_CATEGORIES:
		case FEED_ENTRY_CATEGORIES:
		case ENTRY_CATEGORIES:
            return updateCategories(uri, type, values, selection, selectionArgs);
		default:
            throw new IllegalArgumentException("Unknown URI: " + uri);		
		}
	}
	
	private int updateFeeds(ContentURI uri, URIPatternIds type, ContentValues values, String selection, String[] selectionArgs) {
		StringBuilder where = new StringBuilder();
		
		switch (type) {
		case FEED_ID:
			where.append(Afr.FeedsColumns._ID).append('=').append(uri.getPathLeafId());
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_URI:
        	try {
    			where.append(Afr.FeedsColumns.URI).append("='").append(URLDecoder.decode(uri.getPathLeaf(), "utf-8")).append('\'');
    		} catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Couldn't decode filter URI: " + uri.getPathLeaf(), e);	
    		}
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case CATEGORY_FEEDS:
			where.append(Afr.FeedsColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT f.id "); 
			where.append("FROM feeds f, categories c, category_links cl ");
			where.append("WHERE f._id = cl.feed AND cl.category.category = c._id AND c._id=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEEDS:
			break;
		}
		
		return mDb.update(Afr.Feeds.SQL.tableName, values, where.toString(), selectionArgs);
	}
	
	private int updateEntries(ContentURI uri, URIPatternIds type, ContentValues values, String selection, String[] selectionArgs) {
		StringBuilder where = new StringBuilder();
		
		switch (type) {
		case ENTRY_ID:
			where.append(Afr.EntriesColumns._ID).append('=').append(uri.getPathLeafId());
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRY_ID:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			where.append(" AND ").append(Afr.EntriesColumns._ID).append('=').append(uri.getPathLeafId());
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case ENTRY_URI:
        	try {
    			where.append(Afr.EntriesColumns.URI).append("='").append(URLDecoder.decode(uri.getPathLeaf(), "utf-8")).append('\'');
    		} catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Couldn't decode filter URI: " + uri.getPathLeaf(), e);	
    		}
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case CATEGORY_ENTRIES:
			where.append(Afr.EntriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT e.id "); 
			where.append("FROM entries e, categories c, category_links cl ");
			where.append("WHERE e._id = cl.entry AND cl.category = c._id AND c._id=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES_FILTER_AUTHOR:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			where.append(" AND ").append(Afr.EntriesColumns.AUTHOR).append(" LIKE '").append(uri.getPathLeaf()).append('\'');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES_FILTER_CATEGORY:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			where.append(" AND ").append(Afr.EntriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT e.id "); 
			where.append("FROM entries e, categories c, category_links cl ");
			where.append("WHERE e._id = cl.entry AND cl.category = c._id AND c._id=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES_FILTER_READ:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			where.append(" AND ").append(Afr.EntriesColumns.READ).append("=1");
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES_FILTER_UNREAD:
			where.append(Afr.EntriesColumns.FEED).append('=').append(uri.getPathSegment(1));
			where.append(" AND ").append(Afr.EntriesColumns.READ).append("=0");
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case ENTRIES:
			break;
		}
		
		return mDb.update(Afr.Entries.SQL.tableName, values, where.toString(), selectionArgs);
	}

	private int updateCategories(ContentURI uri, URIPatternIds type, ContentValues values, String selection, String[] selectionArgs) {
		StringBuilder where = new StringBuilder();
		
		switch (type) {
		case CATEGORY_ID:
			where.append(Afr.CategoriesColumns._ID).append('=').append(uri.getPathLeafId());
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_CATEGORIES:
			where.append(Afr.CategoriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT c.id "); 
			where.append("FROM categories c, category_links cl ");
			where.append("WHERE cl.entry IS NULL AND cl.category = c._id AND cl.feed=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRIES_CATEGORIES:
			where.append(Afr.CategoriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT c.id "); 
			where.append("FROM categories c, category_links cl ");
			where.append("WHERE cl.entry IS NOT NULL AND cl.category = c._id AND cl.feed=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case FEED_ENTRY_CATEGORIES:
			where.append(Afr.CategoriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT c.id "); 
			where.append("FROM categories c, category_links cl ");
			where.append("WHERE cl.category = c._id AND cl.feed=").append(uri.getPathSegment(1));
			where.append(" AND cl.entry=").append(uri.getPathSegment(3)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case ENTRY_CATEGORIES:
			where.append(Afr.CategoriesColumns._ID).append(" IN (");
			where.append("SELECT DISTINCT c.id "); 
			where.append("FROM categories c, category_links cl ");
			where.append("WHERE cl.category = c._id AND cl.entry=").append(uri.getPathSegment(1)).append(')');
			if (!TextUtils.isEmpty(selection)) {
				where.append(" AND (").append(selection).append(')');
			}
			break;
		case CATEGORIES:
			break;
		}
		
		return mDb.update(Afr.Categories.SQL.tableName, values, where.toString(), selectionArgs);
	}
	
	@Override
	public String getType(ContentURI uri) {
		switch (URIPatternIds.get(URI_MATCHER.match(uri))) {
		case FEEDS:
		case CATEGORY_FEEDS:
            return Afr.Feeds.CONTENT_TYPE;
		case FEED_ID:
		case FEED_URI:
            return Afr.Feeds.CONTENT_ITEM_TYPE;
		case ENTRIES:
		case CATEGORY_ENTRIES:
		case FEED_ENTRIES:
		case FEED_ENTRIES_FILTER_AUTHOR:
		case FEED_ENTRIES_FILTER_CATEGORY:
		case FEED_ENTRIES_FILTER_READ:
		case FEED_ENTRIES_FILTER_UNREAD:
            return Afr.Entries.CONTENT_TYPE;
		case FEED_ENTRIES_AUTHORS:
			return Afr.Authors.CONTENT_TYPE;
		case ENTRY_ID:
		case ENTRY_URI:
		case FEED_ENTRY_ID:
            return Afr.Entries.CONTENT_ITEM_TYPE;
		case CATEGORIES:
		case FEED_CATEGORIES:
		case FEED_ENTRIES_CATEGORIES:
		case FEED_ENTRY_CATEGORIES:
		case ENTRY_CATEGORIES:
            return Afr.Categories.CONTENT_TYPE;
		case CATEGORY_ID:
            return Afr.Categories.CONTENT_ITEM_TYPE;
		default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}
}
