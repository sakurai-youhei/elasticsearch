/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package com.github.sakurai_youhei.elasticsearch.plugin.mapper;

import com.github.sakurai_youhei.elasticsearch.index.mapper.cryptography.DecryptFieldType;
import com.github.sakurai_youhei.elasticsearch.index.mapper.cryptography.DecryptRuntimeFieldType;
import com.github.sakurai_youhei.elasticsearch.index.mapper.cryptography.EncryptFieldType;
import com.github.sakurai_youhei.elasticsearch.index.mapper.cryptography.EncryptRuntimeFieldType;

import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.RuntimeField;
import org.elasticsearch.plugins.MapperPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapperCryptographyPlugin extends Plugin implements MapperPlugin {

    @Override
    public Map<String, Mapper.TypeParser> getMappers() {
        Map<String, Mapper.TypeParser> mappers = new LinkedHashMap<>();
        mappers.put(DecryptFieldType.CONTENT_TYPE, DecryptFieldType.PARSER);
        mappers.put(EncryptFieldType.CONTENT_TYPE, EncryptFieldType.PARSER);
        return Collections.unmodifiableMap(mappers);
    }

    @Override
    public Map<String, RuntimeField.Parser> getRuntimeFields() {
        Map<String, RuntimeField.Parser> runtimeParsers = new LinkedHashMap<>();
        runtimeParsers.put(DecryptRuntimeFieldType.CONTENT_TYPE, DecryptRuntimeFieldType.PARSER);
        runtimeParsers.put(EncryptRuntimeFieldType.CONTENT_TYPE, EncryptRuntimeFieldType.PARSER);
        return Collections.unmodifiableMap(runtimeParsers);
    }
}
