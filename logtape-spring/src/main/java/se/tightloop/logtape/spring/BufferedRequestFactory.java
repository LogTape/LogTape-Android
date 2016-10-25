package se.tightloop.logtape.spring;


import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Created by dnils on 22/10/16.
 */

public class BufferedRequestFactory extends BufferingClientHttpRequestFactory {
    public BufferedRequestFactory() {
        super(new SimpleClientHttpRequestFactory());
    }
}
