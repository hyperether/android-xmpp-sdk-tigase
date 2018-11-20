package org.tigase.messenger.phone.pro.service;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.tigase.messenger.phone.pro.db.DatabaseContract;

import java.io.IOException;
import java.io.InputStream;

public class FileDownloaderTask implements Runnable {

    private final static String TAG = "OOBDownloader";

    @Override
    public void run() {

    }

    /**
     * Checks if content is alreday downloaded.
     *
     * @param context context.
     * @param messageUri message URI to check.
     *
     * @return <code>true</code> if content is internally stored.
     */
    public static boolean isContentDownloaded(Context context, Uri messageUri) {
        final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
                DatabaseContract.ChatHistory.FIELD_DATA,
                DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI};
        try (Cursor c = context.getContentResolver().query(messageUri, cols, null, null, null)) {
            while (c.moveToNext()) {
                String contentUri = c.getString(
                        c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI));
                if (contentUri == null || contentUri.trim().isEmpty()) {
                    return false;
                } else {
                    try (InputStream in = context.getContentResolver()
                            .openInputStream(Uri.parse(contentUri))) {
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                }
            }
        }
        return false;
    }
}
