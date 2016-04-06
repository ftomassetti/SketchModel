package me.tomassetti.sketchmodel.modeling;

import georegression.struct.point.Point2D_I32;

import java.util.*;

public class Recognizer {

    public static List<RecognizedRectangle> reconstructFigures(List<ClassifiedPoint> classifiedPoints) {
        List<RecognizedRectangle> rectangles = new LinkedList<>();

        // First phase: merge points with same role which are very close
        Map<PointType, List<ClassifiedPoint>> byType = new HashMap<>();
        for (PointType pt : PointType.values()) {
            byType.put(pt, new LinkedList<>());
        }
        double maxDistance = 50;

        for (ClassifiedPoint p : classifiedPoints) {
            byType.get(p.getPointType()).add(p);
        }

        for (PointType pt : PointType.values()) {
            for (int i = 0; i < byType.get(pt).size(); i++) {
                boolean restart = false;
                for (int j = i+1; j < byType.get(pt).size() && !restart; j++) {
                    double distance = byType.get(pt).get(i).getPoint().distance(byType.get(pt).get(j).getPoint());
                    if (distance < maxDistance) {
                        ClassifiedPoint pj = byType.get(pt).remove(j);
                        ClassifiedPoint pi = byType.get(pt).remove(i);
                        System.out.println("MERGING "+pi.getName()+ " "+pj.getName());
                        byType.get(pt).add(i, pi.merge(pj));
                        restart = true;
                        i--;
                    }
                }
            }
        }
        int totalPoints = 0;
        for (PointType pt : PointType.values()) {
            totalPoints += byType.get(pt).size();
        }
        System.out.println("totalPoints "+ totalPoints);

        // Starting from top left find closest bottom right
        // at the right and below the given top left point
        for (ClassifiedPoint topLeft : byType.get(PointType.CORNER_TOP_LEFT)) {
            System.out.println("From Point left  "+ topLeft);
            double minDistance = Double.MAX_VALUE;
            ClassifiedPoint bottomRightSelected = null;
            for (ClassifiedPoint bottomRight : byType.get(PointType.CORNER_BOTTOM_RIGHT)) {
                if (bottomRight.getPoint().getX() > topLeft.getPoint().getX() && bottomRight.getPoint().getY() > topLeft.getPoint().getY()) {
                    double distance = bottomRight.getPoint().distance(topLeft.getPoint());
                    if (distance < minDistance) {
                        minDistance = distance;
                        bottomRightSelected = bottomRight;
                    }
                }
            }
            if (bottomRightSelected != null) {
                System.out.println("Selected bottom right  "+ bottomRightSelected);
                Point2D_I32 expectedTopRight = new Point2D_I32(bottomRightSelected.getPoint().x, topLeft.getPoint().y);
                ClassifiedPoint topRightSelected = findClosestWithin(expectedTopRight, byType.get(PointType.CORNER_TOP_RIGHT), maxDistance * 1.5);
                Point2D_I32 expectedBottomLeft = new Point2D_I32(topLeft.getPoint().x, bottomRightSelected.getPoint().y);
                ClassifiedPoint bottomLeftSelected = findClosestWithin(expectedBottomLeft, byType.get(PointType.CORNER_BOTTOM_LEFT), maxDistance * 1.5);
                if (topRightSelected != null) {
                    System.out.println("Selected top right  "+ topRightSelected);
                }
                if (bottomLeftSelected != null) {
                    System.out.println("Selected bottom left  "+ bottomLeftSelected);
                }
                if (topRightSelected != null && bottomLeftSelected != null) {
                    rectangles.add(new RecognizedRectangle(topLeft, topRightSelected, bottomRightSelected, bottomLeftSelected));
                }
                System.out.println();
            }
        }

        return rectangles;
    }

    private static ClassifiedPoint findClosestWithin(Point2D_I32 ref, List<ClassifiedPoint> classifiedPoints, double maxDistance) {
        double minDistance = Double.MAX_VALUE;
        ClassifiedPoint selected = null;
        for (ClassifiedPoint candidate : classifiedPoints) {
            double distance = candidate.getPoint().distance(ref);
            if (distance < maxDistance && distance < minDistance) {
                minDistance = distance;
                selected = candidate;
            }
        }
        return selected;
    }

