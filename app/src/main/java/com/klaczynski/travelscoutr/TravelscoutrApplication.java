package com.klaczynski.travelscoutr;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class TravelscoutrApplication extends Application {
    @Override
    public void onCreate() {
        DynamicColors.applyToActivitiesIfAvailable(this); //Solely for pretty android 12 Material You color implementation
        super.onCreate();
    }
}
