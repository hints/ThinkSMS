package com.thinksms.app;

/**
 * Created by dtreiman on 4/26/14.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by dtreiman on 4/26/14.
 */
public class MessageReceiver extends BroadcastReceiver {
    private static final String TAG = "MessageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Message Received");

        try {
            String action = intent.getAction();
            String channel = intent.getExtras().getString("com.parse.Channel");
            JSONObject json = new JSONObject(intent.getExtras().getString("com.parse.Data"));

            JSONObject dataObject = json.getJSONObject("data");

            String text = dataObject.getString("text");

            NotifyIntentService.presentNotificationForMessage(context, text);
        }
        catch (JSONException e) {
            Log.d(TAG, "JSONException: " + e.getMessage());
        }
    }

}
