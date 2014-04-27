package com.thinksms.app;

import java.util.Date;

/**
 * Created by dtreiman on 4/27/14.
 */
public class EmotionState {

    public EmotionState() {
        intent = "Hello";
        emoticon = ":-)";
        startDate = null;
        endDate = null;
    }

    public String intent;
    public String emoticon;
    public Date startDate;
    public Date endDate;
}
