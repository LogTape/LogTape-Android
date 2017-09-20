package se.tightloop.logtape.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HurlStack;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import se.tightloop.logtapeandroid.LogTape;

public class LogTapeVolleyStack extends HurlStack {

    @Override
    public org.apache.http.HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        String method = "";

        switch (request.getMethod()) {
            case Request.Method.GET:
                method = "GET";
                break;
            case Request.Method.POST:
                method = "POST";
                break;
            case Request.Method.PUT:
                method = "PUT";
                break;
            case Request.Method.PATCH:
                method = "PATCH";
                break;
            case Request.Method.DELETE:
                method = "DELETE";
                break;
            case Request.Method.OPTIONS:
                method = "OPTIONS";
                break;
            case Request.Method.TRACE:
                method = "TRACE";
                break;
            case Request.Method.HEAD:
                method = "HEAD";
                break;
        }

        Object startEvent = LogTape.LogRequestStart(request.getUrl(), method, request.getHeaders(), request.getBody(), null);

        HttpResponse ret = super.performRequest(request, additionalHeaders);

        if (ret instanceof BasicHttpResponse) {
            BasicHttpResponse basicResponse = (BasicHttpResponse)ret;
            byte[] responseByteArray = null;

            if (basicResponse.getEntity() instanceof BasicHttpEntity) {
                BasicHttpEntity entity = (BasicHttpEntity)basicResponse.getEntity();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                entity.writeTo(bo);
                responseByteArray = bo.toByteArray();
                entity.setContent(new ByteArrayInputStream(responseByteArray));
            }

            Map<String, String> responseHeaders = new HashMap<String, String>();

            for (Header header : basicResponse.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }

            LogTape.LogRequestFinished(startEvent,
                    basicResponse.getStatusLine().getStatusCode(),
                    basicResponse.getStatusLine().getReasonPhrase(),
                    responseHeaders,
                    responseByteArray,
                    "", null);
        }

        return ret;
    }
}