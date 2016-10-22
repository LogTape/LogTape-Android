package se.tightloop.logtapesample.spring;

import org.androidannotations.annotations.EBean;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Created by dnils on 22/10/16.
 */

@EBean
public class BufferedRequestFactory extends BufferingClientHttpRequestFactory {
    BufferedRequestFactory() {
        super(new SimpleClientHttpRequestFactory());
    }
}
