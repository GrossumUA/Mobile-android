package com.grossum.locationapitester;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


public class PermissionsManager {

    public final static int REQUEST_PERMISSIONS = 1000;

    public static boolean requestLocationPermissions(Activity activity) {
        if (checkLocationPermissionGranted(activity)) {
            return true;
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS);
            return false;
        }
    }

    public static boolean onRequestPermissionsResult(Activity activity, int requestCode,
                                                     String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permissions granted!");
                    return true;
                } else {
                    System.out.println("Permissions blocked!");
                    activity.finish();
                    return false;
                }
            }
        }
        return false;
    }

    public static boolean checkLocationPermissionGranted(Activity activity) {
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

}
