package me.tomassetti.fhp;

import boofcv.alg.filter.binary.Contour;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * Created by federico on 01/04/16.
 */
public class Geometry {

    public static double length(List<Point2D_I32> c) {
        double total = 0.0;
        for (int i=1;i<c.size();i++) {
            total += c.get(i).distance(c.get((i+1) % c.size()));
        }
        return total;
    }

    public static double length(Contour c) {
        double total = 0.0;
        for (int i=0;i<c.external.size();i++) {
            total += c.external.get(i).distance(c.external.get((i+1) % c.external.size()));
        }
        return total;
    }

    public static float segmentExtremesDistance(LineSegment2D_F32 s1, LineSegment2D_F32 s2) {
        float d1 = s1.getA().distance(s2.getA());
        float d2 = s1.getA().distance(s2.getB());
        float d3 = s1.getB().distance(s2.getA());
        float d4 = s1.getB().distance(s2.getB());
        return Math.min(d1, Math.min(d2, Math.min(d3, d4)));
    }

    public static LineSegment2D_F32 findClosest(List<LineSegment2D_F32> segments, int index) {
        LineSegment2D_F32 me = segments.get(index);
        LineSegment2D_F32 minSegment = null;
        float minDistance = Float.MAX_VALUE;
        for (int i=0;i<segments.size();i++) {
            if (i != index) {
                LineSegment2D_F32 other = segments.get(i);
                float d = segmentExtremesDistance(me, other);
                if (minSegment == null || d < minDistance) {
                    minSegment = other;
                    minDistance = d;
                }
            }
        }
        return minSegment;
    }

    public static double angleDistance(double angleA, double angleB) {
        return Math.min(Math.abs(angleA - angleB), Math.abs(angleB-angleA));
    }

    public static double angle(Point2D_I32 a, Point2D_I32 b) {
        float dx = Math.abs(a.getX() - b.getX());
        float dy = Math.abs(a.getY() - b.getY());
        float sgnX = Math.signum(a.getX() - b.getX());
        float sgnY = Math.signum(a.getY() - b.getY());

        double angle = Math.atan(dy/dx);
        angle += Math.PI;
        if (sgnX< 0) {
            if (sgnY < 0) {
                angle = Math.PI + angle;
            } else {
                angle = Math.PI - angle;
            }
        } else {
            if (sgnY < 0) {
                angle = Math.PI*2 - angle;
            } else {
                // nothing to do
            }
        }
        return angle;
    }

    public static double angle(LineSegment2D_F32 s) {
        float dx = Math.abs(s.getA().getX() - s.getB().getX());
        float dy = Math.abs(s.getA().getY() - s.getB().getY());
        float sgnX = Math.signum(s.getA().getX() - s.getB().getX());
        float sgnY = Math.signum(s.getA().getY() - s.getB().getY());

        double angle = Math.asin(dy/s.getLength());
        if (sgnX< 0) {
            if (sgnY < 0) {
                angle = Math.PI + angle;
            } else {
                angle = Math.PI - angle;
            }
        } else {
            if (sgnY < 0) {
                angle = Math.PI*2 - angle;
            } else {
                // nothing to do
            }
        }
        return angle;
    }

    public static double angleDistance(LineSegment2D_F32 s1, LineSegment2D_F32 s2) {
        double a1 = angle(s1);
        if (a1>Math.PI) {
            a1 -= Math.PI;
        }
        double a2 = angle(s2);
        if (a2>Math.PI) {
            a2 -= Math.PI;
        }
        return Math.min(Math.abs(a1-a2), Math.min(Math.abs(a1-a2+Math.PI*2),  Math.abs(a1-a2-Math.PI*2)));
    }

}
