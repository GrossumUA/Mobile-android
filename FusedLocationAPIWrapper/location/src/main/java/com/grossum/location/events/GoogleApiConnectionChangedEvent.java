package com.grossum.location.events;

import com.grossum.location.event_constants.ConnectionEventType;

/**
 * @author Severyn Parkhomenko <pseverin@ukr.net>
 */
public class GoogleApiConnectionChangedEvent {

    private ConnectionEventType type;

    public GoogleApiConnectionChangedEvent(ConnectionEventType type) {
        this.type = type;
    }

    public ConnectionEventType getType() {
        return type;
    }
}
