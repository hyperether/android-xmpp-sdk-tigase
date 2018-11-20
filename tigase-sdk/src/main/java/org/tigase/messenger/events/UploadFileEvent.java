package org.tigase.messenger.events;

import android.net.Uri;

public class UploadFileEvent {
    private Uri content;
    private String mimeType;
    private String destination;
    private UploadedFileEvent uploadedFileEvent;

    public UploadFileEvent(Uri content, String mimeType, String destination,
                           UploadedFileEvent event) {
        this.content = content;
        this.mimeType = mimeType;
        this.destination = destination;
        this.uploadedFileEvent = event;
    }

    public Uri getContent() {
        return content;
    }

    public void setContent(Uri content) {
        this.content = content;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public UploadedFileEvent getUploadedFileEvent() {
        return uploadedFileEvent;
    }

    public void setUploadedFileEvent(UploadedFileEvent uploadedFileEvent) {
        this.uploadedFileEvent = uploadedFileEvent;
    }
}
