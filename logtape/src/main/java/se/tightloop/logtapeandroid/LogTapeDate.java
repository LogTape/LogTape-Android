package se.tightloop.logtapeandroid;

import java.util.Date;

/**
 *   Wrapper class for a date. We encapsulate the date implementation
 *   in order to preserve ordering of log events that occur on the same
 *   timestamp (due to date resolution limitations).
 *
 *   This also allows us to change the underlying date implementation
 *   in the future to one with a better resolution.
 */
public class LogTapeDate {
    private static Object CounterMutex = new Object();
    private static int count = 0;

    Date date;
    int index;

    /**
     * Create a new date based on a java.util.Date instance
     * @param date The date to be used as timestamp
     */
    public LogTapeDate(Date date) {
        this.date = date;
        synchronized (LogTapeDate.CounterMutex) {
            this.index = count++;
        }
    }

    /**
     * Creates a new LogTapeDate with the current time as timestamp.
     */
    public LogTapeDate() {
        this.date = new Date();
        synchronized (LogTapeDate.CounterMutex) {
            this.index = count++;
        }
    }
}
