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

    public final ByteString bodyAsBytes;

    public WSBody(ByteString bodyAsBytes) {
        this.bodyAsBytes = bodyAsBytes;
    }

    public String body() {
        return bodyAsBytes.utf8String();
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
