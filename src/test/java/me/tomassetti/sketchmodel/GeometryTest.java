package me.tomassetti.sketchmodel;

import georegression.struct.point.Point2D_I32;
import org.junit.Test;
import static org.junit.Assert.*;

public class GeometryTest {

    @Test
    public void testAngleBasicPoints() {
        // Basic angles
        assertEquals(Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(200, 100)), 0.0, 0.0001);
        assertEquals(Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(0, 100)),   Math.PI, 0.0001);
        assertEquals(Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(100, 0)), Math.PI/2, 0.0001);
        assertEquals(Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(100, 200)), 3*Math.PI/2, 0.0001);
    }

    @Test
    public void testAngleBisects() {
        // Bisects
        assertEquals(Math.PI/4 * 1, Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(200, 200)),0.0001);
        assertEquals(Math.PI/4 * 3, Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(000, 200)), 0.0001);
        assertEquals(Math.PI/4 * 5, Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(000, 000)), 0.0001);
        assertEquals(Math.PI/4 * 7, Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(200, 000)), 0.0001);
    }

    @Test
    public void testAngleBisectsOfBisects() {
        // Bisects
        int d = 41;
        assertEquals(Math.PI/8 * 1,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(200, 100+d)), 0.015);
        assertEquals(Math.PI/8 * 3,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(100+d, 200)), 0.015);
        assertEquals(Math.PI/8 * 5,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(100-d, 200)), 0.015);
        assertEquals(Math.PI/8 * 7,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(  0, 100+d)), 0.015);
        assertEquals(Math.PI/8 * 9,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(  0, 100-d)), 0.015);
        assertEquals(Math.PI/8 * 11, Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(100-d,   0)), 0.015);
        assertEquals(Math.PI/8 * 13, Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(100+d,   0)), 0.015);
        assertEquals(Math.PI/8 * 15, Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(200,  100-d)), 0.015);
    }

    @Test
    public void testAngleThird() {
        int d = 58;
        // 30 degrees c
        assertEquals(Math.PI/6 * 1,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(200, 100+d)), 0.015);
        // 60 degrees
        assertEquals(Math.PI/6 * 2,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(100+d, 200)), 0.015);
        // 120 degrees
        assertEquals(Math.PI/6 * 4,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(100-d, 200)), 0.015);
        // 150 degrees c
        assertEquals(Math.PI/6 * 5,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(  0, 100+d)), 0.015);
        // 210 degrees c
        assertEquals(Math.PI/6 * 7,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(  0, 100-d)), 0.015);
        // 240 degrees
        assertEquals(Math.PI/6 * 8,  Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(100-d, 0)), 0.015);
        // 300 degrees
        assertEquals(Math.PI/6 * 10, Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(100+d,   0)), 0.015);
        // 330 degrees c
        assertEquals(Math.PI/6 * 11, Geometry.angle(new Point2D_I32(100, 100), new Point2D_I32(200, 100-d)), 0.015);
    }
}
