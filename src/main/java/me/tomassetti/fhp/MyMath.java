package me.tomassetti.fhp;

import georegression.struct.point.Point2D_I32;

/**
 * Created by federico on 31/03/16.
 */
public class MyMath {

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


}
