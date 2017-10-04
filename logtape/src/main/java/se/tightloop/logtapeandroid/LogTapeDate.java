package se.tightloop.logtapeandroid;

import java.util.Date;

/**
 * Created by dnils on 2017-09-30.
 */

public class LogTapeDate {
    private static Object CounterMutex = new Object();
    private static int count = 0;

    public Date date;
    public int index;

    public LogTapeDate(Date date) {
        this.date = date;
        synchronized (LogTapeDate.CounterMutex) {
            this.index = count++;
        }
    }
    
    public LogTapeDate() {
        this.date = new Date();
        synchronized (LogTapeDate.CounterMutex) {
            this.index = count++;
        }
    }
}
