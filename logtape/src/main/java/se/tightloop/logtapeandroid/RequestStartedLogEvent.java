package se.tightloop.logtapeandroid;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Map;

/**
 * Created by dnils on 2017-08-24.
 */

class RequestStartedLogEvent extends LogEvent {
    private final String url;
    private final String method;
    private final Map<String, String> requestHeaders;
    private final byte[] body;

    public RequestStartedLogEvent(Date timestamp,
                                  String url,
                                  String method,
                                  Map<String, String> requestHeaders,
                                  byte[] body,
                                  Map<String, String> tags)
    {
        super(tags);
        this.timestamp = new Date();
        this.url = url;
        this.method = method;
        this.requestHeaders = requestHeaders;
        this.body = body;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject ret = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject data = new JSONObject();

        try {
            ret.put("id", this.id);

            if (tags != null) {
                ret.put("tags", new JSONObject(tags));
            }

            JSONObject requestHeadersObj = new JSONObject();

            for(Map.Entry<String, String> entry : this.requestHeaders.entrySet()) {
                requestHeadersObj.put(entry.getKey(), entry.getValue());
            }

            ret.put("type", "REQUEST_START");
            ret.put("timestamp", LogTapeUtil.getUTCDateString(timestamp));

            request.put("method", method);
            request.put("url", url);
            request.put("headers", requestHeadersObj);


            try {
                String requestBodyUtf = new String(body, "UTF-8");
                request.put("data", requestBodyUtf);
            } catch (Exception e) {

            }

            data.put("request", request);
            ret.put("data", data);
        } catch (JSONException exception) {

        }

        return ret;
    }
}
