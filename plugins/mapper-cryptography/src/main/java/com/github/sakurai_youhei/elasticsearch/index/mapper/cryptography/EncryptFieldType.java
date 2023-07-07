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

public class EncryptFieldType extends CryptographyFieldType {
    public static final String CONTENT_TYPE = "encrypt";
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
            return new EncryptFieldMapper(
                name,
                new EncryptFieldType(
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

    protected static class EncryptFieldMapper extends CryptographyFieldMapper {
        private static final Logger logger = LogManager.getLogger(EncryptFieldMapper.class);

        protected EncryptFieldMapper(
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
            return new EncryptFieldType.Builder(simpleName()).init(this);
        }

        @Override
        protected void parseCreateField(DocumentParserContext context) throws IOException {
            final String value = context.parser().textOrNull();

            if (value != null && fieldType().isStored()) {
                byte[] original = value.getBytes(StandardCharsets.UTF_8);
                logger.trace("[{}] Encrypting [{}] bytes of data", cipher.name(), original.length);

                byte[] encrypted = cipher.init(decodeKey()).encrypt(original);
                String storing = Base64.getEncoder().encodeToString(encrypted);
                logger.trace("[{}] Encrypted to [{}] bytes of data [{}]", cipher.name(), encrypted.length, storing);

                context.doc().add(new StoredField(name(), storing));
            }
        }
    }

    protected EncryptFieldType(
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
