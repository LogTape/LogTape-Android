package se.tightloop.logtapeandroid;

/**
 * Created by dnils on 2017-09-21.
 */

public class LogTapeOptions {
    public enum Trigger {
        ShakeGesture,
        None
    }

    public Trigger trigger = Trigger.ShakeGesture;
}
