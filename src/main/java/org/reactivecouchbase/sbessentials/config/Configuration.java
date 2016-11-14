package org.reactivecouchbase.sbessentials.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javaslang.collection.List;
import org.reactivecouchbase.functional.Option;

import java.util.function.Supplier;

public class Configuration {

    private final Config underlying;

    Configuration(Config underlying) {
        this.underlying = underlying;
    }

    private <T> Option<T> readValue(String path, Supplier<T> supplier) {
        try {
            return Option.some(supplier.get());
        } catch (Exception e) {
            return Option.none();
        }
    }

    private <T> List<T> readList(String path, Supplier<java.util.List<T>> supplier) {
        try {
            return Option.some(supplier.get()).map(List::ofAll).getOrElse(List.empty());
        } catch (Exception e) {
            return List.empty();
        }
    }

    public Option<String> getString(String path) {
        return readValue(path, () -> underlying.getString(path));
    }

    public Option<Integer> getInt(String path) {
        return readValue(path, () -> underlying.getInt(path));
    }

    public Option<Boolean> getBoolean(String path) {
        return readValue(path, () -> underlying.getBoolean(path));
    }

    public Option<Double> getDouble(String path) {
        return readValue(path, () -> underlying.getDouble(path));
    }

    public Option<Long> getLong(String path) {
        return readValue(path, () -> underlying.getLong(path));
    }

    public List<Double> getDoubleList(String path) {
        return readList(path, () -> underlying.getDoubleList(path));
    }

    public List<Integer> getIntList(String path) {
        return readList(path, () -> underlying.getIntList(path));
    }

    public List<Long> getLongList(String path) {
        return readList(path, () -> underlying.getLongList(path));
    }

    public List<String> getStringList(String path) {
        return readList(path, () -> underlying.getStringList(path));
    }

    public Configuration at(String path) {
        return new Configuration(underlying.atPath(path).withFallback(ConfigFactory.empty()));
    }
}
