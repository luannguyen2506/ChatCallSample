package com.stringee.kit.ui.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.stringee.kit.ui.activity.ConversationActivity;
import com.stringee.kit.ui.commons.Common;
import com.stringee.kit.ui.commons.Constant;
import com.stringee.kit.ui.commons.utils.Utils;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.messaging.User;

import java.util.List;

public class NotificationService {

    private static final String CHANNEL_ID = "com.stringee.message.notification";
    private static final String CHANNEL_NAME = "Stringee Notification Channel";
    private static final String CHANNEL_DESC = "Channel for notification";

    public static void showNotification(Context context, Message message, Conversation conversation, User user) {
        if (conversation == null) {
            return;
        }
        String title = null;
        String notificationText;
        Bitmap notificationIconBitmap = null;
        int notificationId = conversation.getId();
        int iconResourceId;

        if (conversation.isGroup()) {
            title = conversation.getName();
            if (title == null || title.trim().length() == 0) {
                List<User> participants = conversation.getParticipants();
                for (int j = 0; j < participants.size(); j++) {
                    String userId = participants.get(j).getUserId();
                    if (!conversation.isGroup() && Common.client.getUserId() != null && Common.client.getUserId().equals(userId)) {
                        continue;
                    } else {
                        String name = participants.get(j).getName();
                        if (name == null || name.trim().length() == 0) {
                            name = participants.get(j).getUserId();
                        }
                        title = title + name + ",";
                    }
                }
                if (title.length() > 0) {
                    title = title.substring(0, title.length() - 1);
                }
            }
            iconResourceId = context.getResources().getIdentifier("group_icon", "drawable", context.getPackageName());
        } else {
            if (user == null) {
                return;
            }
            title = user.getName();
            if (title == null || title.trim().length() == 0) {
                title = user.getUserId();
            }
            iconResourceId = context.getResources().getIdentifier("stringee_ic_contact_picture", "drawable", context.getPackageName());
        }

        notificationText = message.getText();
        notificationIconBitmap = BitmapFactory.decodeResource(context.getResources(), iconResourceId);

        NotificationManager mNotificationManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_MAX);
            channel.setDescription(CHANNEL_DESC);
            mNotificationManager = context.getSystemService(NotificationManager.class);
            mNotificationManager.createNotificationChannel(channel);
        } else {
            mNotificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
        }

        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra(Constant.CONVERSATION, conversation);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff),
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);

        mBuilder.setSmallIcon(Utils.getLauncherIcon(context))
                .setLargeIcon(notificationIconBitmap)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(notificationText).setDefaults(Notification.DEFAULT_ALL);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);
        mBuilder.setNumber(conversation.getTotalUnread());

        mNotificationManager.notify(notificationId, mBuilder.build());
    }

    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);
    }
}
