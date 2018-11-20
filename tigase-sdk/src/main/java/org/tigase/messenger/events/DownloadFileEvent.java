package org.tigase.messenger.events;

public class DownloadFileEvent {
    private String fileUrl;
    private String downloadDestination;
    private DownloadedFileEvent downloadedFileEvent;

    public DownloadFileEvent(String fileUrl,
                             String downloadDestination,
                             DownloadedFileEvent downloadedFileEvent) {
        this.fileUrl = fileUrl;
        this.downloadDestination = downloadDestination;
        this.downloadedFileEvent = downloadedFileEvent;
    }

    public DownloadFileEvent(String fileUrl, String downloadDestination) {
        this.fileUrl = fileUrl;
        this.downloadDestination = downloadDestination;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getDownloadDestination() {
        return downloadDestination;
    }

    public void setDownloadDestination(String downloadDestination) {
        this.downloadDestination = downloadDestination;
    }

    public DownloadedFileEvent getDownloadedFileEvent() {
        return downloadedFileEvent;
    }

    public void setDownloadedFileEvent(DownloadedFileEvent downloadedFileEvent) {
        this.downloadedFileEvent = downloadedFileEvent;
    }
}
