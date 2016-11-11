package org.reactivecouchbase.sbessentials.libs.ws;

import akka.util.ByteString;
import org.reactivecouchbase.common.Throwables;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public class WSBody {

    private final ByteString underlying;

    public WSBody(ByteString underlying) {
        this.underlying = underlying;
    }

    public ByteString bytes() {
        return underlying;
    }

    public String body() {
        return underlying.utf8String();
    }

    public JsValue json() {
        return Json.parse(body());
    }

    public Node xml() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(body())));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
