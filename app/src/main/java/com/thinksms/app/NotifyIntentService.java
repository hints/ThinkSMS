package com.thinksms.app;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.preview.support.v4.app.NotificationManagerCompat;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * helper methods.
 */
public class NotifyIntentService extends IntentService {
    private static final String TAG = "NotifyIntentService";

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_NOTIFY = "com.thinksms.app.action.NOTIFY";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM_TEXT = "com.thinksms.app.extra.TEXT";
    private static final String EXTRA_PARAM_EMOTICON = "com.thinksms.app.extra.EMOTICON";

    public static void presentNotificationForMessage(Context context, String text, String emoticon) {
        Intent intent = new Intent(context, NotifyIntentService.class);
        intent.setAction(ACTION_NOTIFY);
        intent.putExtra(EXTRA_PARAM_TEXT, text);
        intent.putExtra(EXTRA_PARAM_EMOTICON, emoticon);
        context.startService(intent);
    }

    public NotifyIntentService() {
        super("NotifyIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_NOTIFY.equals(action)) {
                final String text = intent.getStringExtra(EXTRA_PARAM_TEXT);
                final String emoticon = intent.getStringExtra(EXTRA_PARAM_EMOTICON);
                presentNativeNotification(text, emoticon);
                presentToqNotification(text, emoticon);
            }
        }
    }


    private void presentNativeNotification(String text, String emoticon) {
        Time now = new Time();
        now.setToNow();
        int notificationId = (int)(now.toMillis(true) / 1000); // This is a hack
        // Build intent for notification content
        Intent viewIntent = new Intent(getApplicationContext(), ChatActivity.class);
        //viewIntent.putExtra(EXTRA_EVENT_ID, eventId);
        PendingIntent viewPendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, viewIntent, 0);
        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(text);

        int smallIcon = EmoticonMap.foxImageForEmoticon(emoticon);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(smallIcon)
//                        .setLargeIcon(placeBitmap)
                        .setContentTitle(emoticon)
                        .setContentText(text)
                        .setContentIntent(viewPendingIntent)
                        .setStyle(bigStyle);

// Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());

// Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void presentToqNotification(String text, String emoticon) {
        Log.d(TAG, "NotifyIntentService.sendNotification");

        String title = emoticon;
        String message [] = {text};

        // Create notification text card from UI values
        NotificationTextCard notificationCard= new NotificationTextCard(System.currentTimeMillis(),
                title, // ((EditText)findViewById(R.id.notification_title_text)).getText().toString(),
                message // splitString(((EditText)findViewById(R.id.notification_message_text)).getText().toString())
        );

        Boolean doNotDisturb = true;
        EmotionState state = ChatActivity.getInstance().getEmotionState();
        if (state != null) {
            if ("Tired".equals(state.intent)) {
                doNotDisturb = doNotDisturb;
            }
            else if ("Sleeping".equals(state.intent)) {
                doNotDisturb = doNotDisturb;
            }
            else if ("Busy".equals(state.intent)) {
                doNotDisturb = doNotDisturb;
            }

        }
        notificationCard.setInfoText(text);
        String menuOptions [] = {"Reply"};
        notificationCard.setMenuOptions(menuOptions);
        notificationCard.setReceivingEvents(true);
        notificationCard.setShowDivider(false);
        // notificationCard.setShowDivider(((CheckBox)findViewById(R.id.notification_divider_checkbox)).isChecked());
        notificationCard.setVibeAlert(!doNotDisturb);

        RemoteToqNotification notification= new RemoteToqNotification(this, notificationCard);

        try {
            DeckOfCardsManager deckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
            deckOfCardsManager.sendNotification(notification);
        }
        catch (RemoteDeckOfCardsException e){
            //Toast.makeText(this, "Error sending Toq notification", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "ToqApiDemo.sendNotification - error sending notification", e);
        }
    }
}
