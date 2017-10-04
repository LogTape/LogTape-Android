package se.tightloop.logtapeandroid;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by dnils on 09/10/16.
 */

class MessageLogEvent extends LogEvent {

    private final String message;

    public MessageLogEvent(String message, Map<String, String> tags) {
        super(tags);
        this.timestamp = new LogTapeDate();
        this.message = message;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject ret = new JSONObject();

        try {
            ret.put("id", this.id);
            if (tags != null) {
                ret.put("tags", new JSONObject(tags));
            }
            ret.put("type", "LOG");
            ret.put("timestamp", LogTapeUtil.getUTCDateString(timestamp.date));
            ret.put("data", message);
        } catch (JSONException exception) {

        }

        return ret;
    }
}
