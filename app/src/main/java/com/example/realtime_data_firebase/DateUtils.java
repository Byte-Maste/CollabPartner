package com.example.realtime_data_firebase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static boolean isFuture(String dateTimeString) {
        try {
            // Expected input format: "dd/MM/yyyy HH:mm"
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(dateTimeString);
            return date != null && date.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
