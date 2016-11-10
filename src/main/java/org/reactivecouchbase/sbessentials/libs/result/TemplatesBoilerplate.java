package org.reactivecouchbase.sbessentials.libs.result;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;

public class TemplatesBoilerplate {

    public final String prefix;
    public final String suffix;
    private final TemplateLoader loader;
    private final Handlebars handlebars;

    private TemplatesBoilerplate(Builder builder) {
        this.prefix = builder.prefix;
        this.suffix = builder.suffix;
        this.loader = new ClassPathTemplateLoader();
        this.loader.setPrefix(prefix);
        this.loader.setSuffix(suffix);
        this.handlebars = new Handlebars(loader);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(TemplatesBoilerplate copy) {
        Builder builder = new Builder();
        builder.prefix = copy.prefix;
        builder.suffix = copy.suffix;
        return builder;
    }

    public Handlebars handlebars() {
        return handlebars;
    }

    public static final class Builder {
        private String prefix;
        private String suffix;

        private Builder() {
        }

        public Builder withPrefix(String val) {
            prefix = val;
            return this;
        }

        public Builder withSuffix(String val) {
            suffix = val;
            return this;
        }

        public TemplatesBoilerplate build() {
            return new TemplatesBoilerplate(this);
        }
    }
}
