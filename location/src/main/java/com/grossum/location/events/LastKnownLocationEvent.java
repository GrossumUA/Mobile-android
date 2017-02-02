package com.grossum.location.events;

import android.location.Location;

/**
 * @author Severyn Parkhomenko <pseverin@ukr.net>
 */
public class LastKnownLocationEvent {

    private Location location;

    public LastKnownLocationEvent(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
