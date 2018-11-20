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
import org.tigase.messenger.phone.pro.utils.Parser;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementBuilder;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

public class SendUnsentMessages implements Runnable {

    public static final String TAG = "SendUnsentChat";
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
            DatabaseContract.ChatHistory.FIELD_STANZA_ID,
            DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI,
            DatabaseContract.ChatHistory.FIELD_TIMESTAMP};
    private final Context context;
    private final Jaxmpp jaxmpp;
    private final SessionObject sessionObject;

    static void addOOB(final int messageType, String oobData, Message msg, ContentValues values)
            throws JaxmppException {
        if (oobData != null) {
            Element url = Parser.parseElement(oobData);
            Element x = ElementBuilder.create("x", "jabber:x:oob").getElement();
            x.addChild(url);
            msg.addChild(x);

            values.put(DatabaseContract.ChatHistory.FIELD_DATA, url.getAsString());
        }
    }

    public SendUnsentMessages(Context context, Jaxmpp jaxmpp) {
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
        Uri u = Uri
                .parse(TigaseSdk.getInstance().getUnsentMessagesUri() + "/" +
                        sessionObject.getUserBareJid());
        try (Cursor c = context.getContentResolver()
                .query(u, cols, DatabaseContract.ChatHistory.FIELD_CHAT_TYPE + "=?",
                        new String[]{"" + DatabaseContract.ChatHistory.CHAT_TYPE_P2P},
                        DatabaseContract.ChatHistory.FIELD_TIMESTAMP)) {
            while (c.moveToNext()) {
                send(c);
            }
        }
    }

    private void send(final Cursor c) {
        if (!jaxmpp.isConnected()) {
            return;
        }
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
        final int messageType = c
                .getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE));
        final String localContent = c.getString(
                c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI));

        try {
            final ContentValues values = new ContentValues();
            final Message msg = Message.create();
            msg.setTo(toJid);
            msg.setType(StanzaType.chat);
            msg.setThread(threadId);
            msg.setBody(Crypto.encodeBody(body));
            msg.setId(stanzaId);

            addOOB(messageType, oobData, msg, values);

            if (messageType == DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE &&
                    localContent != null &&
                    oobData == null) {
                UploadedFileEvent uploadedFileEvent = new UploadedFileEvent(null, id, msg, values,
                        jaxmpp, null, false);

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
                        toJid.getBareJid().getLocalpart(), uploadedFileEvent));
            } else {
                send(id, msg, values);
            }
        } catch (Exception e) {
            Log.w(TAG, "Cannot send unsent message", e);
        }
    }

    private void send(final int id, final Message msg,
                      final ContentValues values) throws JaxmppException {
        final MessageModule messageModule = jaxmpp.getModule(MessageModule.class);

        messageModule.sendMessage(msg);
        values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                DatabaseContract.ChatHistory.STATE_OUT_SENT);
        context.getContentResolver()
                .update(Uri.parse(TigaseSdk.getInstance().getChatHistoryUri() + "/" +
                        sessionObject.getUserBareJid() + "/" +
                        msg.getTo().getBareJid() + "/" + id), values, null, null);
    }
}
