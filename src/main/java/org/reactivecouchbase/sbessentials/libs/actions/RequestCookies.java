package org.reactivecouchbase.sbessentials.libs.actions;

import javaslang.collection.*;
import org.reactivecouchbase.functional.Option;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class RequestCookies {

    private final Map<String, Cookie> cookies;

    public RequestCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            Map<String, Cookie> _cookies = HashMap.empty();
            for (Cookie cookie : request.getCookies()) {
                _cookies = _cookies.put(cookie.getName(), cookie);
            }
            cookies = _cookies;
        } else {
            cookies = HashMap.empty();
        }
    }

    public Map<String, Cookie> raw() {
        return cookies;
    }

    public Set<String> cookieNames() {
        return cookies.keySet();
    }

    public Option<Cookie> cookie(String name) {
        return cookies.get(name).transform(opt -> {
            if (opt.isDefined()) {
                return Option.apply(opt.get());
            } else {
                return Option.none();
            }
        });
    }
}
