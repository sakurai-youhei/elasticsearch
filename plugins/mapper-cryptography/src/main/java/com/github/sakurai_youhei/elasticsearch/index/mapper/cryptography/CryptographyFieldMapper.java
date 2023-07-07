/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package com.github.sakurai_youhei.elasticsearch.index.mapper.cryptography;

import org.elasticsearch.common.Strings;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MappedFieldType;

import java.util.Base64;
import java.util.EnumSet;
import java.util.Map;

public abstract class CryptographyFieldMapper extends FieldMapper {

    protected abstract static class Builder extends FieldMapper.Builder {
        protected Builder(String name) {
            super(name);
            this.key.requiresParameter(cipher);
        }

        private static CryptographyFieldMapper toType(FieldMapper in) {
            return (CryptographyFieldMapper) in;
        }

        protected final Parameter<Boolean> stored = Parameter.storeParam(m -> toType(m).stored, false);
        protected final Parameter<Boolean> hasDocValues = Parameter.docValuesParam(m -> toType(m).hasDocValues, false);
        protected final Parameter<Map<String, String>> meta = Parameter.metaParam();

        protected final Parameter<String> key = Parameter.stringParam("key", false, m -> toType(m).key, null).addValidator(key -> {
            if (Strings.isEmpty(key)) {
                throw new IllegalArgumentException("[key] parameter must be specified");
            }
        });

        protected final FieldMapper.Parameter<CipherType> cipher = FieldMapper.Parameter.restrictedEnumParam(
            "cipher",
            false,
            m -> toType(m).cipher,
            null,
            CipherType.class,
            EnumSet.of(CipherType.XOR, CipherType.RSA)
        );

        @Override
        public Parameter<?>[] getParameters() {
            return new Parameter<?>[] { meta, stored, hasDocValues, key, cipher };
        }
    }

    private final boolean stored;
    private final boolean hasDocValues;
    protected final String key;
    protected final CipherType cipher;

    protected CryptographyFieldMapper(
        String name,
        MappedFieldType mappedFieldType,
        MultiFields multiFields,
        CopyTo copyTo,
        Builder builder
    ) {
        super(name, mappedFieldType, multiFields, copyTo);
        this.stored = builder.stored.getValue();
        this.hasDocValues = builder.hasDocValues.getValue();
        this.key = builder.key.getValue();
        this.cipher = builder.cipher.getValue();
    }

    protected byte[] decodeKey() {
        return Base64.getDecoder().decode(key.replaceAll("\\s", ""));
    }
}
