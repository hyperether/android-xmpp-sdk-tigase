package org.tigase.messenger.phone.pro.conversations.chat;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.utils.AccountHelper;

import java.util.Calendar;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.UIDGenerator;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.mam.MessageArchiveManagementModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.utils.RSM;

public class FetchMoreHistoryTask
        extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "FetchMore";
    private final Account account;
    private final Uri chatUri;
    private final Context context;
    private final Jaxmpp jaxmpp;
    private final JID jid;
    private final AccountManager mAccountManager;
    private final MessageArchiveManagementModule mam;
    private final SwipeRefreshLayout swipeRefresh;

    public FetchMoreHistoryTask(Context context, SwipeRefreshLayout swipeRefresh, Jaxmpp jaxmpp,
                                JID jid, Uri uri) {
        this.context = context;
        this.swipeRefresh = swipeRefresh;
        this.jaxmpp = jaxmpp;
        this.chatUri = uri;
        this.jid = jid;

        this.mam = this.jaxmpp.getModule(MessageArchiveManagementModule.class);
        this.mAccountManager = AccountManager.get(context);
        this.account = AccountHelper
                .getAccount(mAccountManager, jaxmpp.getSessionObject().getUserBareJid().toString());
    }

    @Override
    protected Void doInBackground(Void... voids) {
        String syncTime = mAccountManager.getUserData(account, AccountsConstants.MAM_SYNC_TIME);
        final int hours = syncTime == null ? 24 : Integer.valueOf(syncTime);

        Calendar endDate = getFirstMessageDate();
        Calendar startDate = (Calendar) endDate.clone();
        startDate.add(Calendar.HOUR, -hours);

        MessageArchiveManagementModule.Query q = new MessageArchiveManagementModule.Query();
        q.setStart(startDate.getTime());
        q.setEnd(endDate.getTime());
        q.setWith(this.jid);

        final RSM rsm = new RSM();
        final String queryId = UIDGenerator.next() + UIDGenerator.next() + UIDGenerator.next();
        try {
            mam.queryItems(q, queryId, rsm, new MessageArchiveManagementModule.ResultCallback() {
                @Override
                public void onError(Stanza responseStanza,
                                    XMPPException.ErrorCondition error) throws JaxmppException {
                    Log.i(TAG, "ERROR on retrieve " + error);
                }

                @Override
                public void onSuccess(String queryid, boolean complete,
                                      RSM rsm) throws JaxmppException {
                    Log.i(TAG, "Done " + queryId + ";  " + complete);
                }

                @Override
                public void onTimeout() throws JaxmppException {
                    Log.i(TAG, "TIMEOUT on retrieve ");

                }
            });

        } catch (Exception e) {
            Log.w(TAG, "Cannot fetch history", e);
        }

        return null;
    }

    private Calendar getFirstMessageDate() {
        final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
                DatabaseContract.ChatHistory.FIELD_TIMESTAMP};

        try (Cursor cursor = context.getContentResolver()
                .query(this.chatUri, cols, null, null,
                        DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " ASC")) {
            if (cursor.moveToNext()) {
                long t = cursor.getLong(
                        cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_TIMESTAMP));
                Calendar result = Calendar.getInstance();
                result.setTimeInMillis(t);
                return result;
            }
        }
        return Calendar.getInstance();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (this.swipeRefresh != null) {
            this.swipeRefresh.setRefreshing(false);
        }
    }
}
