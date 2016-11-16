package org.reactivecouchbase.sbessentials.libs.json;

import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.json.Throwables;
import org.reactivecouchbase.sbessentials.config.Tools;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JsonMessageConverter implements HttpMessageConverter<JsValue> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return JsValue.class.isAssignableFrom(clazz) && (mediaType.equals(MediaType.APPLICATION_JSON) || mediaType.equals(MediaType.APPLICATION_JSON_UTF8));
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return JsValue.class.isAssignableFrom(clazz) && (mediaType.equals(MediaType.APPLICATION_JSON) || mediaType.equals(MediaType.APPLICATION_JSON_UTF8));
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        List<MediaType> types = new ArrayList<>();
        types.add(MediaType.APPLICATION_JSON);
        types.add(MediaType.APPLICATION_JSON_UTF8);
        return types;
    }

    @Override
    public JsValue read(Class<? extends JsValue> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return Json.parse(Tools.fromInputStream(inputMessage.getBody()));
    }

    @Override
    public void write(JsValue jsValue, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        outputMessage.getHeaders().set("Content-Type", "application/json;charset=UTF-8");
        outputMessage.getBody().write(Json.stringify(jsValue).getBytes("UTF-8"));
    }
}