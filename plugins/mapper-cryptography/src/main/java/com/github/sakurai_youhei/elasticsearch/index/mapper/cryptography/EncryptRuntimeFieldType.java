/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package com.github.sakurai_youhei.elasticsearch.index.mapper.cryptography;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.mapper.LeafRuntimeField;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.MappingParserContext;
import org.elasticsearch.index.mapper.RuntimeField;
import org.elasticsearch.index.mapper.StoredValueFetcher;
import org.elasticsearch.index.mapper.ValueFetcher;
import org.elasticsearch.index.query.SearchExecutionContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class EncryptRuntimeFieldType extends EncryptFieldType {
    public static final RuntimeField.Parser PARSER = new RuntimeField.Parser(Builder::new);

    protected static class Builder extends CryptographyRuntimeField.Builder {
        protected Builder(String name) {
            super(name);
        }

        @Override
        protected RuntimeField createRuntimeField(MappingParserContext parserContext) {
            final MappedFieldType ft = new EncryptRuntimeFieldType(name, meta(), key.get(), cipher.get());
            return new LeafRuntimeField(name, ft, getParameters());
        }
    }

    public EncryptRuntimeFieldType(String name, Map<String, String> meta, String key, CipherType cipher) {
        super(name, false, false, meta, key, cipher);
    }

    @Override
    public ValueFetcher valueFetcher(SearchExecutionContext context, String format) {
        return new StoredValueFetcher(context.lookup(), name()) {
            private static final Logger logger = LogManager.getLogger(StoredValueFetcher.class);

            @Override
            public List<Object> parseStoredValues(List<Object> storedValues) {
                final List<Object> values = new ArrayList<>(storedValues.size());

                for (Object storedValue : storedValues) {
                    if (storedValue instanceof String value) {
                        byte[] original = value.getBytes(StandardCharsets.UTF_8);
                        logger.trace("[{}] Encrypting [{}] bytes of data", cipher.name(), original.length);

                        byte[] encrypted = cipher.init(decodeKey()).encrypt(original);
                        String storing = Base64.getEncoder().encodeToString(encrypted);
                        logger.trace("[{}] Encrypted to [{}] bytes of data [{}]", cipher.name(), encrypted.length, storing);

                        values.add(storing);
                    } else {
                        throw new IllegalArgumentException("Unexpected class fetching [" + name() + "]: " + storedValue.getClass());
                    }
                }
                return values;
            }
        };
    }
}
