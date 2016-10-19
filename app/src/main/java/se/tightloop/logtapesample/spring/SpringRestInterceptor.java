package se.tightloop.logtapesample.spring;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import se.tightloop.logtapeandroid.LogTape;

public class SpringRestInterceptor implements ClientHttpRequestInterceptor {

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];

        for (int len; (len = is.read(buffer)) != -1;)
            os.write(buffer, 0, len);

        os.flush();

        is.reset();
        return os.toByteArray();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] data, ClientHttpRequestExecution execution) throws IOException {
        long startTimeNanos = System.nanoTime();
        Date timestamp = new Date();
        ClientHttpResponse response = execution.execute(request, data);

        long elapsedTimeMillis = (System.nanoTime() - startTimeNanos) / 1000000;

        InputStream body = response.getBody();
        byte[] responseBody = getBytesFromInputStream(response.getBody());

        LogTape.LogRequest(timestamp,
                request.getURI().toString(),
                request.getMethod().toString(),
                request.getHeaders().toSingleValueMap(),
                data,
                response.getStatusCode().value(),
                response.getStatusText(),
                response.getHeaders().toSingleValueMap(),
                responseBody,
                "",
                (int) elapsedTimeMillis);

        return response;
    }
}
