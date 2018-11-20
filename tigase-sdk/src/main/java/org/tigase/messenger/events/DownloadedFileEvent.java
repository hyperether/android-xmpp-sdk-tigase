package org.tigase.messenger.events;

import android.net.Uri;

public class DownloadedFileEvent {
    private Uri chatHistoryUri;
    private String mediaUri;

    public DownloadedFileEvent(Uri chatHistoryUri) {
        this.chatHistoryUri = chatHistoryUri;
    }

    public Uri getChatHistoryUri() {
        return chatHistoryUri;
    }

    public void setChatHistoryUri(Uri chatHistoryUri) {
        this.chatHistoryUri = chatHistoryUri;
    }

    public String getMediaUri() {
        return mediaUri;
    }

    public void setMediaUri(String mediaUri) {
        this.mediaUri = mediaUri;
    }
}
