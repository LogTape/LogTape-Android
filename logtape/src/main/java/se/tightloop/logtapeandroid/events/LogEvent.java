package se.tightloop.logtapeandroid.events;

import org.json.JSONObject;
import java.util.Date;

/**
 * Created by dnils on 09/10/16.
 */

public class LogEvent {
    protected Date timestamp;
    public int id = 0;
    protected static int IdCounter = 0;

    public LogEvent() {
        id = LogEvent.IdCounter;
        LogEvent.IdCounter += 1;
    }

    public JSONObject toJSON() {
        return null;
    }
}
