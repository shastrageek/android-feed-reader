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

import android.net.ContentURI;
import android.provider.BaseColumns;

public final class Afr {
	public static final String AUTHORITY = "ca.luniv.afr.provider.Afr";
	
	public interface FeedsColumns extends BaseColumns {
		/**
	     * The URI of the feed. This is the unique id for the feed
	     * <P>Type: TEXT</P>
	     */
		public static final String URI = "uri";
		/**
	     * The name of the feed
	     * <P>Type: TEXT</P>
	     */
		public static final String NAME = "name";
		/**
	     * The URL of the feed
	     * <P>Type: TEXT</P>
	     */
		public static final String LINK = "link";
		/**
	     * How long, in seconds, to keep feed items for after they have been retrieved (0 indicates use application default)
	     * <P>Type: INTEGER</P>
	     */
		public static final String EXPIRE_AFTER = "expire_after";
		/**
	     * The date & time of when the feed was last checked
	     * <P>Type: TIMESTAMP (INTEGER)</P>
	     */
		public static final String LAST_CHECKED = "last_checked";
		/**
	     * The HTTP date string of when the feed was last modified (used for conditional GETs)
	     * <P>Type: TEXT</P>
	     */
		public static final String LAST_MODIFIED = "last_modified";
		/**
	     * The HTTP ETag of the feed (used for conditional GETs)
	     * <P>Type: TEXT</P>
	     */
		public static final String ETAG = "etag";
	}
	
	public static final class Feeds implements FeedsColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.afr.feed";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.afr.feed";
		
		public static final ContentURI CONTENT_URI = ContentURI.create("content://ca.luniv.afr.provider.Afr/feeds");
		public static final ContentURI CONTENT_FILTER_URI_URI = ContentURI.create("content://ca.luniv.afr.provider.Afr/feeds/uri");
		
		public static final String DEFAULT_SORT_ORDER = "name ASC";
		
