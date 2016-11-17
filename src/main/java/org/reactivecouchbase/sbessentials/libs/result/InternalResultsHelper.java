package org.reactivecouchbase.sbessentials.libs.result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
class InternalResultsHelper {

    static WebApplicationContext webApplicationContext;

    @Autowired
    public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        InternalResultsHelper.webApplicationContext = webApplicationContext;
    }
}
