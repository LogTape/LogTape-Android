package se.tightloop.logtapeandroid;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by dnils on 09/10/16.
 */

class LogEvent {
    public LogTapeDate timestamp;
    public String id = "";


    public Map<String, String> tags = new HashMap<String, String>();

    public LogEvent(Map<String, String> tags) {
        this.tags = tags;
        this.id = UUID.randomUUID().toString();
    }

    public JSONObject toJSON() {
        return null;
    }
}
