package org.reactivecouchbase.sbessentials.libs.streams;

import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class SourceMessageConverter implements HttpMessageConverter<Source<ByteString, ?>> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return Source.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.ALL);
    }

    @Override
    public Source<ByteString, ?> read(Class<? extends Source<ByteString, ?>> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return StreamConverters.fromInputStream(inputMessage::getBody);
    }

    @Override
    public void write(Source<ByteString, ?> source, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
       throw new RuntimeException("Can't write a Source just like that");
    }
}
