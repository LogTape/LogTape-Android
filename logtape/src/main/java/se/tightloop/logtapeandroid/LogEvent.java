package se.tightloop.logtapeandroid;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;

/**
 * Created by dnils on 09/10/16.
 */

class LogEvent {
    LogTapeDate timestamp;
    String id = "";

    public static String getUTCDateString(Date date) {

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    Map<String, String> tags = new HashMap<String, String>();

    LogEvent(Map<String, String> tags) {
        this.tags = tags;
        this.id = UUID.randomUUID().toString();
    }

    JSONObject toJSON() {
        return null;
    }
}
