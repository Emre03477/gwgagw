package com.anticheat.util;

/**
 * Mathematical utility methods for anti-cheat calculations.
 */
public final class MathUtil {

    private MathUtil() {}

    /**
     * Calculate the mean (average) of a double array.
     */
    public static double mean(double[] values) {
        if (values.length == 0) return 0;
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    /**
     * Calculate the variance of a double array.
     */
    public static double variance(double[] values) {
        if (values.length < 2) return 0;
        double mean = mean(values);
        double sumSq = 0;
        for (double v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return sumSq / values.length;
    }

    /**
     * Calculate the standard deviation of a double array.
     */
    public static double stdDev(double[] values) {
        return Math.sqrt(variance(values));
    }

    /** Epsilon used as termination threshold in the GCD Euclidean loop. */
    public static final double GCD_EPSILON = 1.0E-6;

    /**
     * Calculate the Greatest Common Divisor of two doubles.
     * Used for GCD flaw detection in aim assist modules.
     */
    public static double gcd(double a, double b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b > GCD_EPSILON) {
            double t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    /**
     * Clamp a value between min and max.
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Calculate the angle between two vectors in degrees.
     */
    public static double angleBetween(double x1, double y1, double z1,
                                       double x2, double y2, double z2) {
        double dot = x1 * x2 + y1 * y2 + z1 * z2;
        double mag1 = Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
        double mag2 = Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2);
        if (mag1 == 0 || mag2 == 0) return 0;
        double cosAngle = clamp(dot / (mag1 * mag2), -1.0, 1.0);
        return Math.toDegrees(Math.acos(cosAngle));
    }
}
