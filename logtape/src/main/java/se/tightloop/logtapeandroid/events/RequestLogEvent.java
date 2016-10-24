package se.tightloop.logtapeandroid.events;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Map;

import se.tightloop.logtapeandroid.LogTapeUtil;

/**
 * Created by dnils on 16/10/16.
 */

public class RequestLogEvent extends LogEvent {
    String url;
    String method;
    Map<String, String> requestHeaders;
    byte[] body;
    int httpStatusCode;
    String httpStatusText;
    Map<String, String> responseHeaders;
    byte[] responseBody;
    String errorText;
    int elapsedTimeMs;
    Date timestamp;

    public RequestLogEvent(Date timestamp,
                           String url,
                           String method,
                           Map<String, String> requestHeaders,
                           byte[] body,
                           int httpStatusCode,
                           String httpStatusText,
                           Map<String, String> responseHeaders,
                           byte[] responseBody,
                           String errorText,
                           int elapsedTimeMs)
    {
        this.timestamp = timestamp;
        this.url = url;
        this.method = method;
        this.requestHeaders = requestHeaders;
        this.body = body;
        this.httpStatusCode = httpStatusCode;
        this.httpStatusText = httpStatusText;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.errorText = errorText;
        this.elapsedTimeMs = elapsedTimeMs;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject ret = new JSONObject();
        JSONObject response = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject data = new JSONObject();

        try {
            JSONObject responseHeadersObj = new JSONObject();
            JSONObject requestHeadersObj = new JSONObject();

            for(Map.Entry<String, String> entry : this.requestHeaders.entrySet()) {
                requestHeadersObj.put(entry.getKey(), entry.getValue());
            }

            for(Map.Entry<String, String> entry : this.responseHeaders.entrySet()) {
                responseHeadersObj.put(entry.getKey(), entry.getValue());
            }

            ret.put("type", "REQUEST");
            ret.put("timestamp", LogTapeUtil.getUTCDateString(timestamp));

            request.put("method", method);
            request.put("url", url);
            request.put("headers", requestHeadersObj);

            response.put("statusCode", httpStatusCode);
            response.put("statusText", httpStatusText);
            response.put("headers", responseHeadersObj);

            try {
                String utfResponseBody = new String(responseBody, "UTF-8");
                response.put("data", utfResponseBody);
            } catch (Exception e) {

            }

            try {
                String requestBodyUtf = new String(body, "UTF-8");
                request.put("data", requestBodyUtf);
            } catch (Exception e) {

            }

            data.put("response", response);
            data.put("request", request);
            ret.put("data", data);
        } catch (JSONException exception) {

        }

        return ret;
    }
}
