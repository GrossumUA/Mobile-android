package com.grossum.locationapitester;

import android.content.Context;
import android.content.SharedPreferences;


public class SharedPreferencesManager {
    private final static float NULL_COORDINATE = -1000;


    private final static String REAL_LOCATION_LAT = "real_location_lat";
    private final static String REAL_LOCATION_LNG = "real_location_lng";

    private SharedPreferences sharedPreferences;

    public SharedPreferencesManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
    }

    public void setRealLocationLat(float realLocationLat) {
        sharedPreferences.edit().putFloat(REAL_LOCATION_LAT, realLocationLat).apply();
    }

    public Float getRealLocationLat() {
        float lat = sharedPreferences.getFloat(REAL_LOCATION_LAT, NULL_COORDINATE);
        if (lat != NULL_COORDINATE){
            return lat;
        } else {
            return null;
        }
    }

    public void setRealLocationLng(float realLocationLng) {
        sharedPreferences.edit().putFloat(REAL_LOCATION_LNG, realLocationLng).apply();
    }

    public Float getRealLocationLng() {
        float lng = sharedPreferences.getFloat(REAL_LOCATION_LNG, NULL_COORDINATE);
        if (lng != NULL_COORDINATE){
            return lng;
        } else {
            return null;
        }
    }
}
