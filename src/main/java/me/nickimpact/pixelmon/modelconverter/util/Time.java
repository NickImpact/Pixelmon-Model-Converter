package me.nickimpact.pixelmon.modelconverter.util;

import java.util.concurrent.TimeUnit;

public class Time {

    private long millis;

    public Time(long millis) {
        this.millis = millis;
    }

    public long getTime() {
        return this.millis;
    }

    public String toString() {
        return String.format(
                "%02d:%02d:%02d.%03d",
                TimeUnit.MILLISECONDS.toHours(this.millis),
                TimeUnit.MILLISECONDS.toMinutes(this.millis) % 60L,
                TimeUnit.MILLISECONDS.toSeconds(this.millis) % 60L,
                this.millis % 1000
        );
    }

}
