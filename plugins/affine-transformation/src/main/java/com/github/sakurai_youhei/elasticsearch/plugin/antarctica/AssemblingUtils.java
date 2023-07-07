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

import static org.elasticsearch.common.Strings.format;

public class AssemblingUtils {
    private static final RealMatrixFormat realMatrixFormat = new RealMatrixFormat("[", "]", "[", "]", ",", ",");

    public static double[] extractDoubleArray(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("object [null] is not an array");
        }

        IllegalArgumentException e = new IllegalArgumentException(
            format("object [%s] of type [%s] is not an array", o, o.getClass().getName())
        );
        if (o instanceof List<?> list) {
            final double[] array = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof Number num) {
                    array[i] = num.doubleValue();
                } else {
                    throw e;
                }
            }
            return array;
        } else {
            throw e;
        }
    }

    public static float[] extractFloatArray(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("object [null] is not an array");
        }

        IllegalArgumentException e = new IllegalArgumentException(
            format("object [%s] of type [%s] is not an array", o, o.getClass().getName())
        );
        if (o instanceof List<?> list) {
            final float[] array = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof Number num) {
                    array[i] = num.floatValue();
                } else {
                    throw e;
                }
            }
            return array;
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
        return vector.getColumnVector(0).getSubVector(0, vector.getRowDimension() - 1).toArray();
    }

    public static RealMatrix parseTransformationMatrix(String source) throws IllegalArgumentException {
        RealMatrix matrix = realMatrixFormat.parse(source);
        if (matrix == null) {
            throw new IllegalArgumentException("Can't parse source of transformation matrix: " + source);
        } else if (matrix.isSquare() == false) {
            throw new IllegalArgumentException(
                format("Transformation matrix is not square: %dx%d", matrix.getRowDimension(), matrix.getColumnDimension())
            );
        }
        return matrix;
    }

    public static float[] toFloatArray(double[] array) {
        float[] arr = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            arr[i] = (float) array[i];
        }
        return arr;
    }

    public static double[] toDoubleArray(float[] array) {
        double[] arr = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            arr[i] = (double) array[i];
        }
        return arr;
    }
}
