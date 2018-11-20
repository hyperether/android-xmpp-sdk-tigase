package org.tigase.messenger.interfaces;

/**
 * Created by Slobodan on 7/8/2016.
 */
public class NotificationHandler {
    private static NotificationHandler ourInstance = new NotificationHandler();

    public static final String TAG = "NotificationHandler";

    public static NotificationHandler getInstance() {
        return ourInstance;
    }

    private NotificationHandler() {

    }

    public void notifyError(String title, String text) {
        //TODO add
    }

    public boolean isFocusedOnRoomId(long id) {

        // todo add
        // register activity lifecycle callbacks and track opened room
        // id == focusedOnRoomId.getOpenChatId()
        return false;
    }

    public boolean isFocusedOnChatId(long id) {

        // todo add
        // register activity lifecycle callbacks and track opened room
        // id == focusedOnRoomId.getOpenChatId()
        return false;
    }
}
