package org.tigase.messenger.interfaces;

public abstract class ContactHandler {

    /**
     * Override this method to custimize usernames
     *
     * @param username
     *
     * @return
     */
    public static String getUserDisplayName(String username) {
        return username;
    }
}
