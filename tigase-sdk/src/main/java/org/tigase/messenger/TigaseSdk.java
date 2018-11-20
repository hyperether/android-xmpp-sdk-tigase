package org.tigase.messenger;

import android.content.Context;
import android.net.Uri;

/**
 * Class for managing application related behavior and providing context
 *
 * @author Slobodan Prijic
 * @version 1.0 - 12/09/2017
 */
public class TigaseSdk {

    private static TigaseSdk instance;
    private Context context;
    private boolean debugActive;
    private String packageName;

    private Uri openChatUri;
    private Uri chatHistoryUri;
    private Uri mucHistoryUri;
    private Uri unsentMessagesUri;
    private Uri rosterUri;
    private Uri vCardUri;

    public static synchronized TigaseSdk getInstance() {
        if (instance == null) {
            instance = new TigaseSdk();
        }
        return instance;
    }

    public void setContext(Context ctxt) {
        context = ctxt;
    }

    public Context getApplicationContext() {
        return context;
    }

    public void setDebugActive(boolean debugActive) {
        this.debugActive = debugActive;
    }

    public boolean isDebugActive() {
        return debugActive;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Uri getOpenChatsUri() {
        return openChatUri;
    }

    public Uri getChatHistoryUri() {
        return chatHistoryUri;
    }

    public Uri getMucHistoryUri() {
        return mucHistoryUri;
    }

    public Uri getUnsentMessagesUri() {
        return unsentMessagesUri;
    }

    public void setOpenChatUri(Uri openChatUri) {
        this.openChatUri = openChatUri;
    }

    public void setChatHistoryUri(Uri chatHistoryUri) {
        this.chatHistoryUri = chatHistoryUri;
    }

    public void setMucHistoryUri(Uri mucHistoryUri) {
        this.mucHistoryUri = mucHistoryUri;
    }

    public void setUnsentMessagesUri(Uri unsentMessagesUri) {
        this.unsentMessagesUri = unsentMessagesUri;
    }

    public Uri getRosterUri() {
        return rosterUri;
    }

    public void setRosterUri(Uri rosterUri) {
        this.rosterUri = rosterUri;
    }

    public Uri getvCardUri() {
        return vCardUri;
    }

    public void setvCardUri(Uri vCardUri) {
        this.vCardUri = vCardUri;
    }
}
