package se.tightloop.logtapesample.spring;

import org.androidannotations.rest.spring.annotations.Get;
import org.androidannotations.rest.spring.annotations.Rest;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import se.tightloop.logtapesample.model.GetData;

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

