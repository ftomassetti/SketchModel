package me.tomassetti.sketchmodel;

import boofcv.alg.filter.binary.Contour;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_I32;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

/**
 * Basic geometry utilities.
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
        if (sgnX < 0) {
            if (sgnY < 0) {
                //angle = Math.PI + angle;
            } else if (sgnY > 0){
                angle = 4*(Math.PI/2) - angle;
            } else {

            }
        } else if (sgnX> 0){
            if (sgnY > 0) {
                angle = Math.PI + angle;
            } else if (sgnY< 0){
                angle = Math.PI - angle;
            } else {
                angle += Math.PI;
            }
        } else {
            if (sgnY > 0) {
                angle += Math.PI;
                angle = Math.PI*2 - angle;
            } else if (sgnY< 0){
                angle += Math.PI;
                angle = Math.PI*2 + angle;
            }
        }
        while (angle > Math.PI*2){
            angle -= Math.PI*2;
        }
        while (angle < 0){
            angle += Math.PI*2;
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

    private static boolean isXBetween(Point2D_I32 pointA, Point2D_I32 pointB, double x) {
        return ((x >= pointA.getX()) && (x <= pointB.getX()))
                || ((x >= pointB.getX()) && (x <= pointA.getX()));
    }

    private static boolean isYBetween(Point2D_I32 pointA, Point2D_I32 pointB, double y) {
        return ((y >= pointA.getY()) && (y <= pointB.getY()))
                || ((y >= pointB.getY()) && (y <= pointA.getY()));
    }

    private static boolean isBetween(Point2D_I32 pointA, Point2D_I32 pointB, Point2D p) {
        return isXBetween(pointA, pointB, p.getX()) && isYBetween(pointA, pointB, p.getY()) ;
    }

    public static void getCircleLineIntersectionPoint(Point2D_I32 pointA,
                                                               Point2D_I32 pointB, Point2D_I32 center, double radius, List<Point2D> list) {
        double baX = pointB.x - pointA.x;
        double baY = pointB.y - pointA.y;
        double caX = center.x - pointA.x;
        double caY = center.y - pointA.y;

        double a = baX * baX + baY * baY;
        double bBy2 = baX * caX + baY * caY;
        double c = caX * caX + caY * caY - radius * radius;

        double pBy2 = bBy2 / a;
        double q = c / a;

        double disc = pBy2 * pBy2 - q;
        if (disc < 0) {
            return;
        }
        // if disc == 0 ... dealt with later
        double tmpSqrt = Math.sqrt(disc);
        double abScalingFactor1 = -pBy2 + tmpSqrt;
        double abScalingFactor2 = -pBy2 - tmpSqrt;

        Point2D p1 = new Point2D.Double(pointA.x - baX * abScalingFactor1, pointA.y
                - baY * abScalingFactor1);
        if (disc == 0) { // abScalingFactor1 == abScalingFactor2
            if (isBetween(pointA, pointB, p1)) {
                list.add(p1);
            }
            return;
        }
        Point2D p2 = new Point2D.Double(pointA.x - baX * abScalingFactor2, pointA.y
                - baY * abScalingFactor2);
        if (isBetween(pointA, pointB, p1)) {
            list.add(p1);
        }
        if (isBetween(pointA, pointB, p2)) {
            list.add(p2);
        }
    }

    public static double sqr(double x) { return x * x; }
    public static double dist2(Point2D_I32 v, Point2D_I32 w) { return sqr(v.x - w.x) + sqr(v.y - w.y); }
    public static double distToSegmentSquared(Point2D_I32 p, Point2D_I32 v, Point2D_I32 w) {
        double l2 = dist2(v, w);
        if (l2 == 0) return dist2(p, v);
        double t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2;
        t = Math.max(0, Math.min(1, t));
        return dist2(p, new Point2D_I32((int)(v.x + t * (w.x - v.x)), (int)(v.y + t * (w.y - v.y))));
    }
    public static double distToSegment(Point2D_I32 p, Point2D_I32 v, Point2D_I32 w) { return Math.sqrt(distToSegmentSquared(p, v, w)); }

    public static List<Point2D> intersections(List<Point2D_I32> contour, Point2D_I32 center, double radius) {
        List<Point2D> points = new LinkedList<>();
        for (int i=0;i<contour.size();i++) {
            Point2D_I32 a = contour.get(i);
            Point2D_I32 b = contour.get((i+1)%contour.size());
            Geometry.getCircleLineIntersectionPoint(a, b, center, radius, points);
        }
        return points;

    }
}
