package com.thinksms.app;

import android.graphics.Bitmap;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;


/**
 * Created by dtreiman on 4/26/14.
 */
public class Message {

    public String text;
    public List<String> traits;

    public Message(String messageText, List<String> messageTraits) {
        text = messageText;
        traits = messageTraits;
    }

    public String getEmoticon() {
        return ":-)";
    }

    public int getSmallIcon() {
        return R.drawable.microphone;
    }

    public Bitmap getLargeIcon() {
        return null;
    }


    public void send() {

        JSONObject data = new JSONObject();
        try {
            JSONObject dataFields = new JSONObject();
            data.put("action", "com.thinksms.app.MESSAGE_RECEIVED");
            dataFields.put("text", text);
            dataFields.put("emoticon", getEmoticon());
            data.put("data", dataFields);
            ParseQuery<ParseInstallation> allInstallations = ParseInstallation.getQuery(); // <-- Installation query
            ParsePush push = new ParsePush();
            push.setQuery(allInstallations);
            push.setData(data);
            push.sendInBackground();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
