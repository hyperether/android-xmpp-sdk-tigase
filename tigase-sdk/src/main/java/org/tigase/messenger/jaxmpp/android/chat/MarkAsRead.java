package org.tigase.messenger.jaxmpp.android.chat;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.tigase.messenger.TigaseSdk;
import org.tigase.messenger.phone.pro.db.DatabaseContract;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;

public class MarkAsRead {

    private final Context context;
    private Handler mHandler;

    public MarkAsRead(Context context) {
        this.context = context.getApplicationContext();
    }

    private void intMarkAsRead(final Uri u, final long chatId, final BareJID account,
                               final JID jid) {

        getMarkAsReadHandler().post(new Runnable() {
            @Override
            public void run() {
                final Uri uri = Uri.parse(u + "/" + account + "/" + jid.getBareJid());

                ContentValues values = new ContentValues();
                values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                        DatabaseContract.ChatHistory.STATE_INCOMING);

                try (Cursor c = context.getContentResolver()
                        .query(uri, new String[]{DatabaseContract.ChatHistory.FIELD_ID,
                                        DatabaseContract.ChatHistory.FIELD_STATE},
                                DatabaseContract.ChatHistory.FIELD_STATE + "=?",
                                new String[]{
                                        "" + DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD},
                                null)) {
                    while (c.moveToNext()) {
                        final int id = c
                                .getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
                        Uri u = ContentUris.withAppendedId(uri, id);

                        int x = context.getContentResolver().update(u, values, null, null);
                        Log.d("MarkAsRead", "Found unread (" + x + ") " + u);
                    }
                } catch (Exception e) {
                    Log.e("MarkAsRead",
                            "Can't mark as read acc=" + account + ", jid=" + jid.getBareJid(), e);
                }

                context.getContentResolver().notifyChange(ContentUris
                                .withAppendedId(TigaseSdk.getInstance().getOpenChatsUri(), chatId),
                        null);
            }
        });
    }

    public void markChatAsRead(final long chatId, final BareJID account, final JID jid) {
        intMarkAsRead(TigaseSdk.getInstance().getChatHistoryUri(), chatId, account, jid);
    }

    public void markGroupchatAsRead(long openChatId, BareJID account, JID jid) {
        intMarkAsRead(TigaseSdk.getInstance().getMucHistoryUri(), openChatId, account, jid);
    }

    private Handler getMarkAsReadHandler() {
        if (mHandler == null) {
            HandlerThread handlerThread = new HandlerThread("mark_as_read_thread");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper());
        }
        return mHandler;
    }
}
