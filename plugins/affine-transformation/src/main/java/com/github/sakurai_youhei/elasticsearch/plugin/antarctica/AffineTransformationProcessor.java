/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package com.github.sakurai_youhei.elasticsearch.plugin.antarctica;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.Strings.format;
import static org.elasticsearch.ingest.ConfigurationUtils.readBooleanProperty;
import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

public final class AffineTransformationProcessor extends AbstractProcessor {
    private static final Logger logger = LogManager.getLogger(AffineTransformationProcessor.class);

    public static final String TYPE = "affine_transformation";

    private final String field;
    private final boolean ignoreMissing;
    private final String targetField;
    private final String transformationMatrixField;

    private AffineTransformationProcessor(
        final String tag,
        final String description,
        String field,
        boolean ignoreMissing,
        String targetField,
        String transformationMatrixField
    ) {
        super(tag, description);
        this.field = field;
        this.ignoreMissing = ignoreMissing;
        this.targetField = targetField;
        this.transformationMatrixField = transformationMatrixField;
    }

    public String getField() {
        return field;
    }

    boolean isIgnoreMissing() {
        return ignoreMissing;
    }

    String getTargetField() {
        return targetField;
    }

    String getTransformationMatrixField() {
        return transformationMatrixField;
    }

    private static RealMatrix tryParseTransformationMatrix(Object o) {
        if (o instanceof String source) {
            return AssemblingUtils.parseTransformationMatrix(source);
        } else {
            throw new IllegalArgumentException(format("value [%s] of type [%s] cannot be cast to String", o, o.getClass().getName()));
        }
    }

    @Override
    public IngestDocument execute(final IngestDocument document) throws Exception {
        logger.trace("Called: execute([{}])", document);

        Object fieldValue = document.getFieldValue(field, Object.class, ignoreMissing);
        Object transformationMatrixFieldValue = document.getFieldValue(transformationMatrixField, Object.class, ignoreMissing);
        Object newValue;

        if (fieldValue == null || transformationMatrixFieldValue == null && ignoreMissing) {
            return document;
        } else if (fieldValue == null) {
            throw new IllegalArgumentException(format("field [%s] is null, cannot process it.", field));
        } else if (transformationMatrixFieldValue == null) {
            throw new IllegalArgumentException(format("field [%s] is null, cannot process it.", transformationMatrixField));
        }

        final double[] originalVector;
        if (fieldValue instanceof List<?>) {
            originalVector = AssemblingUtils.extractDoubleArray(fieldValue);
        } else {
            throw new IllegalArgumentException(
                format("field [%s] of type [%s] cannot be cast to List", field, fieldValue.getClass().getName())
            );
        }

        RealMatrix matrix;
        try {
            matrix = tryParseTransformationMatrix(transformationMatrixFieldValue);
        } catch (IllegalArgumentException e) {
            if (transformationMatrixFieldValue instanceof List<?> list) {
                if (list.isEmpty()) {
                    throw new IllegalArgumentException(format("field [%s] is empty array", transformationMatrixField));
                }
                Iterator<?> iter = list.iterator();
                matrix = tryParseTransformationMatrix(iter.next());
                while (iter.hasNext()) {
                    RealMatrix m = tryParseTransformationMatrix(iter.next());
                    logger.trace("Transformation matrix: [{}] x [{}]", m, matrix);
                    matrix = m.multiply(matrix);
                }
            } else {
                throw e;
            }
        }

        RealMatrix vector = AssemblingUtils.argumentVector(originalVector);
        logger.trace("Affine transformation: [{}] x [{}]", matrix, vector);

        double[] transformedVector = AssemblingUtils.unargumentVector(matrix.multiply(vector));
        logger.trace("Transformed: [{}] to [{}]", originalVector, transformedVector);

        document.setFieldValue(targetField, transformedVector);
        return document;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory implements Processor.Factory {

        @Override
        public Processor create(
            final Map<String, Processor.Factory> processorFactories,
            final String tag,
            final String description,
            final Map<String, Object> config
        ) {
            String field = readStringProperty(TYPE, tag, config, "field");
            boolean ignoreMissing = readBooleanProperty(TYPE, tag, config, "ignore_missing", false);
            String targetField = readStringProperty(TYPE, tag, config, "target_field", field);
            String transformationMatrixField = readStringProperty(TYPE, tag, config, "transformation_matrix_field");
            return new AffineTransformationProcessor(tag, description, field, ignoreMissing, targetField, transformationMatrixField);
        }
    }
}