    public static PointType classifyPoint(int[] buckets30, int[] buckets100) {
        if (Arrays.equals(new int[]{0,0,0,1,0,0,1,0,0,0,0,0}, buckets30) && Arrays.equals(new int[]{0,0,0,1,0,1,0,0,0,0,0,0}, buckets100)) {
            return PointType.CORNER_TOP_RIGHT;
        }
        if (Arrays.equals(new int[]{0,0,0,1,0,0,1,0,0,0,0,0}, buckets30) && Arrays.equals(new int[]{0,0,0,1,0,0,1,0,0,0,0,0}, buckets100)) {
            return PointType.CORNER_TOP_RIGHT;
        }
        if (Arrays.equals(new int[]{0,0,0,0,0,1,0,0,0,1,0,0}, buckets30) && Arrays.equals(new int[]{0,0,0,0,0,1,0,0,0,1,0,0}, buckets100)) {
            return PointType.CORNER_BOTTOM_RIGHT;
        }
        if (Arrays.equals(new int[]{0,0,0,0,0,0,0,0,1,0,0,1}, buckets30) && Arrays.equals(new int[]{0,0,0,0,0,0,0,0,1,0,0,1}, buckets100)) {
            return PointType.CORNER_BOTTOM_LEFT;
        }
        if (Arrays.equals(new int[]{0,0,0,0,0,0,0,0,0,1,0,1}, buckets30) && Arrays.equals(new int[]{0,0,1,0,0,0,0,0,0,0,0,1}, buckets100)) {
            return PointType.CORNER_TOP_LEFT;
        }
        if (Arrays.equals(new int[]{1,0,0,1,0,0,0,0,0,0,0,0}, buckets30) && Arrays.equals(new int[]{1,0,0,0,0,0,0,0,0,1,0,0}, buckets100)) {
            return PointType.CORNER_BOTTOM_LEFT;
        }
        if (Arrays.equals(new int[]{0,0,0,1,0,0,0,0,0,0,0,1}, buckets30) && Arrays.equals(new int[]{0,0,0,1,0,0,0,0,0,0,0,1}, buckets100)) {
            return PointType.CORNER_TOP_LEFT;
        }
        if (Arrays.equals(new int[]{0,0,0,0,0,0,1,0,0,1,0,0}, buckets30) && Arrays.equals(new int[]{0,0,0,0,0,0,1,0,0,1,0,0}, buckets100)) {
            return PointType.CORNER_BOTTOM_RIGHT;
        }
        if (Arrays.equals(new int[]{0,0,1,0,0,0,1,0,0,0,0,0}, buckets30) && Arrays.equals(new int[]{0,0,1,0,0,1,0,0,0,0,0,0}, buckets100)) {
            return PointType.CORNER_TOP_RIGHT;
        }
        if (Arrays.equals(new int[]{0,0,1,0,0,0,0,0,0,0,0,1}, buckets30) && Arrays.equals(new int[]{0,0,1,0,0,0,0,0,0,0,0,1}, buckets100)) {
            return PointType.CORNER_TOP_LEFT;
        }
        if (Arrays.equals(new int[]{0,0,0,0,0,0,1,0,0,1,0,0}, buckets30) && Arrays.equals(new int[]{0,0,0,0,0,0,1,0,0,1,0,0}, buckets100)) {
            return PointType.CORNER_BOTTOM_RIGHT;
        }
        if (Arrays.equals(new int[]{1,0,0,0,0,0,0,0,0,1,0,0}, buckets30) && Arrays.equals(new int[]{1,0,0,0,0,0,0,0,0,1,0,0}, buckets100)) {
            return PointType.CORNER_BOTTOM_LEFT;
        }
        if (Arrays.equals(new int[]{1,0,0,0,0,0,0,0,1,0,0,0}, buckets30) && Arrays.equals(new int[]{1,0,0,0,0,0,0,0,1,0,0,0}, buckets100)) {
            return PointType.CORNER_BOTTOM_LEFT;
        }
        if (Arrays.equals(new int[]{0,0,1,0,0,0,1,0,0,0,0,0}, buckets30) && Arrays.equals(new int[]{0,0,1,0,0,0,1,0,0,0,0,0}, buckets100)) {
            return PointType.CORNER_TOP_RIGHT;
        }
        if (Arrays.equals(new int[]{0,0,0,0,0,1,0,0,0,1,0,0}, buckets30) && Arrays.equals(new int[]{0,0,0,0,0,1,0,0,1,0,0,0}, buckets100)) {
            return PointType.CORNER_BOTTOM_RIGHT;
        }
        if (Arrays.equals(new int[]{0,0,0,0,0,0,1,0,1,0,0,0}, buckets30) && Arrays.equals(new int[]{0,0,0,0,0,1,0,0,1,0,0,0}, buckets100)) {
            return PointType.CORNER_BOTTOM_RIGHT;
        }
        if (Arrays.equals(new int[]{0,0,1,0,0,0,1,0,0,0,0,0}, buckets30) && Arrays.equals(new int[]{0,0,1,0,0,0,1,0,0,0,0,0}, buckets100)) {
            return PointType.CORNER_TOP_RIGHT;
        }
        if (Arrays.equals(new int[]{1,0,1,0,0,0,0,0,0,0,0,0}, buckets30) && Arrays.equals(new int[]{1,0,1,0,0,0,0,0,0,0,0,0}, buckets100)) {
            return PointType.CORNER_TOP_LEFT;
        }
        if (Arrays.equals(new int[]{0,0,0,0,0,1,0,0,0,1,0,0}, buckets30) && Arrays.equals(new int[]{0,0,1,0,0,1,0,0,0,0,0,0}, buckets100)) {
            return PointType.CORNER_TOP_RIGHT;
        }
        return null;
    }
}
