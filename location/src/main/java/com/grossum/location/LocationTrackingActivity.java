package com.grossum.location;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.grossum.location.events.GoogleApiConnectionChangedEvent;
import com.grossum.location.events.LastKnownLocationEvent;
import com.grossum.location.events.UpdateLocationEvent;
import com.grossum.location.events.UpdatingStartedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Severyn Parkhomenko <pseverin@ukr.net>
 */
public abstract class LocationTrackingActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocationApiWrapperService.startSelf(this);
        LocationApiWrapperService.connectApi(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLastKnownLocationEvent(LastKnownLocationEvent lastKnownLocationEvent) {
        onLastKnownLocation(lastKnownLocationEvent.getLocation());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdateLocationEvent(UpdateLocationEvent updateLocationEvent) {
        onUpdateLocationEvent(updateLocationEvent.getLocation());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectionChangedEvent(GoogleApiConnectionChangedEvent event) {
        switch (event.getType()) {
            case CONNECTED:
                Toast.makeText(this, "Google Api Client connected", Toast.LENGTH_LONG).show();
                break;
            case SUSPENDED:
                Toast.makeText(this, "Google Api Client: connection suspend!", Toast.LENGTH_LONG).show();
                break;
            case CONNECTION_FAILED:
                Toast.makeText(this, "Google Api Client: connection failed!", Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdatingStartedEvent(UpdatingStartedEvent updateLocationEvent) {
        onUpdatingStarted(updateLocationEvent.interval, updateLocationEvent.fastestInterval, updateLocationEvent.priority);
    }

    protected abstract void onLastKnownLocation(Location location);

    protected abstract void onUpdateLocationEvent(Location location);

    protected abstract void onUpdatingStarted(int interval, int fastestInterval, String priority);
}
