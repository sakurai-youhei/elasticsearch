/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package com.github.sakurai_youhei.elasticsearch.plugin.antarctica;

import org.elasticsearch.ingest.Processor;
import org.elasticsearch.plugins.IngestPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class AffineTransformationPlugin extends Plugin implements SearchPlugin, IngestPlugin {
    @Override
    public Map<String, Processor.Factory> getProcessors(Processor.Parameters parameters) {
        return Map.of(AffineTransformationProcessor.TYPE, new AffineTransformationProcessor.Factory());
    }

    @Override
    public List<QueryVectorBuilderSpec<?>> getQueryVectorBuilders() {
        return singletonList(
            new QueryVectorBuilderSpec<>(
                AffineTransformationQueryVectorBuilder.NAME,
                AffineTransformationQueryVectorBuilder::new,
                AffineTransformationQueryVectorBuilder.PARSER
            )
        );
    }
}
