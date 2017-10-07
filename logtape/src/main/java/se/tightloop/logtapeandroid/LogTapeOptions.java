package se.tightloop.logtapeandroid;

/**
 * Created by dnils on 2017-09-21.
 */


/**
 * This class provides the user with the possibility to customize the
 * LogTape session with settings for things like how the user interface
 * should be triggered.
 */
public class LogTapeOptions {
    /**
     * Defines the gesture that will be used to trigger the report
     * activity.
     */
    public enum Trigger {
        ShakeGesture,
        None
    }

    public Trigger trigger = Trigger.ShakeGesture;

    public LogTapeOptions() {}
}
