/*
 * DatabaseHelper.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package org.tigase.messenger.phone.pro.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.tigase.messenger.jaxmpp.android.caps.CapsDbHelper;
import org.tigase.messenger.jaxmpp.android.chat.OpenChatDbHelper;
import org.tigase.messenger.jaxmpp.android.roster.RosterDbHelper;

public class DatabaseHelper
        extends SQLiteOpenHelper {

    private static DatabaseHelper sInstance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("DatabaseHelper", "Create database");
        RosterDbHelper.onCreate(db);
        OpenChatDbHelper.onCreate(db);

        CapsDbHelper.onCreate(db);

        db.execSQL("ALTER TABLE " + DatabaseContract.RosterItemsCache.TABLE_NAME + " ADD COLUMN " +
                DatabaseContract.RosterItemsCache.FIELD_STATUS + " INTEGER DEFAULT 0;");

        db.execSQL(DatabaseContract.ChatHistory.CREATE_TABLE);
        db.execSQL(DatabaseContract.ChatHistory.CREATE_INDEX_JID);
        db.execSQL(DatabaseContract.ChatHistory.CREATE_INDEX_STATE);

        db.execSQL(DatabaseContract.VCardsCache.CREATE_TABLE);
        db.execSQL(DatabaseContract.VCardsCache.CREATE_INDEX);

        db.execSQL(DatabaseContract.Geolocation.CREATE_TABLE);
        db.execSQL(DatabaseContract.Geolocation.CREATE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("DatabaseHelper", "Update database from " + oldVersion + " to " + newVersion);
        if (oldVersion == 6 && newVersion == 7) {
            db.execSQL("DROP INDEX " + DatabaseContract.ChatHistory.INDEX_JID);

            db.execSQL(DatabaseContract.ChatHistory.CREATE_INDEX_JID);
            db.execSQL(DatabaseContract.ChatHistory.CREATE_INDEX_STATE);
        }

        if (oldVersion <= 7) {
            db.execSQL("ALTER TABLE " + DatabaseContract.ChatHistory.TABLE_NAME + " ADD " +
                    DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI + " TEXT");
        }

        if (oldVersion <= 8) {
            db.execSQL("ALTER TABLE " + DatabaseContract.ChatHistory.TABLE_NAME + " ADD " +
                    DatabaseContract.ChatHistory.FIELD_CHAT_TYPE + " INTEGER");

            db.execSQL("UPDATE " + DatabaseContract.ChatHistory.TABLE_NAME + " SET " +
                    DatabaseContract.ChatHistory.FIELD_CHAT_TYPE + "=" +
                    DatabaseContract.ChatHistory.CHAT_TYPE_P2P + " WHERE " +
                    DatabaseContract.ChatHistory.FIELD_ITEM_TYPE + "<>6");

            db.execSQL("UPDATE " + DatabaseContract.ChatHistory.TABLE_NAME + " SET " +
                    DatabaseContract.ChatHistory.FIELD_CHAT_TYPE + "=" +
                    DatabaseContract.ChatHistory.CHAT_TYPE_MUC + " WHERE " +
                    DatabaseContract.ChatHistory.FIELD_ITEM_TYPE + "=6");

            db.execSQL("UPDATE " + DatabaseContract.ChatHistory.TABLE_NAME + " SET " +
                    DatabaseContract.ChatHistory.FIELD_ITEM_TYPE + "=" +
                    DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE + " WHERE " +
                    DatabaseContract.ChatHistory.FIELD_ITEM_TYPE + "=6");

            db.execSQL("UPDATE " + DatabaseContract.ChatHistory.TABLE_NAME + " SET " +
                    DatabaseContract.ChatHistory.FIELD_ITEM_TYPE + "=" +
                    DatabaseContract.ChatHistory.ITEM_TYPE_ERROR + " WHERE " +
                    DatabaseContract.ChatHistory.FIELD_ITEM_TYPE + "=7");
        }

    }
}
