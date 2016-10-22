package se.tightloop.logtapesample.spring;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Date;

import se.tightloop.logtapeandroid.LogTape;
import se.tightloop.logtapeandroid.LogTapeUtil;

public class SpringRestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] data, ClientHttpRequestExecution execution) throws IOException {
        long startTimeNanos = System.nanoTime();
        ClientHttpResponse response = execution.execute(request, data);

        LogTape.LogRequest(new Date(),
                request.getURI().toString(),
                request.getMethod().toString(),
                request.getHeaders().toSingleValueMap(),
                data,
                response.getStatusCode().value(),
                response.getStatusText(),
                response.getHeaders().toSingleValueMap(),
                LogTapeUtil.getBytesFromInputStream(response.getBody()),
                "",
                (int) (System.nanoTime() - startTimeNanos) / 1000000);

        return response;
    }
}
