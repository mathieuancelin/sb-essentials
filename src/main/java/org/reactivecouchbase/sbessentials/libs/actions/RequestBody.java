package org.reactivecouchbase.sbessentials.libs.actions;

import akka.util.ByteString;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.control.Try;
import org.reactivecouchbase.common.Throwables;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Arrays;

public class RequestBody {

    private final ByteString bodyAsBytes;

    RequestBody(ByteString bodyAsBytes) {
        this.bodyAsBytes = bodyAsBytes;
    }

    public ByteString asBytes() {
        return bodyAsBytes;
    }

    public String asString() {
        return bodyAsBytes.utf8String();
    }

    public JsValue asJson() {
        return Json.parse(asString());
    }

    public Try<JsValue> asSafeJson() {
        try {
            return Try.success(Json.parse(asString()));
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    public Node asXml() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(asString())));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public Try<Node> asSafeXml() {
        try {
            return Try.success(DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(asString()))));
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    public Try<Map<String, List<String>>> asSafeURLForm() {
        try {
            return Try.success(asURLForm());
        } catch (Exception e) {
            return Try.failure(e);
        }
    }

    public Map<String, List<String>> asURLForm() {
        Map<String, List<String>> form = HashMap.empty();
        String body = asString();
        List<String> parts = List.ofAll(Arrays.asList(body.split("&")));
        for (String part : parts) {
            String key = part.split("=")[0];
            String value = part.split("=")[1];
            if (!form.containsKey(key)) {
                form = form.put(key, List.empty());
            }
            form = form.put(key, form.get(key).get().append(value));
        }
        return form;
    }
}

