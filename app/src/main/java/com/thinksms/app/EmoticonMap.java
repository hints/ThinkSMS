package com.thinksms.app;

/**
 * Created by dtreiman on 4/26/14.
 */
public class EmoticonMap {
    public static String emoticonForIntent(String intent) {
        if ("Surprise".equals(intent)) {
            return ":-O";
        }
        else if ("Love".equals(intent)) {
            return "<3";
        }
        else if ("Like".equals(intent)) {
            return "b";
        }
        else if ("Hello".equals(intent)) {
            return ":-)";
        }
        else if ("Goodbye".equals(intent)) {
            return "( ^_^)／";
        }
        else if ("Laugh".equals(intent)) {
            return ":-D";
        }
        else if ("Meh".equals(intent)) {
            return ":-|";
        }
        else if ("Tired".equals(intent)) {
            return "=_=";
        }
        else if ("Sleeping".equals(intent)) {
            return "-_- Zzzzz";
        }
        else {
            return ":-)";
        }
    };
}
