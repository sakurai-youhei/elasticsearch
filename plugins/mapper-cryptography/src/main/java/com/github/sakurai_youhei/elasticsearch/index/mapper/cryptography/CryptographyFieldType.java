/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package com.github.sakurai_youhei.elasticsearch.index.mapper.cryptography;

import org.apache.lucene.search.Query;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.TextSearchInfo;
import org.elasticsearch.index.mapper.ValueFetcher;
import org.elasticsearch.index.query.SearchExecutionContext;

import java.util.Base64;
import java.util.Map;

import static org.elasticsearch.common.Strings.format;

public abstract class CryptographyFieldType extends MappedFieldType {
    protected final String key;
    protected final CipherType cipher;

    protected CryptographyFieldType(
        String name,
        boolean isStored,
        boolean hasDocValues,
        Map<String, String> meta,
        String key,
        CipherType cipher
    ) {
        super(name, false, isStored, hasDocValues, TextSearchInfo.NONE, meta);
        this.key = key;
        this.cipher = cipher;
    }

    protected byte[] decodeKey() {
        return Base64.getDecoder().decode(key.replaceAll("\\s", ""));
    }

    @Override
    public ValueFetcher valueFetcher(SearchExecutionContext context, String format) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query termQuery(Object value, SearchExecutionContext context) {
        throw new IllegalArgumentException(format("%s fields do not support searching", typeName()));
    }
}
