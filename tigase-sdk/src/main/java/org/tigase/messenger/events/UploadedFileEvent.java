package org.tigase.messenger.events;

import android.content.ContentValues;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;

public class UploadedFileEvent {
    private String uri;
    private String mimeType;
    private String fileId;
    private String destination;
    private int chatId;
    private Message msg;
    private ContentValues values;
    private Jaxmpp jaxmpp;
    private Room room;
    private boolean isGroupIM;

    public UploadedFileEvent(String fileId,
                             int chatId,
                             Message msg,
                             ContentValues values,
                             Jaxmpp jaxmpp,
                             Room room,
                             boolean groupIM) {
        this.fileId = fileId;
        this.chatId = chatId;
        this.msg = msg;
        this.values = values;
        this.jaxmpp = jaxmpp;
        this.room = room;
        this.isGroupIM = groupIM;
    }

    public Jaxmpp getJaxmpp() {
        return jaxmpp;
    }

    public void setJaxmpp(Jaxmpp jaxmpp) {
        this.jaxmpp = jaxmpp;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public ContentValues getValues() {
        return values;
    }

    public void setValues(ContentValues values) {
        this.values = values;
    }

    public Message getMsg() {
        return msg;
    }

    public void setMsg(Message msg) {
        this.msg = msg;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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

    public boolean isGroupIM() {
        return isGroupIM;
    }

    public void setGroupIM(boolean groupIM) {
        isGroupIM = groupIM;
    }
}
