package se.tightloop.logtapeandroid;

import android.app.Application;

import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *   Public API class for LogTape. Provides logging and initialization functionality.
 */
public class LogTape {
    /**
     *  Initializes LogTape with a resource identifier pointing to a string containing
     *  your application ID. If an invalid resource identifier or if the corresponding
     *  string is empty, LogTape will not be initialized and all log methods
     *  will not perform any actual logging.
     *
     *  @param apiKeyId Resource identifier to a string resource containing your LogTape
     *                  application ID
     *  @param application The current application instance
     *  @param options Object containing customized settings (or null if using defaults).
     *                 See {@link LogTapeOptions} for more information.
     */
    public static void init(int apiKeyId, Application application, LogTapeOptions options) {
        init(application.getResources().getString(apiKeyId), application, options);
    }

    /**
     *  Initializes LogTape with an application API key. It is safe to pass null or an
     *  empty string, but in this case LogTape will not be initialized and all log methods
     *  will not perform any actual logging.
     *
     *  @param apiKey Your LogTape application ID.
     *  @param application The current application instance
     *  @param options Object containing customized settings (or null if using defaults).
     *                 See {@link LogTapeOptions} for more information.
     */
    public static void init(String apiKey, Application application, LogTapeOptions options) {
        if (apiKey == null || apiKey == "") {
            return;
        } else if (LogTapeImpl.instance != null) {
            LogTapeImpl.instance.apiKey = apiKey;
        } else {
            LogTapeImpl.instance = new LogTapeImpl(application, apiKey, options);
        }
    }

    /**
     *   If LogTape is enabled, this method will clear the current log items.
     */
    public static void clearLog() {
        if (hasValidInstance()) {
            LogTapeImpl.instance.clearLogFiles();
        }
    }

    /**
     *   If LogTape is enabled, this method will present an activity that allows the user
     *   to upload the log to the LogTape server. Will not do anything if the activity is
     *   already showing.
     */
    public static void showReportActivity() {
        if (hasValidInstance()) {
            LogTapeImpl.instance.showReportActivity();
        }
    }

    /**
     *   This method allows you to temporarily disable LogTape logging on an already
     *   initialized session.
     *
     *   @param enabled Whether or not to enable LogTape logging.
     */
    public static void setEnabled(boolean enabled) {
        LogTapeImpl.enabled = enabled;
    }

    /**
     * This method tells you if LogTape is initialized and enabled. Can be useful to
     * use in order to avoid doing costly log preparation calls in release builds.
     *
     * @return Whether or not LogTape is initialized and enabled.
     */
    public static boolean isEnabled() {
        if (LogTapeImpl.instance != null) {
            return LogTapeImpl.enabled;
        } else {
            return false;
        }
    }

    /**
     * This method logs a JSON object.
     *
     * @param tag A string that identifies the source of the call (optional)
     * @param message An optional message that identifies the object.
     * @param object The JSON object to be serialized.
     */
    public static void logObject(String tag, String message, JSONObject object) {
        if (hasValidInstance()) {
            Map<String, String> tags = null;

            if (tag != null) {
                tags = new HashMap<String, String>();
                tags.put(tag, "info");
            }

            logObject(message, object, tags);
        }
    }

    /**
     * This method logs a JSON object.
     *
     * @param message An optional message that identifies the object.
     * @param object The JSON object to be serialized.
     * @param tags A collection of tags associated with the log call.
     */
    public static void logObject(String message, JSONObject object, Map<String, String> tags) {
        if (hasValidInstance()) {
            LogTapeImpl.instance.logObject(message, object, tags);;
        }
    }

    /**
     * This method logs a simple message.
     *
     * @param msg The message to log.
     */
    public static void log(String msg) {
        Map<String, String> nullRef = null;
        LogTape.log(msg, nullRef);
    }

