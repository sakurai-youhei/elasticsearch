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
import org.elasticsearch.index.mapper.MappingParserContext;
import org.elasticsearch.index.mapper.OnScriptError;
import org.elasticsearch.index.mapper.RuntimeField;
import org.elasticsearch.script.CompositeFieldScript;
import org.elasticsearch.search.lookup.SearchLookup;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

public interface CryptographyRuntimeField extends RuntimeField {

    abstract class Builder extends RuntimeField.Builder {
        protected final String name;

        protected Builder(String name) {
            super(name);
            this.name = name;
            this.key.requiresParameter(cipher);
        }

        protected final FieldMapper.Parameter<String> key = FieldMapper.Parameter.stringParam(
            "key",
            false,
            RuntimeField.initializerNotSupported(),
            null
        ).addValidator(key -> {
            if (Strings.isEmpty(key)) {
                throw new IllegalArgumentException("[key] parameter must be specified");
            }
        });

        protected final FieldMapper.Parameter<CipherType> cipher = FieldMapper.Parameter.restrictedEnumParam(
            "cipher",
            false,
            RuntimeField.initializerNotSupported(),
            null,
            CipherType.class,
            EnumSet.of(CipherType.XOR, CipherType.RSA)
        );

        @Override
        protected List<FieldMapper.Parameter<?>> getParameters() {
            final List<FieldMapper.Parameter<?>> parameters = new ArrayList<>(super.getParameters());
            parameters.add(key);
            parameters.add(cipher);
            return parameters;
        }

        @Override
        protected RuntimeField createChildRuntimeField(
            MappingParserContext parserContext,
            String parentName,
            Function<SearchLookup, CompositeFieldScript.LeafFactory> parentScriptFactory,
            OnScriptError onScriptError
        ) {
            throw new UnsupportedOperationException();
        }
    }
}
