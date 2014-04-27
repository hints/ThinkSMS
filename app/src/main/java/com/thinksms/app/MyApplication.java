package com.thinksms.app;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.PushService;

/**
 * Created by dtreiman on 4/26/14.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, "bEZYPGcwyfhYuRCUHb5rUQCMuChiWDl5RBjqRMG1", "GqfwDAXN23bwkBY8xvCk405d4NIPDljb1JFmmoUL");
        PushService.setDefaultPushCallback(this, ChatActivity.class);
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }
}
