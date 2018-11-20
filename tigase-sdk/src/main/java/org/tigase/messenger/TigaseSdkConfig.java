package org.tigase.messenger;

import android.content.Context;
import android.net.Uri;

import com.hyperether.toolbox.HyperLog;

/**
 * Class for creating toolbox configuration builder
 * <p>
 * Created by Slobodan on 12/11/2017.
 */

public class TigaseSdkConfig {

    private TigaseSdkConfig(Builder builder, Context context) {
        TigaseSdk.getInstance().setContext(context);
        TigaseSdk.getInstance().setDebugActive(builder.debug);
        TigaseSdk.getInstance().setPackageName(builder.packageName);
        TigaseSdk.getInstance().setOpenChatUri(builder.openChatUri);
        TigaseSdk.getInstance().setChatHistoryUri(builder.chatHistoryUri);
        TigaseSdk.getInstance().setMucHistoryUri(builder.mucHistoryUri);
        TigaseSdk.getInstance().setUnsentMessagesUri(builder.unsentMessagesUri);
        TigaseSdk.getInstance().setRosterUri(builder.rosterUri);
        TigaseSdk.getInstance().setvCardUri(builder.vCardUri);
    }

    public static class Builder {

        private boolean debug = false;
        private String packageName;
        private Uri openChatUri;
        private Uri chatHistoryUri;
        private Uri mucHistoryUri;
        private Uri unsentMessagesUri;
        private Uri rosterUri;
        private Uri vCardUri;

        public TigaseSdkConfig build(Context context) {
            return new TigaseSdkConfig(this, context);
        }

        /**
         * If not set default value will be false
         *
         * @param debug If is true {@link HyperLog} is set to debug mode.
         *
         * @return builder instance
         */
        public Builder setDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder setOpenChatUri(Uri openChatUri) {
            this.openChatUri = openChatUri;
            return this;
        }

        public Builder setChatHistoryUri(Uri chatHistoryUri) {
            this.chatHistoryUri = chatHistoryUri;
            return this;
        }

        public Builder setMucHistoryUri(Uri mucHistoryUri) {
            this.mucHistoryUri = mucHistoryUri;
            return this;
        }

        public Builder setUnsentMessagesUri(Uri unsentMessagesUri) {
            this.unsentMessagesUri = unsentMessagesUri;
            return this;
        }

        public Builder setRosterUri(Uri rosterUri) {
            this.rosterUri = rosterUri;
            return this;
        }

        public Builder setvCardUri(Uri vCardUri) {
            this.vCardUri = vCardUri;
            return this;
        }
    }
}
