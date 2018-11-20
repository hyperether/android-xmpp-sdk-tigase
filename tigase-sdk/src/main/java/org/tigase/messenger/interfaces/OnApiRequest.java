package org.tigase.messenger.interfaces;

public interface OnApiRequest {

    void onSuccess();

    void onError(String err);
}
