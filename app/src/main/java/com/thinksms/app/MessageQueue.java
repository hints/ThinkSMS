package com.thinksms.app;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by dtreiman on 4/27/14.
 */
public class MessageQueue {
    private static final String TAG = "MessageQueue";
    private static MessageQueue messageQueue = null;

    public static MessageQueue getInstance() {
        if (messageQueue == null) {
            messageQueue = new MessageQueue();
        }
        return messageQueue;
    }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ArrayList<Message> pendingMessages = new ArrayList<Message>();


    public void queueMessage(Message message) {
        Log.d(TAG, "Queueing message: " + message.text);
        pendingMessages.add(message);
    }


    public void deliverPendingMessagesOnUIThread() {
        ChatActivity.getInstance().runOnUiThread(new Runnable() {
            public void run() {
                deliverPendingMessages();
            }
        });
    }

    public void deliverPendingMessages() {
        for (Message message : pendingMessages) {
            Log.d(TAG, "Delivering queued message: " + message.text);
            ChatActivity activity = ChatActivity.getInstance();
            NotifyIntentService.presentNotificationForMessage(activity.getBaseContext(), message.text, message.getEmoticon());
            ((TextView) activity.findViewById(R.id.messageText)).setText(message.getEmoticon() + " " + message.text);
        }
        pendingMessages.clear();
    }

    public void queuePopState(long delay) { // delay in seconds
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                ChatActivity.getInstance().popEmotionState();
                if (!ChatActivity.getInstance().doNotDisturb()) {
                    deliverPendingMessagesOnUIThread();
                }
            }
        }, delay, TimeUnit.SECONDS);
    }
}
