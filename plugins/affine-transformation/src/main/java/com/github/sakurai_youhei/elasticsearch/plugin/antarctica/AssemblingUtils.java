/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package com.github.sakurai_youhei.elasticsearch.plugin.antarctica;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixFormat;

import java.util.Arrays;
import java.util.List;

public class AssemblingUtils {
    private static final RealMatrixFormat realMatrixFormat = new RealMatrixFormat("[", "]", "[", "]", ",", ",");

    public static double[] extractVector(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("object [null] is not a vector");
        }

        IllegalArgumentException e = new IllegalArgumentException(
            "object [" + o + "] of type [" + o.getClass().getName() + "] is not a vector"
        );
        if (o instanceof List<?> list) {
            final double[] vector = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof Number num) {
                    vector[i] = num.doubleValue();
                } else {
                    throw e;
                }
            }
            return vector;
        } else {
            throw e;
        }
    }

    public static RealMatrix argumentVector(double[] vecotr) {
        double[] argumentedVector = Arrays.copyOf(vecotr, vecotr.length + 1);
        argumentedVector[vecotr.length] = 1;
        return MatrixUtils.createRealMatrix(new double[][] { argumentedVector }).transpose();
    }

    public static double[] unargumentVector(RealMatrix vector) {
        final int N = vector.getRowDimension() - 1;
        return vector.getColumnVector(0).getSubVector(0, N).toArray();
    }

    public static float[] unargumentVectorFloat(RealMatrix vector) {
        double[] unargumentedVector = unargumentVector(vector);
        float[] unargumentedVectorFloat = new float[unargumentedVector.length];
        for (int i = 0; i < unargumentedVector.length; i++) {
            unargumentedVectorFloat[i] = (float) unargumentedVector[i];
        }
        return unargumentedVectorFloat;
    }

    public static RealMatrix parseTransformationMatrix(String source) throws IllegalArgumentException {
        RealMatrix matrix = realMatrixFormat.parse(source);
        if (matrix == null) {
            throw new IllegalArgumentException("Can't parse source of transformation matrix: " + source);
        } else if (matrix.isSquare() == false) {
            throw new IllegalArgumentException(
                "Transformation matrix is not square: " + matrix.getRowDimension() + "x" + matrix.getColumnDimension()
            );
        }
        return matrix;
    }
}
