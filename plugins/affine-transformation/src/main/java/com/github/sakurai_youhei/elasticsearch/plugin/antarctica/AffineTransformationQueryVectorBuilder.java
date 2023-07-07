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

import static com.github.sakurai_youhei.elasticsearch.plugin.antarctica.AssemblingUtils.toDoubleArray;
import static com.github.sakurai_youhei.elasticsearch.plugin.antarctica.AssemblingUtils.toFloatArray;
import static java.util.Objects.requireNonNull;
import static org.elasticsearch.common.Strings.format;
import static org.elasticsearch.xcontent.ConstructingObjectParser.constructorArg;
import static org.elasticsearch.xcontent.ConstructingObjectParser.optionalConstructorArg;

public class AffineTransformationQueryVectorBuilder implements QueryVectorBuilder {
    private static final Logger logger = LogManager.getLogger(AffineTransformationQueryVectorBuilder.class);

    public static final String NAME = "affine_transformation";
    public static final ParseField QUERY_VECTOR_FIELD = new ParseField("query_vector");
    public static final ParseField QUERY_VECTOR_BUILDER_FIELD = new ParseField("query_vector_builder");
    public static final ParseField TRANSFORMATION_MATRIX_FIELD = new ParseField("transformation_matrix");
    public static final ConstructingObjectParser<AffineTransformationQueryVectorBuilder, Void> PARSER = new ConstructingObjectParser<>(
        NAME,
        args -> {
            final float[] vector = args[0] != null ? AssemblingUtils.extractFloatArray(args[0]) : null;
            return new AffineTransformationQueryVectorBuilder(vector, (QueryVectorBuilder) args[1], (String) args[2]);
        }
    );
    static {
        PARSER.declareFloatArray(optionalConstructorArg(), QUERY_VECTOR_FIELD);
        PARSER.declareNamedObject(
            optionalConstructorArg(),
            (p, c, n) -> p.namedObject(QueryVectorBuilder.class, n, c),
            QUERY_VECTOR_BUILDER_FIELD
        );
        PARSER.declareString(constructorArg(), TRANSFORMATION_MATRIX_FIELD);
    }

    public static AffineTransformationQueryVectorBuilder fromXContent(XContentParser parser) throws IOException {
        logger.trace("Called: fromXContent(parser=[{}])", parser);

        return PARSER.parse(parser, null);
    }

    final float[] queryVector;
    final QueryVectorBuilder queryVectorBuilder;
    final String transformationMatrix;

    private AffineTransformationQueryVectorBuilder(
        float[] queryVector,
        QueryVectorBuilder queryVectorBuilder,
        String transformationMatrix
    ) {
        logger.trace(
            "Called: AffineTransformationQueryVectorBuilder(queryVector=[{}], queryVectorBuilder=[{}], transformationMatrix=[{}])",
            queryVector,
            queryVectorBuilder,
            transformationMatrix
        );

        if (queryVector == null && queryVectorBuilder == null) {
            throw new IllegalArgumentException(
                format(
                    "either [%s] or [%s] must be provided",
                    QUERY_VECTOR_BUILDER_FIELD.getPreferredName(),
                    QUERY_VECTOR_FIELD.getPreferredName()
                )
            );
        }

        this.queryVector = queryVector == null ? new float[0] : queryVector;
        this.queryVectorBuilder = queryVectorBuilder;
        this.transformationMatrix = requireNonNull(transformationMatrix, format("[%s] cannot be null", TRANSFORMATION_MATRIX_FIELD));
    }

    public AffineTransformationQueryVectorBuilder(StreamInput in) throws IOException {
        logger.trace("Called: AffineTransformationQueryVectorBuilder(in=[{}])", in);

        this.queryVector = in.readFloatArray();
        if (in.getTransportVersion().onOrAfter(TransportVersion.V_8_7_0)) {
            this.queryVectorBuilder = in.readOptionalNamedWriteable(QueryVectorBuilder.class);
        } else {
            this.queryVectorBuilder = null;
        }
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
        logger.trace("Called: StreamOutput(out=[{}])", out);

        out.writeFloatArray(queryVector);
        if (out.getTransportVersion().before(TransportVersion.V_8_7_0) && queryVectorBuilder != null) {
            throw new IllegalArgumentException(
                format(
                    "cannot serialize [%s] to older node of version [%s]",
                    QUERY_VECTOR_BUILDER_FIELD.getPreferredName(),
                    out.getTransportVersion()
                )
            );
        }
        if (out.getTransportVersion().onOrAfter(TransportVersion.V_8_7_0)) {
            out.writeOptionalNamedWriteable(queryVectorBuilder);
        }
        out.writeString(transformationMatrix);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        logger.trace("Called: toXContent(builder=[{}], params=[{}])", builder, params);

        if (queryVectorBuilder != null) {
            builder.startObject(QUERY_VECTOR_BUILDER_FIELD.getPreferredName());
            builder.field(queryVectorBuilder.getWriteableName(), queryVectorBuilder);
            builder.endObject();
        } else {
            builder.array(QUERY_VECTOR_FIELD.getPreferredName(), queryVector);
        }
        builder.field(TRANSFORMATION_MATRIX_FIELD.getPreferredName(), transformationMatrix);
        return builder;
    }

    @Override
    public void buildVector(Client client, ActionListener<float[]> listener) {
        logger.trace("Called: buildVector(client=[{}], listener=[{}])", client, listener);

        if (queryVectorBuilder == null) {
            transformVector(queryVector, listener);
        } else {
            queryVectorBuilder.buildVector(client, new ActionListener<float[]>() {
                @Override
                public void onResponse(float[] v) {
                    if (v == null) {
                        listener.onFailure(
                            new IllegalArgumentException(
                                format(
                                    "[%s] with name [%s] returned null query_vector",
                                    QUERY_VECTOR_BUILDER_FIELD.getPreferredName(),
                                    queryVectorBuilder.getWriteableName()
                                )
                            )
                        );
                    } else {
                        transformVector(v, listener);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        }
    }

    private void transformVector(float[] v, ActionListener<float[]> listener) {
        logger.trace("Called: transformVector(v=[{}], listener=[{}])", v, listener);

        try {
            RealMatrix vector = AssemblingUtils.argumentVector(toDoubleArray(v));
            RealMatrix matrix = AssemblingUtils.parseTransformationMatrix(transformationMatrix);
            logger.trace("Affine transformation: [{}] x [{}]", matrix, vector);

            float[] transformedQueryVector = toFloatArray(AssemblingUtils.unargumentVector(matrix.multiply(vector)));
            logger.trace("Transformed: [{}] to [{}]", v, transformedQueryVector);
            listener.onResponse(transformedQueryVector);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        logger.trace("Called: equals(o=[{}])", o);

        if (o == null) {
            return false;
        } else if (o instanceof AffineTransformationQueryVectorBuilder obj) {
            return Objects.equals(queryVector, obj.queryVector)
                && Objects.equals(queryVectorBuilder, obj.queryVectorBuilder)
                && Objects.equals(transformationMatrix, obj.transformationMatrix);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        logger.trace("Called: hashCode()");

        return Objects.hash(this.getClass(), Objects.hash(queryVector, queryVectorBuilder, transformationMatrix));
    }
}
