package org.tigase.messenger.phone.pro.conversations.chat;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.tigase.messenger.TigaseSdk;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.service.XMPPService;

import java.util.Collection;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;

public class ChatUtils {

    private static final String TAG = ChatUtils.class.getSimpleName();
    private static ChatUtils instance;
    private Handler mHandler;

    private ChatUtils() {

    }

    public static ChatUtils getInstance() {
        if (instance == null)
            instance = new ChatUtils();
        return instance;
    }

    public void updateChat(String searchText, OnCursorChanged callback) {
        getChatHandler().post(new Runnable() {
            @Override
            public void run() {
                final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
                        DatabaseContract.OpenChats.FIELD_ACCOUNT,
                        DatabaseContract.OpenChats.FIELD_JID, ChatProvider.FIELD_NAME,
                        ChatProvider.FIELD_UNREAD_COUNT,
                        DatabaseContract.OpenChats.FIELD_TYPE,
                        ChatProvider.FIELD_CONTACT_PRESENCE, ChatProvider.FIELD_LAST_MESSAGE,
                        ChatProvider.FIELD_LAST_MESSAGE_TIMESTAMP,
                        ChatProvider.FIELD_LAST_MESSAGE_STATE};

//                String searchText = params != null ? params[0] : null;

                String selection;
                String[] args;
                if (searchText == null) {
                    selection = null;
                    args = null;
                } else {
                    selection = ChatProvider.FIELD_NAME + " like ? OR " +
                            DatabaseContract.OpenChats.TABLE_NAME + "." +
                            DatabaseContract.OpenChats.FIELD_JID + " like ?";
                    args = new String[]{"%" + searchText + "%", "%" + searchText + "%"};

                }
                Cursor cursor = TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                        .query(TigaseSdk.getInstance().getOpenChatsUri(), cols, selection, args,
                                ChatProvider.FIELD_LAST_MESSAGE_TIMESTAMP + " DESC");

                callback.onChanged(cursor);
            }
        });
    }

    public void onArchiveChat(Collection<Long> chatsId, XMPPService service) {
        if (service == null) {
            Log.w("OpenChatItemFragment", "Service is not binded");
            return;
        }

        final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
                DatabaseContract.OpenChats.FIELD_ACCOUNT,
                DatabaseContract.OpenChats.FIELD_JID};
        for (long chatId : chatsId) {
            String account;
            try (Cursor c = TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                    .query(ContentUris.withAppendedId(TigaseSdk.getInstance().getOpenChatsUri(), chatId), cols,
                            null, null, null)) {
                if (c.moveToNext()) {
                    account = c
                            .getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
                } else {
                    continue;
                }
            }

            final Jaxmpp jaxmpp = service.getJaxmpp(account);

            if (jaxmpp == null) {
                Log.w("OpenChatItemFragment", "There is no account " + account);
                return;
            }

            Intent i = new Intent(XMPPService.LAST_ACCOUNT_ACTIVITY);
            i.putExtra("account", jaxmpp.getSessionObject().getUserBareJid().toString());
            service.sendBroadcast(i);

            getChatHandler().post(new Runnable() {
                @Override
                public void run() {
                    Chat chat = null;
                    for (Chat c : jaxmpp.getModule(MessageModule.class).getChats()) {
                        if (c.getId() == chatId) {
                            chat = c;
                            break;
                        }
                    }
                    if (chat != null) {
                        try {
                            jaxmpp.getModule(MessageModule.class).close(chat);
                        } catch (JaxmppException e) {
                            Log.e("OpenChat", "Cannot close chat", e);
                        }
                    }
                }
            });
        }
    }

    public void onLeaveRoom(Collection<Long> chatsId, XMPPService service) {

        final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
                DatabaseContract.OpenChats.FIELD_ACCOUNT,
                DatabaseContract.OpenChats.FIELD_JID};
        for (long chatId : chatsId) {
            String account;
            String roomJID;
            try (Cursor c = TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                    .query(ContentUris.withAppendedId(TigaseSdk.getInstance().getOpenChatsUri(), chatId), cols,
                            null, null, null)) {
                if (c.moveToNext()) {
                    account = c
                            .getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
                    roomJID = c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_JID));
                } else {
                    continue;
                }
            }

            getChatHandler().post(new Runnable() {
                @Override
                public void run() {
                    Jaxmpp jaxmpp = service.getJaxmpp(account);
                    MucModule mucModule = jaxmpp.getModule(MucModule.class);
                    Room room = mucModule.getRoom(BareJID.bareJIDInstance(roomJID));
                    if (room != null) {
                        try {
                            mucModule.leave(room);
                        } catch (JaxmppException e) {
                            Log.e(TAG, "Cannot leave room", e);
                        }
                    }
                }
            });
        }
    }


    public void doDeleteChat(final Collection<Long> chatsId, XMPPService service) {
        if (service == null) {
            Log.w("OpenChatItemFragment", "Service is not binded");
            return;
        }

        final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
                DatabaseContract.OpenChats.FIELD_ACCOUNT,
                DatabaseContract.OpenChats.FIELD_JID};
        for (long chatId : chatsId) {
            String account;
            try (Cursor c = TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                    .query(ContentUris.withAppendedId(TigaseSdk.getInstance().getOpenChatsUri(), chatId), cols,
                            null, null, null)) {
                if (c.moveToNext()) {
                    account = c
                            .getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
                } else {
                    continue;
                }
            }

            final Jaxmpp jaxmpp = service.getJaxmpp(account);

            if (jaxmpp == null) {
                Log.w("OpenChatItemFragment", "There is no account " + account);
                return;
            }

            Intent i = new Intent(XMPPService.LAST_ACCOUNT_ACTIVITY);
            i.putExtra("account", jaxmpp.getSessionObject().getUserBareJid().toString());
            service.sendBroadcast(i);

            getChatHandler().post(new Runnable() {
                @Override
                public void run() {
                    Chat chat = null;
                    for (Chat c : jaxmpp.getModule(MessageModule.class).getChats()) {
                        if (c.getId() == chatId) {
                            chat = c;
                            break;
                        }
                    }
                    if (chat != null) {
                        try {
                            jaxmpp.getModule(MessageModule.class).close(chat);
                            Uri chatHistoryUri = Uri.parse(
                                    TigaseSdk.getInstance().getChatHistoryUri() + "/" + account + "/" +
                                            chat.getJid().getBareJid());
                            TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                                    .delete(chatHistoryUri, null, null);
                        } catch (Exception e) {
                            Log.e("OpenChat", "Cannot delete chat", e);
                        }

                    }
                }
            });
        }
    }

    private Handler getChatHandler() {
        if (mHandler == null) {
            HandlerThread handlerThread = new HandlerThread("chat_thread");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper());
        }
        return mHandler;
    }
}
