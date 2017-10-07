package se.tightloop.logtapeandroid;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by dnils on 16/10/16.
 */

class RequestLogEvent extends LogEvent {
    private final int httpStatusCode;
    private final String httpStatusText;
    private final Map<String, String> responseHeaders;
    private final byte[] responseBody;
    private final String errorText;
    private final long elapsedTimeMs;
    private final RequestStartedLogEvent reqStartedEvent;

    RequestLogEvent(LogTapeDate timestamp,
                           RequestStartedLogEvent reqStartedEvent,
                           int httpStatusCode,
                           String httpStatusText,
                           Map<String, String> responseHeaders,
                           byte[] responseBody,
                           String errorText,
                           Map<String, String> tags)
    {
        super(tags);
        this.elapsedTimeMs = timestamp.date.getTime() - reqStartedEvent.timestamp.date.getTime();
        this.timestamp = timestamp;
        this.reqStartedEvent = reqStartedEvent;
        this.httpStatusCode = httpStatusCode;
        this.httpStatusText = httpStatusText;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.errorText = errorText;
    }

    @Override
    JSONObject toJSON() {
        JSONObject ret = this.reqStartedEvent.toJSON();
        JSONObject response = new JSONObject();
        JSONObject data = null;

        try {
            ret.put("startId", this.reqStartedEvent.id);
            ret.put("id", this.id);

            if (tags != null) {
                ret.put("tags", new JSONObject(tags));
            }

            data = ret.getJSONObject("data");

            JSONObject responseHeadersObj = new JSONObject();

            if (responseHeaders != null) {
                for(Map.Entry<String, String> entry : this.responseHeaders.entrySet()) {
                    responseHeadersObj.put(entry.getKey(), entry.getValue());
                }
            }

            ret.remove("type");
            ret.put("type", "REQUEST");
            ret.put("timestamp", LogEvent.getUTCDateString(timestamp.date));

            response.put("statusCode", httpStatusCode);
            response.put("statusText", httpStatusText);
            response.put("headers", responseHeadersObj);
            response.put("time", this.elapsedTimeMs);

            if (responseBody != null) {
                try {
                    String utfResponseBody = new String(responseBody, "UTF-8");
                    response.put("data", utfResponseBody);
                } catch (Exception e) {

                }
            }

            data.put("response", response);
            ret.put("data", data);
        } catch (JSONException exception) {
            Log.e("LogTape", "Caught exception when generating req JSON: " + exception.toString());
        }

        return ret;
    }
}
