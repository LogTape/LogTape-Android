package se.tightloop.logtapesample;

import org.androidannotations.rest.spring.annotations.Get;
import org.androidannotations.rest.spring.annotations.Rest;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import se.tightloop.logtapesample.model.GetData;
import se.tightloop.logtapesample.spring.BufferedRequestFactory;
import se.tightloop.logtapesample.spring.SpringRestInterceptor;

/**
 * Created by dnils on 16/10/16.
 */

@Rest(rootUrl = "http://www.httpbin.org",
        converters = { GsonHttpMessageConverter.class },
        interceptors = { SpringRestInterceptor.class },
        requestFactory = BufferedRequestFactory.class )
public interface RestClient {
    @Get("/get")
    GetData get();
}

