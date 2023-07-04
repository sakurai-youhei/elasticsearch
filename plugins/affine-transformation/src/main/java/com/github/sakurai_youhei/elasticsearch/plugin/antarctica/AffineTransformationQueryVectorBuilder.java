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
import org.elasticsearch.TransportVersion;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.search.vectors.QueryVectorBuilder;
import org.elasticsearch.xcontent.ConstructingObjectParser;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.elasticsearch.common.Strings.format;
import static org.elasticsearch.xcontent.ConstructingObjectParser.constructorArg;

public class AffineTransformationQueryVectorBuilder implements QueryVectorBuilder {
    private static final Logger logger = LogManager.getLogger(AffineTransformationQueryVectorBuilder.class);

    public static final String NAME = "affine_transformation";
    public static final ParseField QUERY_VECTOR_FIELD = new ParseField("query_vector");
    public static final ParseField TRANSFORMATION_MATRIX_FIELD = new ParseField("transformation_matrix");
    public static final ConstructingObjectParser<AffineTransformationQueryVectorBuilder, Void> PARSER = new ConstructingObjectParser<>(
        NAME,
        args -> {
            final double[] vector = AssemblingUtils.extractVector(args[0]);
            return new AffineTransformationQueryVectorBuilder(vector, (String) args[1]);
        }
    );
    static {
        PARSER.declareDoubleArray(constructorArg(), QUERY_VECTOR_FIELD);
        PARSER.declareString(constructorArg(), TRANSFORMATION_MATRIX_FIELD);
    }

    public static AffineTransformationQueryVectorBuilder fromXContent(XContentParser parser) throws IOException {
        logger.trace("Called: fromXContent([{}])", parser);

        return PARSER.parse(parser, null);
    }

    final double[] queryVector;
    final String transformationMatrix;

    private AffineTransformationQueryVectorBuilder(double[] queryVector, String transformationMatrix) {
        logger.trace("Called: AffineTransformationQueryVectorBuilder([{}], [{}])", queryVector, transformationMatrix);

        this.queryVector = requireNonNull(queryVector, format("[%s] cannot be null", QUERY_VECTOR_FIELD));
        this.transformationMatrix = requireNonNull(transformationMatrix, format("[%s] cannot be null", TRANSFORMATION_MATRIX_FIELD));
    }

    public AffineTransformationQueryVectorBuilder(StreamInput in) throws IOException {
        logger.trace("Called: AffineTransformationQueryVectorBuilder([{}])", in);

        this.queryVector = in.readDoubleArray();
        this.transformationMatrix = in.readString();
    }

    @Override
    public String getWriteableName() {
        logger.trace("Called: getWriteableName()");

        return NAME;
    }

    @Override
    public TransportVersion getMinimalSupportedVersion() {
        logger.trace("Called: getMinimalSupportedVersion()");

        return TransportVersion.V_8_7_0;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        logger.trace("Called: StreamOutput([{}])", out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        logger.trace("Called: toXContent([{}], [{}])", builder, params);

        builder.startObject();
        builder.field(QUERY_VECTOR_FIELD.getPreferredName(), queryVector);
        builder.field(TRANSFORMATION_MATRIX_FIELD.getPreferredName(), transformationMatrix);
        builder.endObject();
        return builder;
    }

    @Override
    public void buildVector(Client client, ActionListener<float[]> listener) {
        logger.trace("Called: buildVector([{}], [{}])", client, listener);

        try {
            RealMatrix matrix = AssemblingUtils.parseTransformationMatrix(transformationMatrix);
            RealMatrix vector = AssemblingUtils.argumentVector(queryVector);
            logger.trace("Affine Transformation: [{}] x [{}]", matrix, vector);

            float[] transformedQueryVector = AssemblingUtils.unargumentVectorFloat(matrix.multiply(vector));
            logger.trace("Transformed: [{}] to [{}]", queryVector, transformedQueryVector);
            listener.onResponse(transformedQueryVector);
        } catch (IllegalArgumentException e) {
            listener.onFailure(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        logger.trace("Called: equals([{}])", o);

        if (o == null) {
            return false;
        } else if (o instanceof AffineTransformationQueryVectorBuilder) {
            AffineTransformationQueryVectorBuilder obj = (AffineTransformationQueryVectorBuilder) o;
            return Objects.equals(queryVector, obj.queryVector) && Objects.equals(transformationMatrix, obj.transformationMatrix);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        logger.trace("Called: hashCode()");

        return Objects.hash(this.getClass(), Objects.hash(queryVector, transformationMatrix));
    }
}
