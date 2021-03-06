package se.tightloop.logtapeandroid;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by dnils on 2017-09-19.
 */

class ObjectLogEvent extends LogEvent {

    private String message;
    private JSONObject object;

    ObjectLogEvent(String message, JSONObject object, Map<String, String> tags) {
        super(tags);
        this.timestamp = new LogTapeDate();
        this.message = message;
        this.object = object;
        this.tags = tags;
    }

    @Override
    JSONObject toJSON() {

        JSONObject ret = new JSONObject();

        try {
            ret.put("id", this.id);
            ret.put("type", "JSON");
            ret.put("timestamp", LogEvent.getUTCDateString(timestamp.date));
            ret.put("message", message);

            if (tags != null) {
                ret.put("tags", new JSONObject(tags));
            }

            ret.put("data", object);
        } catch (JSONException exception) {

        }

        return ret;
    }
}
