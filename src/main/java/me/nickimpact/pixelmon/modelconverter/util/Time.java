package me.nickimpact.pixelmon.modelconverter.util;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Time {

    private long time;
    private static final int secondsPerMinute = 60;
    private static final int secondsPerHour = 3600;
    private static final int secondsPerDay = 86400;
    private static final int secondsPerWeek = 604800;

    public Time(long seconds) {
        this.time = seconds;
    }

    public Time(String formattedTime) throws IllegalArgumentException {
        Pattern minorTimeString = Pattern.compile("^\\d+$");
        Pattern timeString = Pattern.compile("^((\\d+)w)?((\\d+)d)?((\\d+)h)?((\\d+)m)?((\\d+)s)?$");
        if (minorTimeString.matcher(formattedTime).matches()) {
            this.time += Long.parseUnsignedLong(formattedTime);
        } else {
            Matcher m = timeString.matcher(formattedTime);
            if (m.matches()) {
                this.time = this.amount(m.group(2), 604800);
                this.time += this.amount(m.group(4), 86400);
                this.time += this.amount(m.group(6), 3600);
                this.time += this.amount(m.group(8), 60);
                this.time += this.amount(m.group(10), 1);
            }

        }
    }

    private long amount(String g, int multiplier) {
        return g != null && g.length() > 0 ? (long)multiplier * Long.parseUnsignedLong(g) : 0L;
    }

    public long getTime() {
        return this.time;
    }

    public String asShort() {
        if (this.time < 60L) {
            return this.time + "s";
        } else {
            return this.time < 3600L ? TimeUnit.SECONDS.toMinutes(this.time) + "m" : TimeUnit.SECONDS.toHours(this.time) + "h";
        }
    }

    public String toString() {
        return this.time <= 0L ? "Expired" : String.format("%02d:%02d:%02d", TimeUnit.SECONDS.toHours(this.time), TimeUnit.SECONDS.toMinutes(this.time) % 60L, this.time % 60L);
    }

}
