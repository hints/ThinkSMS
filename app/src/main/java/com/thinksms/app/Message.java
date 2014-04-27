package com.thinksms.app;

import android.graphics.Bitmap;
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
}