		static final class SQL {
			static final String tableName = "feeds";
			static final String create = "CREATE TABLE feeds (" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"uri TEXT NOT NULL UNIQUE, " +
				"name TEXT NOT NULL, " +
				"link TEXT NOT NULL, " +
				"expire_after INTEGER, " +
				"last_checked TIMESTAMP, " +
				"last_modified TEXT, " +
				"etag TEXT " +
				");";
			static final String create_item_trigger = "CREATE TRIGGER fkd_feeds_items BEFORE DELETE ON feeds " +
				"FOR EACH ROW BEGIN " +
				"   DELETE FROM entries WHERE feed = OLD._id; " +
				"END;";
			static final String create_category_trigger = "CREATE TRIGGER fkd_feeds_category_links BEFORE DELETE ON feeds " +
				"FOR EACH ROW BEGIN " +
				"   DELETE FROM category_links WHERE feed = OLD._id; " +
				"END;";
			static final String delete = "DROP TABLE IF EXISTS feeds;";
		}
	}
	
	public interface EntriesColumns extends BaseColumns {
		/**
	     * The id of the feed the item belongs to
	     * <P>Type: INTEGER</P>
	     */
		public static final String FEED = "feed";
		/**
	     * The URI of the item. This is the unique id for the item
	     * <P>Type: TEXT</P>
	     */
		public static final String URI = "uri";
		/**
	     * The description of the item
	     * <P>Type: TEXT</P>
	     */
		public static final String TITLE = "title";
		/**
	     * The author of the item
	     * <P>Type: TEXT</P>
	     */
		public static final String AUTHOR = "author";
		/**
	     * The date & time of when the item was posted
	     * <P>Type: TIMESTAMP (INTEGER)</P>
	     */
		public static final String DATE = "date";
		/**
	     * The URL of the item
	     * <P>Type: TEXT</P>
	     */
		public static final String LINK = "link";
		/**
	     * The contents of the item
	     * <P>Type: TEXT</P>
	     */
		public static final String CONTENT = "content";
		/**
	     * The MIME type of the item's contents
	     * <P>Type: TEXT</P>
	     */
		public static final String TYPE = "type";
		/**
	     * Indicates if the item has been read or not
	     * <P>Type: BOOLEAN (INTEGER)</P>
	     */
		public static final String READ = "read";
	}
	
	public static final class Entries implements EntriesColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.afr.entry";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.afr.entry";
		
		public static final ContentURI CONTENT_URI = ContentURI.create("content://ca.luniv.afr.provider.Afr/entries");
		public static final ContentURI CONTENT_FILTER_URI_URI = ContentURI.create("content://ca.luniv.afr.provider.Afr/entries/uri");
		
		public static final String DEFAULT_SORT_ORDER = "date DESC";
	
		static final class SQL {
			static final String tableName = "entries";
			static final String create = "CREATE TABLE entries (" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"feed INTEGER NOT NULL, " +
				"uri TEXT NOT NULL UNIQUE, " +
				"title TEXT, " +
				"author TEXT, " +
				"date TIMESTAMP NOT NULL, " + 
				"link TEXT, " +
				"content TEXT NOT NULL, " +
				"type TEXT NOT NULL, " +
				"read BOOLEAN DEFAULT 0 " +
				"); ";
			static final String create_category_trigger = "CREATE TRIGGER fkd_entries_category_links BEFORE DELETE ON entries " +
				"FOR EACH ROW BEGIN " +
				"   DELETE FROM category_links WHERE entry = OLD._id; " +
				"END;";
			static final String delete = "DROP TABLE IF EXISTS entries";
		}
	}

	public interface AuthorsColumns {
		/**
	     * The author's name
	     * <P>Type: TEXT</P>
	     */
		public static final String AUTHOR = "author";
		/**
	     * The id of the feed the author belongs to
	     * <P>Type: INTEGER</P>
	     */
		public static final String FEED = "feed";
		/**
	     * The number of entries posted by this author
	     * <P>Type: INTEGER</P>
	     */
		public static final String NUM_POSTED = "num_posted";
	}
	
	public static final class Authors implements AuthorsColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.afr.author";
		
	}

	public interface CategoriesColumns extends BaseColumns {
		/**
	     * The name of the category
	     * <P>Type: TEXT</P>
	     */
		public static final String NAME = "name";		
	}
	
	public static final class Categories implements CategoriesColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.afr.category";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.afr.category";
		
		public static final ContentURI CONTENT_URI = ContentURI.create("content://ca.luniv.afr.provider.Afr/categories");
		
		public static final String DEFAULT_SORT_ORDER = "name DESC";
	
		static final class SQL {
			static final String tableName = "categories";
			static final String create = "CREATE TABLE categories (" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"name TEXT NOT NULL " +
				");";
			static final String create_category_trigger = "CREATE TRIGGER fkd_categories_category_links BEFORE DELETE ON categories " +
				"FOR EACH ROW BEGIN " +
				"   DELETE FROM category_links WHERE category = OLD._id; " +
				"END;";
			static final String delete = "DROP TABLE IF EXISTS categories";
		}
	}

	/**
	 * These are the columns for the internal table that links feeds & entries to categories.
	 * @author James Gilbertson
	 */
	interface CategoryLinksColumns {
		/**
	     * The name of the category
	     * <P>Type: TEXT</P>
	     */
		public static final String CATEGORY = "category";
		/**
	     * The id of the feed to associate with the category
	     * <P>Type: INTEGER</P>
	     */
		public static final String FEED = "feed";
		/**
	     * The id of the item to associate with the category
	     * <P>Type: INTEGER</P>
	     */
		public static final String ENTRY = "entry";
	}
	
	static final class CategoryLinks implements CategoryLinksColumns {
		static final class SQL {
			static final String tableName = "category_links";
			static final String create = "CREATE TABLE category_links (" +
				"category INTEGER NOT NULL, " +
				"feed INTEGER, " +
				"entry INTEGER " +
				");";
			static final String delete = "DROP TABLE IF EXISTS category_links";
		}
	}
}
