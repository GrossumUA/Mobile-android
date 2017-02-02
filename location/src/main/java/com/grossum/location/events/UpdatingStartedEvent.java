package com.grossum.location.events;

/**
 * @author Severyn Parkhomenko <pseverin@ukr.net>
 */
public class UpdatingStartedEvent {

    public final int interval;
    public final int fastestInterval;
    public final String priority;

    public UpdatingStartedEvent(int interval, int fastestInterval, String priority) {
        this.interval = interval;
        this.fastestInterval = fastestInterval;
        this.priority = priority;
    }
}
