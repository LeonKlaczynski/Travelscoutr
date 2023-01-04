package com.klaczynski.better_locationscout;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public static void log(String TAG, String msg) {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            System.out.println("["+formatter.format(date)+"] "+TAG+": "+msg);
    }
    public static void log(String TAG, int msg) {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            System.out.println("["+formatter.format(date)+"] "+TAG+": "+msg);
    }

    public static void debug(String TAG, String msg) {
        if(!Constants.DEBUG) return;
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            System.out.println("["+formatter.format(date)+"] "+TAG+": "+msg);
    }
}
