package org.reactivecouchbase.sbessentials.libs.result;

import akka.stream.javadsl.Source;
import akka.util.ByteString;
import javaslang.collection.HashMap;
import javaslang.collection.HashSet;
import javaslang.collection.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.Cookie;

@Component
public class Results {

    static WebApplicationContext webApplicationContext;

    @Autowired
    public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        Results.webApplicationContext = webApplicationContext;
    }

    /** Generates a ‘100 Continue’ result. */
    public static Result Continue = new Result(HttpStatus.CONTINUE.value());

    /** Generates a ‘101 Switching Protocols’ result. */
    public static Result SwitchingProtocols = new Result(HttpStatus.SWITCHING_PROTOCOLS.value());

    /** Generates a ‘200 OK’ result. */
    public static Result Ok = new Result(HttpStatus.OK.value());

    /** Generates a ‘201 CREATED’ result. */
    public static Result Created = new Result(HttpStatus.CREATED.value());

    /** Generates a ‘202 ACCEPTED’ result. */
    public static Result Accepted = new Result(HttpStatus.ACCEPTED.value());

    /** Generates a ‘203 NON_AUTHORITATIVE_INFORMATION’ result. */
    public static Result NonAuthoritativeInformation = new Result(HttpStatus.NON_AUTHORITATIVE_INFORMATION.value());

    /** Generates a ‘204 NO_CONTENT’ result. */
    public static Result NoContent = new Result(HttpStatus.NO_CONTENT.value());

    /** Generates a ‘205 RESET_CONTENT’ result. */
    public static Result ResetContent = new Result(HttpStatus.RESET_CONTENT.value());

    /** Generates a ‘206 PARTIAL_CONTENT’ result. */
    public static Result PartialContent = new Result(HttpStatus.PARTIAL_CONTENT.value());

    /** Generates a ‘207 MULTI_STATUS’ result. */
    public static Result MultiStatus = new Result(HttpStatus.MULTI_STATUS.value());

    /**
     * Generates a ‘301 MOVED_PERMANENTLY’ simple result.
     *
     * @param url the URL to redirect to
     */
    public static Result MovedPermanently(String url) {
        return new Result(
                HttpStatus.MOVED_PERMANENTLY.value(),
                Source.<ByteString>empty(),
                "text/plain",
                HashMap.<String, List<String>>empty().put("Location", List.of(url)),
                HashSet.<Cookie>empty()
        );
    }

    /**
     * Generates a ‘302 FOUND’ simple result.
     *
     * @param url the URL to redirect to
     */
    public static Result Found(String url) {
        return new Result(
                HttpStatus.FOUND.value(),
                Source.<ByteString>empty(),
                "text/plain",
                HashMap.<String, List<String>>empty().put("Location", List.of(url)),
                HashSet.<Cookie>empty()
        );
    }

    /**
     * Generates a ‘303 SEE_OTHER’ simple result.
     *
     * @param url the URL to redirect to
     */
    public static Result SeeOther(String url) {
        return new Result(
                HttpStatus.SEE_OTHER.value(),
                Source.<ByteString>empty(),
                "text/plain",
                HashMap.<String, List<String>>empty().put("Location", List.of(url)),
                HashSet.<Cookie>empty()
        );
    }

    /** Generates a ‘304 NOT_MODIFIED’ result. */
    public static Result NotModified = new Result(HttpStatus.NOT_MODIFIED.value());

    /**
     * Generates a ‘307 TEMPORARY_REDIRECT’ simple result.
     *
     * @param url the URL to redirect to
     */
    public static Result TemporaryRedirect(String url) {
        return new Result(
                HttpStatus.TEMPORARY_REDIRECT.value(),
                Source.<ByteString>empty(),
                "text/plain",
                HashMap.<String, List<String>>empty().put("Location", List.of(url)),
                HashSet.<Cookie>empty()
        );
    }

    /**
     * Generates a ‘308 PERMANENT_REDIRECT’ simple result.
     *
     * @param url the URL to redirect to
     */
    public static Result PermanentRedirect(String url) {
        return new Result(
                HttpStatus.PERMANENT_REDIRECT.value(),
                Source.<ByteString>empty(),
                "text/plain",
                HashMap.<String, List<String>>empty().put("Location", List.of(url)),
                HashSet.<Cookie>empty()
        );
    }

    /** Generates a ‘400 BAD_REQUEST’ result. */
    public static Result BadRequest = new Result(HttpStatus.BAD_REQUEST.value());

    /** Generates a ‘401 UNAUTHORIZED’ result. */
    public static Result Unauthorized = new Result(HttpStatus.UNAUTHORIZED.value());

    /** Generates a ‘402 PAYMENT_REQUIRED’ result. */
    public static Result PaymentRequired = new Result(HttpStatus.PAYMENT_REQUIRED.value());

    /** Generates a ‘403 FORBIDDEN’ result. */
    public static Result Forbidden = new Result(HttpStatus.FORBIDDEN.value());

    /** Generates a ‘404 NOT_FOUND’ result. */
    public static Result NotFound = new Result(HttpStatus.NOT_FOUND.value());

    /** Generates a ‘405 METHOD_NOT_ALLOWED’ result. */
    public static Result MethodNotAllowed = new Result(HttpStatus.METHOD_NOT_ALLOWED.value());

    /** Generates a ‘406 NOT_ACCEPTABLE’ result. */
    public static Result NotAcceptable = new Result(HttpStatus.NOT_ACCEPTABLE.value());

    /** Generates a ‘408 REQUEST_TIMEOUT’ result. */
    public static Result RequestTimeout = new Result(HttpStatus.REQUEST_TIMEOUT.value());

    /** Generates a ‘409 CONFLICT’ result. */
    public static Result Conflict = new Result(HttpStatus.CONFLICT.value());

    /** Generates a ‘410 GONE’ result. */
    public static Result Gone = new Result(HttpStatus.GONE.value());

    /** Generates a ‘412 PRECONDITION_FAILED’ result. */
    public static Result PreconditionFailed = new Result(HttpStatus.PRECONDITION_FAILED.value());

    /** Generates a ‘413 REQUEST_ENTITY_TOO_LARGE’ result. */
    public static Result EntityTooLarge = new Result(HttpStatus.REQUEST_ENTITY_TOO_LARGE.value());

    /** Generates a ‘414 REQUEST_URI_TOO_LONG’ result. */
    public static Result UriTooLong = new Result(HttpStatus.REQUEST_URI_TOO_LONG.value());

    /** Generates a ‘415 UNSUPPORTED_MEDIA_TYPE’ result. */
    public static Result UnsupportedMediaType = new Result(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());

    /** Generates a ‘417 EXPECTATION_FAILED’ result. */
    public static Result ExpectationFailed = new Result(HttpStatus.EXPECTATION_FAILED.value());

    /** Generates a ‘422 UNPROCESSABLE_ENTITY’ result. */
    public static Result UnprocessableEntity = new Result(HttpStatus.UNPROCESSABLE_ENTITY.value());

    /** Generates a ‘423 LOCKED’ result. */
    public static Result Locked = new Result(HttpStatus.LOCKED.value());

    /** Generates a ‘424 FAILED_DEPENDENCY’ result. */
    public static Result FailedDependency = new Result(HttpStatus.FAILED_DEPENDENCY.value());

    /** Generates a ‘429 TOO_MANY_REQUESTS’ result. */
    public static Result TooManyRequests = new Result(HttpStatus.TOO_MANY_REQUESTS.value());

    /** Generates a ‘500 INTERNAL_SERVER_ERROR’ result. */
    public static Result InternalServerError = new Result(HttpStatus.INTERNAL_SERVER_ERROR.value());

    /** Generates a ‘501 NOT_IMPLEMENTED’ result. */
    public static Result NotImplemented = new Result(HttpStatus.NOT_IMPLEMENTED.value());

    /** Generates a ‘502 BAD_GATEWAY’ result. */
    public static Result BadGateway = new Result(HttpStatus.BAD_GATEWAY.value());

    /** Generates a ‘503 SERVICE_UNAVAILABLE’ result. */
    public static Result ServiceUnavailable = new Result(HttpStatus.SERVICE_UNAVAILABLE.value());

    /** Generates a ‘504 GATEWAY_TIMEOUT’ result. */
    public static Result GatewayTimeout = new Result(HttpStatus.GATEWAY_TIMEOUT.value());

    /** Generates a ‘505 HTTP_VERSION_NOT_SUPPORTED’ result. */
    public static Result HttpVersionNotSupported = new Result(HttpStatus.HTTP_VERSION_NOT_SUPPORTED.value());

    /** Generates a ‘507 INSUFFICIENT_STORAGE’ result. */
    public static Result InsufficientStorage = new Result(HttpStatus.INSUFFICIENT_STORAGE.value());

    public static Result status(int code) {
        return new Result(code);
    }

    public static Result redirect(String url) {
        return new Result(
            200,
            Source.<ByteString>empty(),
            "text/plain",
            HashMap.<String, List<String>>empty().put("Location", List.of(url)),
            HashSet.<Cookie>empty()
        );
    }
}
