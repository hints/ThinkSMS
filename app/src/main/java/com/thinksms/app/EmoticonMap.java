package com.thinksms.app;

import java.util.Arrays;

/**
 * Created by dtreiman on 4/26/14.
 */
public class EmoticonMap {

    private static String[] intents = {
            "Surprise",
            "Love",
            "Busy",
            "Like",
            "Hello",
            "Goodbye",
            "Laugh",
            "Meh",
            "Tired",
            "Sleeping",
            "Sick",
            "Anger",
            "Awesome"
    };

    private static String[] emoticons = {
            ":-O",
            "<3",
            ":-#",
            "b",
            ":-)",
            "( ^_^)／",
            ":-D",
            ":-|",
            "-_-",
            "-_- Zzzzz",
            ":-@",
            ">.<",
            "\\o/"
    };

    private static int[] foxImages = {
            R.drawable.fox_abd517cd_93c4_44e6_99ca_9aa9e14afaa8_image_thumb,
            R.drawable.fox_dd5a1f5a_f7d1_4dd3_a51a_ad155ba3ffc2_image_thumb,
            R.drawable.fox_f79649e4_8e6f_4b1a_8f22_ce975f5fef71_image_thumb,
            R.drawable.fox_f79649e4_8e6f_4b1a_8f22_ce975f5fef71_image_thumb,
            R.drawable.fox_cc28a4d4_459c_4d4b_86db_50c3d2c45d4a_image_thumb,
            R.drawable.fox_cc28a4d4_459c_4d4b_86db_50c3d2c45d4a_image_thumb,
            R.drawable.fox_f8667c37_9328_46e7_9a20_a66b09c63fd3_image_thumb,
            R.drawable.fox_4575ed05_06d7_4877_84af_61a6119e1d82_image_thumb,
            R.drawable.fox_e7c362e6_ff36_491c_bbb1_8723b8ab68ea_image_thumb,
            R.drawable.fox_e7c362e6_ff36_491c_bbb1_8723b8ab68ea_image_thumb,
            R.drawable.fox_3591dbc1_22a3_4987_8950_fc68f93c82dc_image_thumb,
            R.drawable.fox_677856ff_1a5d_4f23_94b5_98ebf02ca8e2_image_thumb,
            R.drawable.fox_abd517cd_93c4_44e6_99ca_9aa9e14afaa8_image_thumb
    };

    public static int indexForIntent(String intent) {
        return Arrays.asList(intents).indexOf(intent);
    }


    public static int indexForEmoticon(String emoticon) {
        return Arrays.asList(emoticons).indexOf(emoticon);
    }


    public static String intentForEmoticon(String emoticon) {
        int index = indexForEmoticon(emoticon);
        if (index == -1) {
            return "Hello";
        }
        else {
            return intents[index];
        }
    }


    public static String emoticonForIntent(String intent) {
        int index = indexForIntent(intent);
        if (index == -1) {
            return ":-)";
        }
        else {
            return emoticons[index];
        }
    }


    public static int foxImageForIntent(String intent) {
        int index = indexForIntent(intent);
        if (index == -1) {
            return foxImages[0];
        }
        else {
            return foxImages[index];
        }
    }

    public static int foxImageForEmoticon(String emoticon) {
        int index = indexForEmoticon(emoticon);
        if (index == -1) {
            return foxImages[0];
        }
        else {
            return foxImages[index];
        }
    }

}
