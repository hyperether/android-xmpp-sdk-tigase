package org.tigase.messenger.interfaces;

import java.util.ArrayList;

import tigase.jaxmpp.core.client.JID;

public class DataCache {
    private static final String TAG = DataCache.class.getSimpleName();

    private static DataCache instance;
    private ArrayList<JID> invites = new ArrayList<>();

    private DataCache() {
    }

    public static synchronized DataCache getInstance() {
        if (instance == null) {
            instance = new DataCache();
        }
        return instance;
    }

    public static void clear() {
        instance = null;
    }

    public void setInvites(ArrayList<JID> i) {
        this.invites = i;
    }

    public ArrayList<JID> getInvites() {
        return invites;
    }

    public void sendSms(String to, String body, OnApiRequest callback) {
        // TODO: override and implement
    }

    public boolean isE164(String msisdn) {
        // todo add
        return false;
    }

}
