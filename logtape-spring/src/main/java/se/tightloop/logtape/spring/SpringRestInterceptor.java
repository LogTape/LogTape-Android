package se.tightloop.logtape.spring;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import se.tightloop.logtapeandroid.LogTape;
import se.tightloop.logtapeandroid.LogTapeUtil;

public class SpringRestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] data, ClientHttpRequestExecution execution) throws IOException {
        long startTimeNanos = System.nanoTime();
        Object startEvent = LogTape.logRequestStart(request.getURI().toString(),
                request.getMethod().toString(),
                request.getHeaders().toSingleValueMap(),
                data, null);

        ClientHttpResponse response = execution.execute(request, data);

        LogTape.logRequestFinished(startEvent,
                response.getStatusCode().value(),
                response.getStatusText(),
                response.getHeaders().toSingleValueMap(),
                LogTapeUtil.getBytesFromInputStream(response.getBody()),
                "", null);

        return response;
    }
}
