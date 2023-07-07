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
import org.apache.lucene.document.StoredField;
import org.elasticsearch.index.mapper.DocumentParserContext;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.MapperBuilderContext;
import org.elasticsearch.index.mapper.MappingParserContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class DecryptFieldType extends CryptographyFieldType {
    public static final String CONTENT_TYPE = "decrypt";
    public static final FieldMapper.TypeParser PARSER = new FieldMapper.TypeParser(Builder::new);

    protected static class Builder extends CryptographyFieldMapper.Builder {
        public Builder(String name) {
            super(name);
        }

        protected Builder(String name, MappingParserContext mappingParserContext) {
            super(name);
        }

        @Override
        public FieldMapper build(MapperBuilderContext context) {
            stored.setValue(true);  // Always store processed data
            return new DecryptFieldMapper(
                name,
                new DecryptFieldType(
                    context.buildFullName(name),
                    stored.getValue(),
                    hasDocValues.getValue(),
                    meta.getValue(),
                    key.getValue(),
                    cipher.getValue()
                ),
                multiFieldsBuilder.build(this, context),
                copyTo.build(),
                this
            );
        }
    }

    protected static class DecryptFieldMapper extends CryptographyFieldMapper {
        private static final Logger logger = LogManager.getLogger(DecryptFieldMapper.class);

        protected DecryptFieldMapper(
            String name,
            MappedFieldType mappedFieldType,
            MultiFields multiFields,
            CopyTo copyTo,
            Builder builder
        ) {
            super(name, mappedFieldType, multiFields, copyTo, builder);
        }

        @Override
        protected String contentType() {
            return CONTENT_TYPE;
        }

        @Override
        public FieldMapper.Builder getMergeBuilder() {
            return new DecryptFieldType.Builder(simpleName()).init(this);
        }

        @Override
        protected void parseCreateField(DocumentParserContext context) throws IOException {
            final String value = context.parser().textOrNull();

            if (value != null && fieldType().isStored()) {
                byte[] original = Base64.getDecoder().decode(value.replaceAll("\\s", ""));
                logger.trace("[{}] Decrypting [{}] bytes of data [{}]", cipher.name(), original.length, value);

                byte[] decrypted = cipher.init(decodeKey()).decrypt(original);
                logger.trace("[{}] Decrypted to [{}] bytes of data", cipher.name(), decrypted.length);

                String storing = new String(decrypted, StandardCharsets.UTF_8);
                context.doc().add(new StoredField(name(), storing));
            }
        }
    }

    protected DecryptFieldType(
        String name,
        boolean isStored,
        boolean hasDocValues,
        Map<String, String> meta,
        String key,
        CipherType cipher
    ) {
        super(name, isStored, hasDocValues, meta, key, cipher);
    }

    @Override
    public String typeName() {
        return CONTENT_TYPE;
    }
}
