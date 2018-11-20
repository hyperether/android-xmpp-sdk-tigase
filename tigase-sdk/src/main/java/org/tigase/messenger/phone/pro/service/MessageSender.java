package org.tigase.messenger.phone.pro.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import com.hyperether.toolbox.HyperLog;

import org.tigase.messenger.TigaseSdk;
import org.tigase.messenger.interfaces.DataCache;
import org.tigase.messenger.interfaces.OnApiRequest;
import org.tigase.messenger.jaxmpp.android.chat.AndroidChatManager;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.utils.Crypto;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Date;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementBuilder;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;

public class MessageSender {

    public static final String SEND_CHAT_MESSAGE_ACTION = "org.tigase.messenger.phone.pro.service.MessageSender.SEND_CHAT_MESSAGE";
    public static final String SEND_GROUPCHAT_MESSAGE_ACTION = "org.tigase.messenger.phone.pro.service.MessageSender.SEND_GROUPCHAT_MESSAGE";
    public final static String CHAT_ID = "CHAT_ID";
    public final static String BODY = "BODY";
    public final static String ACCOUNT = "ACCOUNT";
    /**
     * Local content to send URI.
     */
    public final static String LOCAL_CONTENT_URI = "LOCAL_CONTENT_URI";
    public final static String ROOM_JID = "ROOM_JID";
    private final static String TAG = "MessageSender";
    private final XMPPService service;

