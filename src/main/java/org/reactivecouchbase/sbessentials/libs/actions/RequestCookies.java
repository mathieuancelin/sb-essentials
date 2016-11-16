package org.reactivecouchbase.sbessentials.libs.actions;

import javaslang.collection.HashMap;
import javaslang.collection.Map;
import javaslang.collection.Set;
import javaslang.control.Option;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class RequestCookies {

    private final Map<String, Cookie> cookies;

    RequestCookies(HttpServletRequest request) {
        this.cookies = Option.of(request.getCookies()).map(cookies -> {
            Map<String, Cookie> _cookies = HashMap.empty();
            for (Cookie cookie : cookies) {
                _cookies = _cookies.put(cookie.getName(), cookie);
            }
            return _cookies;
        }).getOrElse(HashMap.empty());
    }

    public Map<String, Cookie> raw() {
        return cookies;
    }

    public Set<String> cookieNames() {
        return cookies.keySet();
    }

    public Option<Cookie> cookie(String name) {
        return cookies.get(name);
    }
}
