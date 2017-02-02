package com.grossum.location;

import android.location.Location;

/**
 * @author Severyn Parkhomenko <pseverin@ukr.net>
 */
public class LocationUtils {

    public static String getFormattedLatLng(Location location){
        if (location == null){
            return " - ";
        } else {
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            String latStr = String.valueOf(roundCoordinate(lat));
            String lngStr = String.valueOf(roundCoordinate(lng));

            return latStr + " | " + lngStr;
        }
    }

    public static String getFormattedAccuracy(Location location){
        if (location == null){
            return "Acc: - ";
        } else {
            return "Acc: " + location.getAccuracy();
        }
    }

    private static double roundCoordinate(double coordinate){
        return ((double) Math.round(coordinate * 100000L))/100000d;
    }
}
