package com.grossum.location.events;

import android.location.Location;

/**
 * @author Severyn Parkhomenko <pseverin@ukr.net>
 */
public class UpdateLocationEvent {

    private Location location;

    public UpdateLocationEvent(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
