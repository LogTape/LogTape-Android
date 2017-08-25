package se.tightloop.logtape.spring;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import se.tightloop.logtapeandroid.LogTape;
import se.tightloop.logtapeandroid.LogTapeUtil;
import se.tightloop.logtapeandroid.events.RequestStartedLogEvent;

public class SpringRestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] data, ClientHttpRequestExecution execution) throws IOException {
        long startTimeNanos = System.nanoTime();
        RequestStartedLogEvent startEvent = LogTape.LogRequestStart(request.getURI().toString(),
                request.getMethod().toString(),
                request.getHeaders().toSingleValueMap(),
                data);

        ClientHttpResponse response = execution.execute(request, data);

        LogTape.LogRequestFinished(startEvent,
                response.getStatusCode().value(),
                response.getStatusText(),
                response.getHeaders().toSingleValueMap(),
                LogTapeUtil.getBytesFromInputStream(response.getBody()),
                "");

        return response;
    }
}