    private static Uri copyLocalImageToAlbum(Context context,
                                             Uri localContentUri) throws IOException {
        final ContentValues values = new ContentValues();

        Bitmap bmp = getBitmapFromUri(context, localContentUri);
        String imu = MediaStore.Images.Media
                .insertImage(context.getContentResolver(), bmp, "Sent image", "");

        return Uri.parse(imu);
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver()
                .openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    public static String getMimeType(final Context context, final Uri localContentUri) {
        String displayName = null;
        try (Cursor cursor = context.getContentResolver()
                .query(localContentUri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

            }
        }
        return FileUploaderTask.guessMimeType(displayName);
    }

    public MessageSender(XMPPService xmppService) {
        this.service = xmppService;
    }

    private Chat getChat(final BareJID account, int chatId) {
        if (service == null) {
            Log.w("ChatItemFragment", "Service is not binded");
            return null;
        }

        Jaxmpp jaxmpp = service.getJaxmpp(account);

        if (jaxmpp == null) {
            Log.w("ChatItemFragment", "There is no account " + account);
            return null;
        }

        AbstractChatManager chatManager = jaxmpp.getModule(MessageModule.class).getChatManager();

        if (chatManager instanceof AndroidChatManager) {
            return ((AndroidChatManager) chatManager).getChat(chatId);
        } else {
            for (Chat chat : chatManager.getChats()) {
                if (chat.getId() == chatId) {
                    return chat;
                }
            }
        }

        return null;
    }

    public void process(Context context, Intent intent) {
        switch (intent.getAction()) {
            case SEND_CHAT_MESSAGE_ACTION:
                sendChatMessage(context, intent);
                break;
            case SEND_GROUPCHAT_MESSAGE_ACTION:
                sendGroupchatMessage(context, intent);
                break;
            default:
                Log.wtf(TAG, "Unknown action: " + intent.getAction());
                throw new RuntimeException("Unknown action: " + intent.getAction());
        }
    }

    private void sendChatMessage(final Context context, Intent intent) {
        final int chatId = intent.getIntExtra(CHAT_ID, 0);
        final String body = intent.getStringExtra(BODY);
        final BareJID account = BareJID.bareJIDInstance(intent.getStringExtra(ACCOUNT));
        final Uri localContentUri = intent.getParcelableExtra(LOCAL_CONTENT_URI);

//		if (localContentUri != null) {
//			context.getContentResolver()
//					.takePersistableUriPermission(localContentUri, intent.getFlags() &
//							(Intent.FLAG_GRANT_READ_URI_PERMISSION + Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
//		}

        final Chat chat = getChat(account, chatId);
        if (chat == null) {
            HyperLog.getInstance().e(TAG, "sendChatMessage",
                    "Chat NULL for " + chatId + " and " + account.getLocalpart());
            return;
        }

        final JID recipient = chat.getJid();
        final String to = recipient.getBareJid().toString();

        int state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
        Message msg;
        String stanzaId = null;
        final ContentValues values = new ContentValues();
        int itemType = DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;
        Jaxmpp jaxmpp = null;
        try {
            jaxmpp = service.getJaxmpp(chat.getSessionObject().getUserBareJid());
            msg = chat.createMessage(Crypto.encodeBody(body));
            stanzaId = msg.getId();
            MessageModule m = jaxmpp.getModule(MessageModule.class);

            if (DataCache.getInstance().isE164(to)) {
                DataCache.getInstance()
                        .sendSms(to, body, new OnApiRequest() {
                            @Override
                            public void onSuccess() {
//                                ContentValues values = new ContentValues();
                                values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                                        DatabaseContract.ChatHistory.STATE_OUT_SENT);
                                Uri uri = Uri
                                        .parse(TigaseSdk.getInstance().getChatHistoryUri() + "/" +
                                                account + "/" + recipient.getBareJid());
                                TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                                        .update(uri, values, null, null);
                                TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                                        .notifyChange(uri, null);
                            }

                            @Override
                            public void onError(String err) {

                            }
                        });

            } else if (localContentUri != null) {
                state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
                final String mimeType = getMimeType(context, localContentUri);
                if (mimeType.startsWith("image/")) {
                    itemType = DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE;
                } else if (mimeType.startsWith("video/")) {
                    itemType = DatabaseContract.ChatHistory.ITEM_TYPE_VIDEO;
                } else {
                    itemType = DatabaseContract.ChatHistory.ITEM_TYPE_FILE;
                }
            } else if (jaxmpp.isConnected()) {
                m.sendMessage(msg);
                state = DatabaseContract.ChatHistory.STATE_OUT_SENT;
                itemType = DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;
            } else {
                state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
                itemType = DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;
            }
        } catch (Exception e) {
            state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
            Log.w("ChatItemFragment", "Cannot send message", e);
        }

        values.put(DatabaseContract.ChatHistory.FIELD_CHAT_TYPE,
                DatabaseContract.ChatHistory.CHAT_TYPE_P2P);
        values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_JID,
                chat.getSessionObject().getUserBareJid().toString());
        values.put(DatabaseContract.ChatHistory.FIELD_JID, recipient.getBareJid().toString());
        values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, new Date().getTime());
        values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
        values.put(DatabaseContract.ChatHistory.FIELD_THREAD_ID, chat.getThreadId());
        values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT,
                chat.getSessionObject().getUserBareJid().toString());
        values.put(DatabaseContract.ChatHistory.FIELD_STATE, state);
        values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, itemType);
        if (stanzaId != null) {
            values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, stanzaId);
        }

        if (localContentUri != null) {
            try {
                Uri imageUri = copyLocalImageToAlbum(context, localContentUri);
                values.put(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI,
                        imageUri.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Uri uri = Uri.parse(TigaseSdk.getInstance().getChatHistoryUri() + "/" + account + "/" +
                recipient.getBareJid());
        Uri u = TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                .insert(uri, values);

        if (u != null) {
            TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                    .notifyChange(u, null);
        }

        if (localContentUri != null && jaxmpp != null) {
            SendUnsentMessages sum = new SendUnsentMessages(context, jaxmpp);
            sum.run(u);
        }
    }

    private void sendGroupchatMessage(final Context context, final Intent intent) {
        final String body = intent.getStringExtra(BODY);
        final String url = intent.getStringExtra("oob:url");
        final BareJID account = BareJID.bareJIDInstance(intent.getStringExtra(ACCOUNT));
        final BareJID roomJID = BareJID.bareJIDInstance(intent.getStringExtra(ROOM_JID));
        final Jaxmpp jaxmpp = service.getJaxmpp(account);
        final Uri localContentUri = intent.getParcelableExtra(LOCAL_CONTENT_URI);
        Room room = jaxmpp.getModule(MucModule.class).getRoom(roomJID);

        int state;
        Message msg;
        String stanzaId = null;
        int itemType = DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;

        try {
            msg = room.createMessage(Crypto.encodeBody(body));
            stanzaId = msg.getId();

            if (url != null) {
                Element x = ElementBuilder.create("x", "jabber:x:oob").child("url").setValue(url)
                        .getElement();
                msg.addChild(x);
            }

            if (localContentUri != null) {
                state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
                final String mimeType = getMimeType(context, localContentUri);
                if (mimeType != null && mimeType.startsWith("image/")) {
                    itemType = DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE;
                } else if (mimeType != null && mimeType.startsWith("video/")) {
                    itemType = DatabaseContract.ChatHistory.ITEM_TYPE_VIDEO;
                } else {
                    itemType = DatabaseContract.ChatHistory.ITEM_TYPE_FILE;
                }
            } else if (jaxmpp.isConnected() && room.getState() == Room.State.joined) {
                room.sendMessage(msg);
                state = DatabaseContract.ChatHistory.STATE_OUT_SENT;
            } else {
                state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
            }
        } catch (Exception e) {
            state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
            Log.w("MucItemFragment", "Cannot send message", e);
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.ChatHistory.FIELD_JID, room.getRoomJid().toString());
        values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME, room.getNickname());
        values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, System.currentTimeMillis());
        values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, stanzaId);

        values.put(DatabaseContract.ChatHistory.FIELD_STATE, state);
        values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);

        values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, itemType);
        values.put(DatabaseContract.ChatHistory.FIELD_CHAT_TYPE,
                DatabaseContract.ChatHistory.CHAT_TYPE_MUC);

        values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT,
                room.getSessionObject().getUserBareJid().toString());

        if (localContentUri != null) {
            try {
                Uri imageUri = copyLocalImageToAlbum(context, localContentUri);
                values.put(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI,
                        imageUri.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Uri uri = Uri.parse(TigaseSdk.getInstance().getMucHistoryUri() + "/" +
                room.getSessionObject().getUserBareJid() + "/" +
                Uri.encode(room.getRoomJid().toString()));
        Uri x = TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                .insert(uri, values);
        if (x != null) {
            TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                    .notifyChange(x, null);
        }

        if (localContentUri != null) {
            SendUnsentGroupMessages sum = new SendUnsentGroupMessages(context, jaxmpp, room);
            sum.run(x);
        }
    }

}
