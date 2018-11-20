/*
 * Tigase XMPP Client Library
 * Copyright (C) 2014 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package org.tigase.messenger.jaxmpp.android.chat;

import android.database.sqlite.SQLiteDatabase;

import org.tigase.messenger.phone.pro.db.DatabaseContract;

public class OpenChatDbHelper {

    private static final String CREATE_OPEN_CHATS_TABLE =
            "CREATE TABLE " + DatabaseContract.OpenChats.TABLE_NAME + " (" +
                    DatabaseContract.OpenChats.FIELD_ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.OpenChats.FIELD_ACCOUNT + " TEXT, " +
                    DatabaseContract.OpenChats.FIELD_JID + " TEXT, " +
                    DatabaseContract.OpenChats.FIELD_TIMESTAMP +
                    " DATETIME, " + DatabaseContract.OpenChats.FIELD_TYPE + " INTEGER, " +
                    DatabaseContract.OpenChats.FIELD_THREAD_ID + " TEXT, " +
                    DatabaseContract.OpenChats.FIELD_RESOURCE +
                    " TEXT," + DatabaseContract.OpenChats.FIELD_NICKNAME + " TEXT, " +
                    DatabaseContract.OpenChats.FIELD_PASSWORD + " TEXT, " +
                    DatabaseContract.OpenChats.FIELD_ROOM_STATE + " INTEGER" + ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_OPEN_CHATS_TABLE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
