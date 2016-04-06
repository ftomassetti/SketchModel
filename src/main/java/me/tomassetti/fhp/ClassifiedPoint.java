package me.tomassetti.fhp;

import georegression.struct.point.Point2D_I32;

public class ClassifiedPoint {
    private Point2D_I32 point;
    private String name;

    @Override
    public String toString() {
        return "ClassifiedPoint{" +
                "point=" + point +
                ", name='" + name + '\'' +
                ", pointType=" + pointType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassifiedPoint)) return false;

        ClassifiedPoint that = (ClassifiedPoint) o;

        if (!point.equals(that.point)) return false;
        if (!name.equals(that.name)) return false;
        return pointType == that.pointType;

    }

    @Override
    public int hashCode() {
        int result = point.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + pointType.hashCode();
        return result;
    }

    public String getName() {
        return name;
    }

    public ClassifiedPoint(String name, Point2D_I32 point, PointType pointType) {
        this.name = name;
        this.point = point;
        this.pointType = pointType;
    }

    private PointType pointType;

    public Point2D_I32 getPoint() {
        return point;
    }

    public PointType getPointType() {
        return pointType;
    }

    public ClassifiedPoint merge(ClassifiedPoint other) {
        return new ClassifiedPoint(this.getName()+"_merged_"+other.getName(),
                new Point2D_I32((this.getPoint().x+other.getPoint().x)/2, (this.getPoint().y+other.getPoint().y)/2),
                pointType);
    }
}
