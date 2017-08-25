package se.tightloop.logtape.okhttp;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import se.tightloop.logtapeandroid.LogTape;
import se.tightloop.logtapeandroid.events.RequestStartedLogEvent;

public class LogTapeLoggingInterceptor implements Interceptor {
    // We only support headers with unique keys for now. If
    // this turns out to be problematic we will add support
    // for multiple keys later on.
    Map<String, String> multiValueMapToSingleValueMap(Map<String, List<String>> multiMap) {
        Map<String, String> ret = new HashMap<String, String>();

        for (Map.Entry<String, List<String>> entry : multiMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                ret.put(entry.getKey(), entry.getValue().get(0));
            }
        }

        return ret;
    }

    private static byte[] requestBodyToBuffer(final Request request){
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            if (copy.body() == null) {
                return null;
            }
            copy.body().writeTo(buffer);
            return buffer.readByteArray();
        } catch (final IOException e) {
            return null;
        }
    }

    private ResponseBody copyBody(ResponseBody body, long limit) throws IOException {
        BufferedSource source = body.source();
        if (source.request(limit)) throw new IOException("body too long!");
        Buffer bufferedCopy = source.buffer().clone();
        return ResponseBody.create(body.contentType(), body.contentLength(), bufferedCopy);
    }

    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
        Date startDate = new Date();
        Request request = chain.request();

        RequestStartedLogEvent startEvent = LogTape.LogRequestStart(request.url().toString(),
                request.method(),
                multiValueMapToSingleValueMap(request.headers().toMultimap()),
                requestBodyToBuffer(request));

        Response response = chain.proceed(request);

        ResponseBody copiedBody = copyBody(response.body(), 999999999);

        LogTape.LogRequestFinished(startEvent,
                response.networkResponse().code(),
                response.networkResponse().message(),
                multiValueMapToSingleValueMap(response.headers().toMultimap()),
                copiedBody.bytes(),
                "");

        return response;
    }
}