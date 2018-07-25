package com.wr.qt.nmediademo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by wr-app1 on 2017/9/14.
 */
public class TextFormatter {
    public static String getMovieTime(long duration) {
        return new SimpleDateFormat("h:mm:ss", Locale.CHINA).format(new Date(
                duration));
    }
}