    /**
     * This method logs a message with a tag.
     *
     * @param tag A string that identifies the source of the call (optional)
     * @param message The message to log.
     */
    public static void log(String tag, String message) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(tag, "info");
        LogTape.log(message, tags);
    }

    /**
     * This method logs a message with a tag.
     *
     * @param message The message to log.
     * @param tags A collection of tags associated with the log call.
     */
    public static void log(String message, Map<String, String> tags) {
        if (hasValidInstance()) {
            LogTapeImpl.instance.log(message, tags);
        }
    }

    /**
     * This is a convenience method to log a standard HTTPUrlConnection request.
     *
     * @param startDate The start date of the request. Create an instance of this object
     *                  at the start of the request. See {@link LogTapeDate} for more information.
     * @param requestHeaders This needs to be passed as a separate argument since it can't be
     *                       read from the connection after it has been started, unlike the response
     *                       headers.
     * @param body The request body.
     * @param responseBody The response body.
     * @param tags A collection of tags associated with the log call.
     */
    public static void logHttpURLConnectionRequest(LogTapeDate startDate,
                                                   Map<String, List<String>> requestHeaders,
                                                   HttpURLConnection connection,
                                                   byte[] body,
                                                   byte[] responseBody,
                                                   Map<String, String> tags)
    {
        startDate = validateDate(startDate);

        if (hasValidInstance()) {
            try {
                logRequest(startDate,
                        connection.getURL().toString(),
                        connection.getRequestMethod(),
                        LogTapeUtil.multiValueMapToSingleValueMap(requestHeaders),
                        body,
                        connection.getResponseCode(),
                        connection.getResponseMessage(),
                        LogTapeUtil.multiValueMapToSingleValueMap(connection.getHeaderFields()),
                        responseBody, "", tags, true);
            } catch (IOException e) {

            }

        }
    }

    /**
     * This is a method to log request in a single call. Depending on your implementation it
     * will probably make more sense to do it in two separate calls, see
     * {@link LogTape#logRequestStart(String, String, Map, byte[], Map)}.
     *
     * @param startDate The start date of the request. Create an instance of this object
     *                  at the start of the request. See {@link LogTapeDate} for more information.
     * @param url The request URL
     * @param method The request HTTP method
     * @param requestHeaders The request headers of the call.
     * @param body The request body.
     * @param httpStatusCode The HTTP response status code.
     * @param httpStatusText The HTTP status text (leave empty if your library doesn't provide it)
     * @param responseHeaders The response headers of the call
     * @param responseBody The response body.
     * @param tags A collection of tags associated with the log call.
     * @param logStartEvent If the request and response should be logged as separate entries.
     *                      Recommended to do since you can hide the requests easily in the web
     *                      UI. However, if you implement custom caching or similar mechanisms
     *                      it might make sense to only show the response if it happens immediately.
     */
    public static void logRequest(LogTapeDate startDate,
                                  String url,
                                  String method,
                                  Map<String, String> requestHeaders,
                                  byte[] body,
                                  int httpStatusCode,
                                  String httpStatusText,
                                  Map<String, String> responseHeaders,
                                  byte[] responseBody,
                                  String errorText,
                                  Map<String, String> tags,
                                  boolean logStartEvent)
    {
        startDate = validateDate(startDate);

        if (hasValidInstance()) {
            LogTapeImpl.instance.logRequest(
                    startDate,
                    url,
                    method,
                    requestHeaders,
                    body,
                    httpStatusCode,
                    httpStatusText,
                    responseHeaders,
                    responseBody,
                    errorText,
                    tags,
                    logStartEvent);
        }
    }

    /**
     * This is a method to log the start of a request.
     *
     * @param url The request URL
     * @param method The request HTTP method
     * @param requestHeaders The request headers of the call.
     * @param body The request body.
     * @param tags A collection of tags associated with the log call.
     */
    public static Object logRequestStart(String url,
                                         String method,
                                         Map<String, String> requestHeaders,
                                         byte[] body,
                                         Map<String, String> tags)
    {
        if (hasValidInstance()) {
            return LogTapeImpl.instance.logRequestStart(url, method, requestHeaders, body, tags);
        } else {
            return null;
        }
    }

    /**
     * This is a method to log the completion of a network request. Should always be preceded by
     * a call to {@link LogTape#logRequestStart(String, String, Map, byte[], Map)}.
     *
     * @param startEvent The event that started the request. Use return value from
     *                   {@link LogTape#logRequestStart(String, String, Map, byte[], Map)}.
     * @param httpStatusCode The HTTP response status code.
     * @param httpStatusText The HTTP status text (leave empty if your library doesn't provide it)
     * @param responseHeaders The response headers of the call
     * @param responseBody The response body.
     * @param tags A collection of tags associated with the log call.
     */
    public static void logRequestFinished(Object startEvent,
                                          int httpStatusCode,
                                          String httpStatusText,
                                          Map<String, String> responseHeaders,
                                          byte[] responseBody,
                                          String errorText,
                                          Map<String, String> tags)
    {
        if (hasValidInstance()) {
            LogTapeImpl.instance.logRequestFinished(startEvent, httpStatusCode, httpStatusText, responseHeaders, responseBody, errorText, tags);
        }
    }

    /**
     * This method configures a property supplier. Use property suppliers to add additional
     * metadata to your log reports, such as git commit SHA, user session properties and so on.
     *
     * @param supplier The supplier that will provide LogTape with the custom properties.
     *                 See {@link LogTapePropertySupplier} for more information.

     */
    public static void setPropertySupplier(LogTapePropertySupplier supplier) {
        if (hasValidInstance()) {
            LogTapeImpl.instance.setPropertySupplier(supplier);
        }
    }

    /**
     * @return If our session is enabled and instantiated correctly
     */
    private static boolean hasValidInstance() {
        return LogTapeImpl.instance != null && LogTapeImpl.enabled;
    }

    private static LogTapeDate validateDate(LogTapeDate date) {
        if (date == null || date.date == null) {
            return new LogTapeDate();
        } else {
            return date;
        }
    }
}
