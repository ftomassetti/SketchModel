package me.tomassetti.sketchmodel.modeling;

/**
 * Created by federico on 06/04/16.
 */
public class RecognizedRectangle {
    private ClassifiedPoint topLeft;
    private ClassifiedPoint topRight;
    private ClassifiedPoint bottomLeft;
    private ClassifiedPoint bottomRight;

    public int getLeft() {
        return Math.min(topLeft.getPoint().x, bottomLeft.getPoint().x);
    }

    public int getRight() {
        return Math.max(topRight.getPoint().x, bottomRight.getPoint().x);
    }

    public int getTop() {
        return Math.min(topLeft.getPoint().y, topRight.getPoint().y);
    }

    public int getBottom() {
        return Math.max(bottomLeft.getPoint().y, bottomRight.getPoint().y);
    }

    public int getWidth() {
        return getRight() - getLeft();
    }

    public int getHeight() {
        return getBottom() - getTop();
    }

    public ClassifiedPoint getTopLeft() {
        return topLeft;
    }

    public ClassifiedPoint getTopRight() {
        return topRight;
    }

    public ClassifiedPoint getBottomLeft() {
        return bottomLeft;
    }

    public ClassifiedPoint getBottomRight() {
        return bottomRight;
    }

    public RecognizedRectangle(ClassifiedPoint topLeft, ClassifiedPoint topRight, ClassifiedPoint bottomRight, ClassifiedPoint bottomLeft) {
        if (topLeft.getPointType() != PointType.CORNER_TOP_LEFT) {
            throw new RuntimeException();
        }
        if (topRight.getPointType() != PointType.CORNER_TOP_RIGHT) {
            throw new RuntimeException();
        }
        if (bottomLeft.getPointType() != PointType.CORNER_BOTTOM_LEFT) {
            throw new RuntimeException();
        }
        if (bottomRight.getPointType() != PointType.CORNER_BOTTOM_RIGHT) {
            throw new RuntimeException();
        }
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
    }
}
