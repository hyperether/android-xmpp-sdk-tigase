package org.tigase.messenger.phone.pro.conversations;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.View;

import org.tigase.messenger.interfaces.ContactHandler;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectFragment;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import messenger.tigase.org.tigasesdk.R;
import tigase.jaxmpp.core.client.BareJID;

public class ViewHolderMsg
        extends AbstractViewHolder {

    public ViewHolderMsg(View itemView, MultiSelectFragment fragment) {
        super(itemView, fragment);
    }

    @Override
    public void bind(Context context, Cursor cursor) {
        final int id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
        final String jid = cursor
                .getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_JID));
        final String body = cursor
                .getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
        final long timestampt = cursor
                .getLong(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_TIMESTAMP));
        final int state = cursor
                .getInt(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STATE));
        final String nickname = cursor.getString(
                cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME));

        mContentView.setText(body);
        mTimestamp.setText(
                DateUtils.getRelativeDateTimeString(context, timestampt, DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.WEEK_IN_MILLIS, 0));

        if (mNickname != null) {
            mNickname.setVisibility(View.VISIBLE);
            int col = ContextCompat.getColor(context, getColor(nickname));
            mNickname.setTextColor(col);
            String name = ContactHandler.getUserDisplayName(nickname);
            mNickname.setText(name);
        }

        boolean mentioned = (state == DatabaseContract.ChatHistory.STATE_INCOMING ||
                state == DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD) && body != null &&
                ownNickname != null &&
                body.toLowerCase().contains(ownNickname.toLowerCase());
        if (mentioned) {
            mContentView.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            mContentView.setTypeface(Typeface.DEFAULT);
        }

        if (mDeliveryStatus != null) {
            switch (state) {
                case DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT:
                    mDeliveryStatus.setImageResource(R.drawable.ic_message_not_sent_24dp);
                    break;
                case DatabaseContract.ChatHistory.STATE_OUT_SENT:
                    mDeliveryStatus.setImageResource(R.drawable.ic_message_sent_24dp);
                    break;
                case DatabaseContract.ChatHistory.STATE_OUT_DELIVERED:
                    mDeliveryStatus.setImageResource(R.drawable.ic_message_delivered_24dp);
                    break;
            }
        }
        if (mAvatar != null) {
            AvatarHelper.setAvatarToImageView(BareJID.bareJIDInstance(jid), mAvatar);
        }
    }

    private int getColor(String nickname) {
        if (nickname != null) {
            final int i = ((Math.abs(nickname.hashCode()) + 3) * 13) % 19;
            switch (i) {
                case 0:
                    return R.color.mucmessage_his_nickname_0;
                case 1:
                    return R.color.mucmessage_his_nickname_1;
                case 2:
                    return R.color.mucmessage_his_nickname_2;
                case 3:
                    return R.color.mucmessage_his_nickname_3;
                case 4:
                    return R.color.mucmessage_his_nickname_4;
                case 5:
                    return R.color.mucmessage_his_nickname_5;
                case 6:
                    return R.color.mucmessage_his_nickname_6;
                case 7:
                    return R.color.mucmessage_his_nickname_7;
                case 8:
                    return R.color.mucmessage_his_nickname_8;
                case 9:
                    return R.color.mucmessage_his_nickname_9;
                case 10:
                    return R.color.mucmessage_his_nickname_10;
                case 11:
                    return R.color.mucmessage_his_nickname_11;
                case 12:
                    return R.color.mucmessage_his_nickname_12;
                case 13:
                    return R.color.mucmessage_his_nickname_13;
                case 14:
                    return R.color.mucmessage_his_nickname_14;
                case 15:
                    return R.color.mucmessage_his_nickname_15;
                case 16:
                    return R.color.mucmessage_his_nickname_16;
                case 17:
                    return R.color.mucmessage_his_nickname_17;
                case 18:
                    return R.color.mucmessage_his_nickname_18;
                case 19:
                    return R.color.mucmessage_his_nickname_19;
                default:
                    return R.color.mucmessage_his_nickname_0;
            }
        }
        return R.color.mucmessage_his_nickname_0;
    }

    @Override
    protected void onItemClick(View v) {

    }

    @Override
    public String toString() {
        return super.toString() + " '" + mContentView.getText() + "'";
    }

}
