package me.tomassetti.sketchmodel.imageprocessing;

import boofcv.alg.filter.binary.Contour;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Drawing {

    public final static int WHITE = 255;
    public final static int BLACK = 0;

    public static GrayU8 binaryToDrawable(GrayU8 image) {
        GrayU8 drawable = new GrayU8(image.width,image.height);
        for (int y=0;y<image.getHeight();y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.get(x, y) > 0) {
                    drawable.set(x, y, BLACK);
                } else {
                    drawable.set(x, y, WHITE);
                }
            }
        }
        return drawable;
    }

    public static BufferedImage drawContourns(List<Contour> contours, int width, int height) {
        BufferedImage imageCountoursPure = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D gCountoursPure = imageCountoursPure.createGraphics();
        gCountoursPure.setStroke(new BasicStroke(3));
        for( Contour c : contours ) {
            int i=0;
            gCountoursPure.setColor(Color.RED);
            for (Point2D_I32 p : c.external) {
                Point2D_I32 b = c.external.get((i + 1)%c.external.size());
                gCountoursPure.drawLine(p.x, p.y, b.x, b.y);
                i++;
            }

            gCountoursPure.setColor(Color.BLUE);
            for (List<Point2D_I32> anInternalC : c.internal) {
                i=0;
                for (Point2D_I32 p : anInternalC) {
                    Point2D_I32 b = anInternalC.get((i + 1) % anInternalC.size());
                    gCountoursPure.drawLine(p.x, p.y, b.x, b.y);
                    i++;
                }
            }
        }
        return imageCountoursPure;
    }

    public static BufferedImage drawKeyPoints(BufferedImage image2, List<List<Point2D_I32>> contours2, boolean copyingimage) {
        BufferedImage result = new BufferedImage(image2.getWidth(), image2.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g3= result.createGraphics();
        if (copyingimage) {
            g3.drawImage(image2,0,0,null);
        }
        g3.setStroke(new BasicStroke(3));

        g3.setColor(Color.RED);
        for( List<Point2D_I32> c : contours2 ) {
            int i=0;
            for (Point2D_I32 p : c) {
                Point2D_I32 b = c.get((i + 1)%c.size());
                g3.drawLine(p.x, p.y, b.x, b.y);
                i++;
            }
        }

        g3.setColor(Color.BLUE);
        for( List<Point2D_I32> c : contours2 ) {
            for (Point2D_I32 p : c) {
                g3.fillOval(p.getX()-3, p.getY()-3, 6, 6);
            }
        }
        return result;
    }

    public static void drawCircle(int around, Graphics2D g, int highlightArea) {
        g.drawOval(around - highlightArea, around - highlightArea, highlightArea*2, highlightArea*2);
    }

}
