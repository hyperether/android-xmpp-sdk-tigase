package org.tigase.messenger.phone.pro.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.tigase.messenger.TigaseSdk;
import org.tigase.messenger.events.UploadFileEvent;
import org.tigase.messenger.events.UploadedFileEvent;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.utils.Crypto;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

class SendUnsentGroupMessages implements Runnable {

    public static final String TAG = "SendUnsentMuc";
    private final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
            DatabaseContract.ChatHistory.FIELD_ACCOUNT,
            DatabaseContract.ChatHistory.FIELD_AUTHOR_JID,
            DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
            DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME,
            DatabaseContract.ChatHistory.FIELD_BODY,
            DatabaseContract.ChatHistory.FIELD_DATA,
            DatabaseContract.ChatHistory.FIELD_JID,
            DatabaseContract.ChatHistory.FIELD_STATE,
            DatabaseContract.ChatHistory.FIELD_THREAD_ID,
            DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI,
            DatabaseContract.ChatHistory.FIELD_STANZA_ID,
            DatabaseContract.ChatHistory.FIELD_TIMESTAMP};
    private final Context context;
    private final Jaxmpp jaxmpp;
    private final Room room;
    private final SessionObject sessionObject;

    public SendUnsentGroupMessages(Context context, Jaxmpp jaxmpp, Room room) {
        this.room = room;
        this.context = context;
        this.jaxmpp = jaxmpp;
        this.sessionObject = jaxmpp.getSessionObject();
    }

    public void run(final Uri itemUri) {
        try (Cursor c = context.getContentResolver().query(itemUri, cols, null, null, null)) {
            while (c.moveToNext()) {
                send(c);
            }
        }
    }

    @Override
    public void run() {
        Log.d("SendUnsentGroupMessages", "Sending unsent MUC " + room.getRoomJid() + " messages ");

        Uri u = Uri.parse(TigaseSdk.getInstance().getUnsentMessagesUri() + "/" +
                room.getSessionObject().getUserBareJid());

        try (Cursor c = context.getContentResolver()
                .query(u, cols, DatabaseContract.ChatHistory.FIELD_CHAT_TYPE + "==? AND " +
                                DatabaseContract.ChatHistory.FIELD_JID + "=?",
                        new String[]{"" + DatabaseContract.ChatHistory.CHAT_TYPE_MUC, room
                                .getRoomJid().toString()},
                        DatabaseContract.ChatHistory.FIELD_TIMESTAMP)) {
            while (c.moveToNext()) {
                send(c);
            }
        }
    }

    private void send(final Cursor c) {
        final int id = c.getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
        final JID toJid = JID
                .jidInstance(c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_JID)));
        final String threadId = c
                .getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_THREAD_ID));
        final String body = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
        final String stanzaId = c
                .getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STANZA_ID));
        final String oobData = c
                .getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_DATA));
        final int itemType = c
                .getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE));
        final String localContent = c.getString(
                c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI));

        Log.d("SendUnsentGroupMessages", "Preparing " + id + ": " + body);

        if (jaxmpp.isConnected()) {
            try {
                final ContentValues values = new ContentValues();
                values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP,
                        System.currentTimeMillis());

                final Message msg = Message.create();
                msg.setTo(toJid);
                msg.setType(StanzaType.groupchat);
                msg.setThread(threadId);
                msg.setBody(Crypto.encodeBody(body));
                msg.setId(stanzaId);

                SendUnsentMessages.addOOB(itemType, oobData, msg, values);

                if (itemType == DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE &&
                        localContent != null && oobData == null) {
                    Uri content = Uri.parse(localContent);
                    String mimeType = null;
                    try (Cursor cursor = context.getContentResolver()
                            .query(content, null, null, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            String displayName = cursor
                                    .getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            mimeType = FileUploaderTask.guessMimeType(displayName);
                        }
                    }

                    EventBus.getDefault().post(new UploadFileEvent(content, mimeType,
                            toJid.getBareJid().getLocalpart(),
                            new UploadedFileEvent(null, id, msg, values, jaxmpp, room, true)));
                } else {
                    send(id, msg, values);
                }
            } catch (JaxmppException e) {
                Log.w("XMPPService", "Cannot send unsent message", e);
            }
        } else {
            Log.w("XMPPService", "Can't find chat object for message");
        }
    }

    private void send(final int id, final Message msg,
                      final ContentValues values) throws JaxmppException {
        room.sendMessage(msg);
        values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                DatabaseContract.ChatHistory.STATE_OUT_SENT);
        context.getContentResolver()
                .update(Uri.parse(TigaseSdk.getInstance().getMucHistoryUri() + "/" +
                        room.getSessionObject().getUserBareJid() + "/" +
                        msg.getTo().getBareJid() + "/" + id), values, null, null);
    }
}
