package com.grossum.location;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.grossum.location.events.GoogleApiConnectionChangedEvent;
import com.grossum.location.events.LastKnownLocationEvent;
import com.grossum.location.events.UpdateLocationEvent;
import com.grossum.location.events.UpdatingStartedEvent;

import org.greenrobot.eventbus.EventBus;

import static com.grossum.location.event_constants.ConnectionEventType.CONNECTED;
import static com.grossum.location.event_constants.ConnectionEventType.CONNECTION_FAILED;
import static com.grossum.location.event_constants.ConnectionEventType.SUSPENDED;

/**
 * @author Severyn Parkhomenko <pseverin@ukr.net>
 */
public class LocationApiWrapperService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final static String TAG = "LOCATION";

    private final static String ACTION_ARGS = "action";
    private final static String INTERVAL_ARGS = "interval";
    private final static String FASTEST_INTERVAL_ARGS = "fastest_interval";
    private final static String PRIORITY_ARGS = "priority";

    public final static int DEFAULT_UPDATE_INTERVAL = 1000;
    public final static int DEFAULT_UPDATE_FASTEST_INTERVAL = DEFAULT_UPDATE_INTERVAL;
    private final static int DEFAULT_UPDATE_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    public enum PriorityMapPair {
        HIGH_ACCURACY(100, "High accuracy"),
        BALANCED(102, "Balanced"),
        LOW_BATTERY(104, "Low battery"),
        NO_BATTERY(105, "No battery");

        public final int code;
        public final String label;

        PriorityMapPair(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public static String getLabelByCode(int code) {
            for (PriorityMapPair priorityMapPair : PriorityMapPair.values()) {
                if (priorityMapPair.code == code) {
                    return priorityMapPair.label;
                }
            }
            return "";
        }

        public static String[] getLabelsArray() {
            String[] array = new String[PriorityMapPair.values().length];
            for (int i = 0; i < PriorityMapPair.values().length; i++) {
                array[i] = PriorityMapPair.values()[i].label;
            }
            return array;
        }
    }

    private enum Action {
        CONNECT,
        DISCONNECT,
        CALL_LAST_KNOWN_LOCATION,
        START_LOCATION_UPDATES,
        STOP_LOCATION_UPDATES,
        CHANGE_UPDATE_INTERVAL,
        CHANGE_FASTEST_UPDATE_INTERVAL,
        CHANGE_UPDATE_PRIORITY,
        CHANGE_UPDATE_SETTINGS
    }

    public static void startSelf(Context context) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        context.startService(intent);
    }

    public static void stopSelf(Context context) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        context.stopService(intent);
    }

    public static void connectApi(Context context) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        intent.putExtra(ACTION_ARGS, Action.CONNECT.name());
        context.startService(intent);
    }

    public static void disconnectApi(Context context) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        intent.putExtra(ACTION_ARGS, Action.DISCONNECT.name());
        context.startService(intent);
    }

    public static void callLastLocation(Context context) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        intent.putExtra(ACTION_ARGS, Action.CALL_LAST_KNOWN_LOCATION.name());
        context.startService(intent);
    }

    public static void startLocationUpdates(Context context) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        intent.putExtra(ACTION_ARGS, Action.START_LOCATION_UPDATES.name());
        context.startService(intent);
    }

    public static void stopLocationUpdates(Context context) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        intent.putExtra(ACTION_ARGS, Action.STOP_LOCATION_UPDATES.name());
        context.startService(intent);
    }

    public static void changeUpdateInterval(Context context, int interval) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        intent.putExtra(ACTION_ARGS, Action.CHANGE_UPDATE_INTERVAL.name());
        intent.putExtra(INTERVAL_ARGS, interval);
        context.startService(intent);
    }

    public static void changeFastestUpdateInterval(Context context, int fastestInterval) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        intent.putExtra(ACTION_ARGS, Action.CHANGE_FASTEST_UPDATE_INTERVAL.name());
        intent.putExtra(FASTEST_INTERVAL_ARGS, fastestInterval);
        context.startService(intent);
    }

    public static void changeUpdatePriority(Context context, int priority) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        intent.putExtra(ACTION_ARGS, Action.CHANGE_UPDATE_PRIORITY.name());
        intent.putExtra(PRIORITY_ARGS, priority);
        context.startService(intent);
    }

    public static void changeUpdateSettings(Context context, int interval, int fastestInterval, int priority) {
        Intent intent = new Intent(context, LocationApiWrapperService.class);
        intent.putExtra(ACTION_ARGS, Action.CHANGE_UPDATE_SETTINGS.name());
        intent.putExtra(INTERVAL_ARGS, interval);
        intent.putExtra(FASTEST_INTERVAL_ARGS, fastestInterval);
        intent.putExtra(PRIORITY_ARGS, priority);
        context.startService(intent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        createLocationService(this, this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        if (intent != null && intent.hasExtra(ACTION_ARGS)) {
            switch (Action.valueOf(intent.getStringExtra(ACTION_ARGS))) {
                case CONNECT:
                    connect();
                    break;
                case DISCONNECT:
                    disconnect();
                    break;
                case CALL_LAST_KNOWN_LOCATION:
                    callLastLocation();
                    break;
                case START_LOCATION_UPDATES:
                    startLocationUpdates();
                    break;
                case STOP_LOCATION_UPDATES:
                    stopLocationUpdates();
                    break;
                case CHANGE_UPDATE_INTERVAL: {
                    int interval = intent.getIntExtra(INTERVAL_ARGS, DEFAULT_UPDATE_INTERVAL);
                    setUpdateInterval(interval);
                    break;
                }
                case CHANGE_FASTEST_UPDATE_INTERVAL: {
                    int fastestInterval = intent.getIntExtra(FASTEST_INTERVAL_ARGS, DEFAULT_UPDATE_FASTEST_INTERVAL);
                    setUpdateFastestInterval(fastestInterval);
                    break;
                }
                case CHANGE_UPDATE_PRIORITY: {
                    int priority = intent.getIntExtra(PRIORITY_ARGS, DEFAULT_UPDATE_PRIORITY);
                    setUpdatePriority(priority);
                    break;
                }
                case CHANGE_UPDATE_SETTINGS: {
                    int interval = intent.getIntExtra(INTERVAL_ARGS, DEFAULT_UPDATE_INTERVAL);
                    int fastestInterval = intent.getIntExtra(FASTEST_INTERVAL_ARGS, DEFAULT_UPDATE_FASTEST_INTERVAL);
                    int priority = intent.getIntExtra(PRIORITY_ARGS, DEFAULT_UPDATE_PRIORITY);
                    setUpdateSettings(interval, fastestInterval, priority);
                    break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        EventBus.getDefault().post(new GoogleApiConnectionChangedEvent(CONNECTED));
    }

    @Override
    public void onConnectionSuspended(int i) {
        EventBus.getDefault().post(new GoogleApiConnectionChangedEvent(SUSPENDED));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        EventBus.getDefault().post(new GoogleApiConnectionChangedEvent(CONNECTION_FAILED));
    }

    @Override
    public void onLocationChanged(Location location) {
        EventBus.getDefault().post(new UpdateLocationEvent(location));
    }

    public void createLocationService(Context context) {
        createLocationService(context, null, null);
    }

    public void createLocationService(Context context,
                                      GoogleApiClient.ConnectionCallbacks connectionCallbacks,
                                      GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        if (mGoogleApiClient == null) {
            GoogleApiClient.Builder builder = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API);
            if (connectionCallbacks != null) {
                builder.addConnectionCallbacks(connectionCallbacks);
            }
            if (connectionCallbacks != null) {
                builder.addOnConnectionFailedListener(onConnectionFailedListener);
            }
            mGoogleApiClient = builder.build();
            createLocationRequest();
        }
    }

    public void connect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    public void disconnect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    public void setUpdateInterval(int interval) {
        stopLocationUpdates();
        mLocationRequest.setInterval(interval);
        startLocationUpdates();
    }

    public void setUpdateFastestInterval(int fastestInterval) {
        stopLocationUpdates();
        mLocationRequest.setFastestInterval(fastestInterval);
        startLocationUpdates();
    }

    public void setUpdatePriority(int priority) {
        stopLocationUpdates();
        mLocationRequest.setPriority(priority);
        startLocationUpdates();
    }

    public void setUpdateSettings(int interval, int fastestInterval, int priority) {
        stopLocationUpdates();
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(fastestInterval);
        mLocationRequest.setPriority(priority);
        startLocationUpdates();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(DEFAULT_UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(DEFAULT_UPDATE_FASTEST_INTERVAL);
        mLocationRequest.setPriority(DEFAULT_UPDATE_PRIORITY);
    }

    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        EventBus.getDefault().post(new UpdatingStartedEvent(
                (int) mLocationRequest.getInterval(),
                (int) mLocationRequest.getFastestInterval(),
                PriorityMapPair.getLabelByCode(mLocationRequest.getPriority()))
        );
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    public void callLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        EventBus.getDefault().post(new LastKnownLocationEvent(LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient)));
    }
}
