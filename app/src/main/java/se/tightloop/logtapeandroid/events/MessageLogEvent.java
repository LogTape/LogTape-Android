package se.tightloop.logtapeandroid.events;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;

import se.tightloop.logtapeandroid.LogTapeUtil;

/**
 * Created by dnils on 09/10/16.
 */

public class MessageLogEvent extends LogEvent {

    private String message;
    private Date timestamp = new Date();

    public MessageLogEvent(String message) {
        this.message = message;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject ret = new JSONObject();

        try {
            ret.put("type", "LOG");
            ret.put("timestamp", LogTapeUtil.getUTCDateString(timestamp));
            ret.put("data", message);
        } catch (JSONException exception) {

        }

        return ret;
    }
}
