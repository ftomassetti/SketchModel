package me.tomassetti.fhp;

import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.abst.feature.detect.line.DetectLineSegmentsGridRansac;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.shapes.FitData;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.FactoryImageBorderAlgs;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_I32;
import georegression.struct.point.Point3D_I32;
import georegression.struct.shapes.EllipseRotated_F64;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by federico on 28/03/16.
 */
public class Main {

    // adjusts edge threshold for identifying pixels belonging to a line
    private static final float edgeThreshold = 25;
    // adjust the maximum number of found lines in the image
    private static final int maxLines = 10;

    private static ListDisplayPanel listPanel = new ListDisplayPanel();

    /**
     * Detects lines inside the image using different types of Hough detectors
     *
     * @param image Input image.
     * @param imageType Type of image processed by line detector.
     * @param derivType Type of image derivative.
     */
    public static<T extends ImageGray, D extends ImageGray>
    void detectLines( BufferedImage image ,
                      Class<T> imageType ,
                      Class<D> derivType )
    {
        // convert the line into a single band image
        T input = ConvertBufferedImage.convertFromSingle(image, null, imageType );

        // Comment/uncomment to try a different type of line detector
        DetectLineHoughPolar<T,D> detector = FactoryDetectLineAlgs.houghPolar(
                new ConfigHoughPolar(3, 30, 2, Math.PI / 180,edgeThreshold, maxLines), imageType, derivType);
//		DetectLineHoughFoot<T,D> detector = FactoryDetectLineAlgs.houghFoot(
//				new ConfigHoughFoot(3, 8, 5, edgeThreshold,maxLines), imageType, derivType);
//		DetectLineHoughFootSubimage<T,D> detector = FactoryDetectLineAlgs.houghFootSub(
//				new ConfigHoughFootSubimage(3, 8, 5, edgeThreshold,maxLines, 2, 2), imageType, derivType);

        List<LineParametric2D_F32> found = detector.detect(input);

        // display the results
        ImageLinePanel gui = new ImageLinePanel();
        gui.setBackground(image);
        gui.setLines(found);
        gui.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));

        listPanel.addItem(gui, "Found Lines");
    }

    static float segmentExtremesDistance(LineSegment2D_F32 s1, LineSegment2D_F32 s2) {
        float d1 = s1.getA().distance(s2.getA());
        float d2 = s1.getA().distance(s2.getB());
        float d3 = s1.getB().distance(s2.getA());
        float d4 = s1.getB().distance(s2.getB());
        return Math.min(d1, Math.min(d2, Math.min(d3, d4)));
    }

    static LineSegment2D_F32 findClosest(List<LineSegment2D_F32> segments, int index) {
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

    private static double angleDistance(double angleA, double angleB) {
        return Math.min(Math.abs(angleA - angleB), Math.abs(angleB-angleA));
    }

    private static double angle(Point2D_I32 a, Point2D_I32 b) {
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

    private static double angle(LineSegment2D_F32 s) {
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

    private static double angleDistance(LineSegment2D_F32 s1, LineSegment2D_F32 s2) {
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

    /**
     * Detects segments inside the image
     *
     * @param image Input image.
     * @param imageType Type of image processed by line detector.
     * @param derivType Type of image derivative.
     */
    public static<T extends ImageGray, D extends ImageGray>
    void detectLineSegments( BufferedImage image ,
                             Class<T> imageType ,
                             Class<D> derivType )
    {
        // convert the line into a single band image
        T input = ConvertBufferedImage.convertFromSingle(image, null, imageType );

        // Comment/uncomment to try a different type of line detector
        DetectLineSegmentsGridRansac<T,D> detector = FactoryDetectLineAlgs.lineRansac(40, 30, 2.36, true, imageType, derivType);

        List<LineSegment2D_F32> found = detector.detect(input);

        System.out.println("Number of segments: "+ found.size());

        List<LineSegment2D_F32> orderedSegments = new LinkedList<LineSegment2D_F32>();
        orderedSegments.addAll(found);

        orderedSegments.sort(new Comparator<LineSegment2D_F32>() {
            @Override
            public int compare(LineSegment2D_F32 o1, LineSegment2D_F32 o2) {
                return Float.compare(o1.getLength(), o2.getLength());
            }
        });

        boolean tryAgain = true;
        while (tryAgain) {
            tryAgain = false;
            for (int i = 0; i < orderedSegments.size(); i++) {
                LineSegment2D_F32 segment = orderedSegments.get(i);
                float length = segment.a.distance(segment.b);
                //System.out.println(segment.a + " "+ segment.b + " " + length);
                LineSegment2D_F32 closest = findClosest(orderedSegments, i);
                double distance = segmentExtremesDistance(segment, closest);
                //System.out.println("    " + closest.a + " "+ closest.b + " " + distance);
                if (distance < 3.0) {

                    LineSegment2D_F32 merged = merge(segment, closest);
                    double angleVariation = segment.getLength()>closest.getLength() ? angleDistance(segment, merged) : angleDistance(closest, merged);
                    if (angleVariation < 0.15) {
                        System.out.println("angleVariation " + angleVariation);

                        System.out.println("MERGING " + segment.slopeX() + "/" + segment.slopeY() + "   " + closest.slopeX() + "/" + closest.slopeY());
                        int indexOfOther = orderedSegments.indexOf(closest);
                        //int indexToRemove1 = Math.max(i, indexOfOther);
                        //int indexToRemove2 = Math.min(i, indexOfOther);
                        orderedSegments.remove(segment);
                        orderedSegments.remove(closest);
                        System.out.println(segment.a + " " + segment.b + " " + segment.getLength());
                        System.out.println("   CLOSEST " + closest.a + " " + closest.b + " " + closest.getLength());
                        System.out.println("   MERGED " + merged.a + " " + merged.b + " " + +merged.getLength());
                        orderedSegments.add(merged);
                        tryAgain = true;
                    }
                }
            }
        }

        System.out.println("Number of segments: "+ orderedSegments.size());

        // display the results
        ImageLinePanel gui = new ImageLinePanel();
        gui.setBackground(image);
        gui.setLineSegments(found);
        gui.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));

        listPanel.addItem(gui, "Found Line Segments");


        ImageLinePanel gui2 = new ImageLinePanel();
        gui2.setBackground(image);
        gui2.setLineSegments(orderedSegments);
        gui2.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));
        listPanel.addItem(gui2, "Cleaned Line Segments");
    }

    private static LineSegment2D_F32 merge(LineSegment2D_F32 s1, LineSegment2D_F32 s2) {
        // We need to find the couple of points which are closest
        float d1 = s1.getA().distance(s2.getA());
        float d2 = s1.getA().distance(s2.getB());
        float d3 = s1.getB().distance(s2.getA());
        float d4 = s1.getB().distance(s2.getB());
        float minD = Math.min(d1, Math.min(d2, Math.min(d3, d4)));
        if (minD == d1) {
            return new LineSegment2D_F32(s1.getB(), s2.getB());
        } else if (minD == d2) {
            return new LineSegment2D_F32(s1.getB(), s2.getA());
        } else if (minD == d3) {
            return new LineSegment2D_F32(s1.getA(), s2.getB());
        } else if (minD == d4) {
            return new LineSegment2D_F32(s1.getA(), s2.getA());
        } else {
            throw new RuntimeException();
        }
    }

    private static void alternative() {
        GrayU8 input = UtilImageIO.loadImage("rectangle1.png",GrayU8.class);
        double sigma = -1;
        int blurRadius = 5;
        GrayU8 blurred = new GrayU8(input.getWidth(),input.getHeight());

        BlurImageOps.gaussian(input,blurred,-1,blurRadius,null);


        // display the results
        ImageLinePanel gui = new ImageLinePanel();
        BufferedImage blurredImg = new BufferedImage(blurred.getWidth(), blurred.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        VisualizeBinaryData.renderBinary(blurred, true, blurredImg);
        gui.setBackground(blurredImg);
        gui.setPreferredSize(new Dimension(input.getWidth(),input.getHeight()));
        listPanel.addItem(gui, "Blurred");

    }


    public static
    void filter( GrayU8 input )
    {
        int blurRadius = 10;

        GrayU8 blurred = new GrayU8(input.width,input.height);
        GrayS16 derivX = new GrayS16(input.width,input.height);
        GrayS16 derivY = new GrayS16(input.width,input.height);

        // Gaussian blur: Convolve a Gaussian kernel
        BlurImageOps.gaussian(input,blurred,-1,blurRadius,null);

        // Calculate image's derivative
        GradientSobel.process(blurred, derivX, derivY, FactoryImageBorderAlgs.extend(input));

        // display the results
        BufferedImage outputImage = VisualizeImageData.colorizeGradient(derivX, derivY, -1);
        listPanel.addImage(outputImage,"Procedural Fixed Type");


        BufferedImage image = VisualizeImageData.standard(blurred, null);

        DetectLineSegmentsGridRansac<GrayU8,GrayS16> detector = FactoryDetectLineAlgs.lineRansac(40, 30, 2.36, true, GrayU8.class, GrayS16.class);



        List<LineSegment2D_F32> found = detector.detect(blurred);
        ImageLinePanel gui = new ImageLinePanel();
        gui.setBackground(image);
        gui.setLineSegments(found);
        gui.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));
        listPanel.addItem(gui, "Line Segments on Blurred");


        DetectLineSegmentsGridRansac<GrayS16,GrayS16> detector2 = FactoryDetectLineAlgs.lineRansac(40, 30, 2.36, true, GrayS16.class, GrayS16.class);
        List<LineSegment2D_F32> found2 = detector2.detect(derivX);
        ImageLinePanel gui2 = new ImageLinePanel();
        gui2.setBackground(image);
        gui2.setLineSegments(found2);
        gui2.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));
        listPanel.addItem(gui2, "Line Segments on DerivativeX");
    }

    private static void segmentation( GrayU8 input ) {
        GrayU8 binary = new GrayU8(input.width,input.height);
        GThresholdImageOps.threshold(input, binary, ImageStatistics.mean(input), true);

        BufferedImage outputImage = VisualizeImageData.grayMagnitude(binary, null, 1.0);
        listPanel.addImage(outputImage,"Segmentation");
    }

    private static double length(Contour c) {
        double total = 0.0;
        for (int i=1;i<c.external.size();i++) {
            total += c.external.get(i-1).distance(c.external.get(i));
        }
        return total;
    }

    public static void sharpen(GrayU8 input, BufferedImage image) {
        GrayU8 gray = ConvertBufferedImage.convertFrom(image,(GrayU8)null);
        GrayU8 adjusted = gray.createSameShape();



        EnhanceImageOps.sharpen4(gray, adjusted);
        listPanel.addImage(ConvertBufferedImage.convertTo(adjusted,null),"Sharpen-4");

        EnhanceImageOps.sharpen8(gray, adjusted);
        listPanel.addImage(ConvertBufferedImage.convertTo(adjusted,null),"Sharpen-8");

    }

    private static void controurns(GrayU8 input, BufferedImage image) {

        GrayU8 binary = new GrayU8(input.width,input.height);

        // the mean pixel value is often a reasonable threshold when creating a binary image
        double mean = ImageStatistics.mean(input);

        // create a binary image by thresholding
        ThresholdImageOps.threshold(input, binary, (int)mean, true);

        // reduce noise with some filtering
        GrayU8 filtered = BinaryImageOps.erode8(binary, 3, null);
        filtered = BinaryImageOps.dilate8(filtered, 10, null);

        // Find the contour around the shapes
        List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT,null);

        // Fit an ellipse to each external contour and draw the results
        Graphics2D g2 = image.createGraphics();
        g2.setStroke(new BasicStroke(3));
        g2.setColor(Color.RED);

        for( Contour c : contours ) {
            System.out.println("LENGTH " + length(c));

            g2.setColor(Color.RED);

            // List<Point2D_I32> points, int iterations ,
            //boolean computeError ,
            // FitData<EllipseRotated_F64> outputStorage
            // List<Point2D_I32> sequence,  boolean loop,
            // double splitFraction, double minimumSideFraction, int iterations)

            //VisualizeShapes.drawEllipse(ellipse.shape, g2);
            if (length(c)> 500) {
                List<PointIndex_I32> ellipse = ShapeFittingOps.fitPolygon(c.external,true,0.05,0.01,50);
                VisualizeShapes.drawPolygon(ellipse, true,g2);
            }



        }

//		ShowImages.showWindow(VisualizeBinaryData.renderBinary(filtered, false, null),"Binary",true);
        //ShowImages.showWindow(image,"Ellipses",true);
        listPanel.addImage(image, "Contourns");
    }

    static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private static void contrournsContrasted(BufferedImage image) {

        mycontrast(image);

        GrayU8 input = ConvertBufferedImage.convertFrom(image,(GrayU8)null);


        GrayU8 binary = new GrayU8(input.width,input.height);

        // the mean pixel value is often a reasonable threshold when creating a binary image
        double mean = ImageStatistics.mean(input);

        // create a binary image by thresholding
        ThresholdImageOps.threshold(input, binary, (int)mean, true);

        // reduce noise with some filtering
        GrayU8 filtered = BinaryImageOps.erode8(binary, 3, null);
        filtered = BinaryImageOps.dilate8(filtered, 10, null);

        // Find the contour around the shapes
        List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT,null);

        BufferedImage image2 = deepCopy(image);

        // Fit an ellipse to each external contour and draw the results
        Graphics2D g2 = image.createGraphics();
        g2.setStroke(new BasicStroke(3));
        g2.setColor(Color.RED);

        for( Contour c : contours ) {
            System.out.println("LENGTH " + length(c));

            // List<Point2D_I32> points, int iterations ,
            //boolean computeError ,
            // FitData<EllipseRotated_F64> outputStorage
            // List<Point2D_I32> sequence,  boolean loop,
            // double splitFraction, double minimumSideFraction, int iterations)

            //VisualizeShapes.drawEllipse(ellipse.shape, g2);
            if (length(c)> 500) {
                List<PointIndex_I32> ellipse = ShapeFittingOps.fitPolygon(c.external,true,0.05,0.01,50);
                VisualizeShapes.drawPolygon(ellipse, true,g2);
            }

        }

//		ShowImages.showWindow(VisualizeBinaryData.renderBinary(filtered, false, null),"Binary",true);
        //ShowImages.showWindow(image,"Ellipses",true);
        listPanel.addImage(image, "Contourns contrasted");

        List<List<Point2D_I32>> contours2 = contours.stream().map(c -> c.external).collect(Collectors.<List<Point2D_I32>>toList());
        splitContourns(contours2);

        Graphics2D g3= image2.createGraphics();
        g3.setStroke(new BasicStroke(3));
        g3.setColor(Color.RED);

        System.out.println("contours2 " + contours2.size());
        for( List<Point2D_I32> c : contours2 ) {
            g3.setColor(Color.RED);
            //System.out.println("c.size " + c.size());

            // List<Point2D_I32> points, int iterations ,
            //boolean computeError ,
            // FitData<EllipseRotated_F64> outputStorage
            // List<Point2D_I32> sequence,  boolean loop,
            // double splitFraction, double minimumSideFraction, int iterations)

            //VisualizeShapes.drawEllipse(ellipse.shape, g2);
            //if (c.size()>2 && c.size()<50) {
                List<PointIndex_I32> ellipse = ShapeFittingOps.fitPolygon(c,true,0.05,0.01,50);
                //VisualizeShapes.drawPolygon(ellipse, true,g3);
            //}

        }

        for( List<Point2D_I32> c : contours2 ) {
            for (Point2D_I32 p : c) {
                g3.setColor(Color.BLUE);
                g3.fillOval(p.getX()-1, p.getY()-1, 4, 4);
                //image2.setRGB(p.getX(), p.getY(), 255 >> 16);
            }
        }
        listPanel.addImage(image2, "Contourns contrasted splitted");
    }

    private static void mergeCloseConsecutivePoints(List<List<Point2D_I32>> contours, double threshold) {
        // When consecutive points are very close let's merge them
        for (int i=0;i<contours.size();i++) {
            List<Point2D_I32> c = contours.get(i);
            System.out.println("INITIAL SIZE "+c.size());
            for (int pi=0;pi<c.size() && c.size()>2;pi++){
                Point2D_I32 pointI = c.get(pi);
                Point2D_I32 pointJ = c.get((pi+1) % c.size());
                double d = pointI.distance(pointJ);
                if (d<threshold) {
                    c.remove(pointJ);
                    pi--;
                }
            }
            if (c.size() <= 3) {
                contours.remove(i);
                i--;
            }
            System.out.println("FINAL SIZE "+c.size());
        }
    }

    private static void contournsSplitter(List<List<Point2D_I32>> contours) {
        for (int i = 0; i < contours.size(); i++) {
            List<Point2D_I32> c = contours.get(i);
            for (int pi = 0; pi < c.size(); pi++) {
                Point2D_I32 pa = c.get(pi);
                for (int pj = pi + 2; pj < c.size() + 2; pj++) {
                    int pjnorm = pi % c.size();
                    Point2D_I32 pb = c.get(pjnorm);
                }
            }
        }
    }

    // When a segment is way shorter than the segment following and preeceding that segment can be removed.
    // The extreme of that segment are replaced with a single point representing the average of the extremes of the segment
    private static void shortSegmentKiller(List<List<Point2D_I32>> contours) {
        double factor = 3.5;
        for (int cIndex = 0; cIndex < contours.size(); cIndex++) {
            List<Point2D_I32> c = contours.get(cIndex);
            if (c.size() < 3) {
                continue;
            }
            for (int pi = 0; pi < c.size(); pi++) {
                Point2D_I32 pa = c.get((pi - 1 + c.size()) % c.size());
                Point2D_I32 pb = c.get(pi);
                Point2D_I32 pc = c.get((pi + 1) % c.size());
                Point2D_I32 pd = c.get((pi + 2) % c.size());

                double lengthOfPrevSegment = pa.distance(pb);
                double lengthOfMidSegment = pb.distance(pc);
                double lengthOfNextSegment = pc.distance(pd);

                System.out.println("lengthOfPrevSegment " + lengthOfPrevSegment);
                System.out.println("lengthOfMidSegment " + lengthOfMidSegment);
                System.out.println("lengthOfNextSegment " + lengthOfNextSegment);
                if (lengthOfMidSegment < lengthOfPrevSegment/factor && lengthOfMidSegment < lengthOfNextSegment/factor) {
                    Point2D_I32 midPoint = new Point2D_I32((pa.x+pb.x)/2, (pa.y+pb.y)/2);
                    c.remove(pi);
                    c.add(pi, midPoint);
                    c.remove((pi + 1) % c.size());
                }
                System.out.println();
            }
        }
    }

    private static void mergePointsInLine(List<List<Point2D_I32>> contours, double threshold) {
        // Let's merge the segments when the point in the middle is close to the segment skipping it
        for (int i = 0; i < contours.size(); i++) {
            List<Point2D_I32> c = contours.get(i);
            boolean running = true;
            for (int pi = 0; pi < c.size() && running; pi++) {
                int pj = (pi + 1) % c.size();
                int pz = (pi + 2) % c.size();
                Point2D_I32 pointI = c.get(pi);
                Point2D_I32 pointJ = c.get(pj);
                Point2D_I32 pointZ = c.get(pz);
                double d = MyMath.distToSegment(pointJ, pointI, pointZ);
                if (d < threshold) {
                    c.remove(pj);
                    running = false;
                    i--;
                }
            }
        }
    }

    private static void splitContourns(List<List<Point2D_I32>> contours) {

        // JUST TO TRY
        //contours.remove(1);

        double VERY_CLOSE = 5;
        double PRACTICALLY_SOME_POINT = 5.0;
        double CLOSE = 50;

        System.out.println("PHASE 0 "+contours.size());
        for (List<Point2D_I32> c : contours) {
            System.out.println("* "+c.size());
        }

        mergeCloseConsecutivePoints(contours, PRACTICALLY_SOME_POINT);

        System.out.println("PHASE 2 "+contours.size());
        for (List<Point2D_I32> c : contours) {
            System.out.println("* "+c.size());
        }

        // Merge consecutive segments if the angle stays ~the same
        if (false) {
            for (int i = 0; i < contours.size(); i++) {
                List<Point2D_I32> c = contours.get(i);
                System.out.println("ANGLES INITIAL SIZE " + c.size());
                for (int pi = 0; pi < c.size() && c.size() > 2; pi++) {
                    Point2D_I32 pointI = c.get(pi);
                    Point2D_I32 pointJ = c.get((pi + 1) % c.size());
                    Point2D_I32 pointZ = c.get((pi + 2) % c.size());
                    if (pointI.distance(pointJ) < 500 || pointJ.distance(pointZ) < 500) {
                        double angle1 = angle(pointI, pointJ);
                        double angle2 = angle(pointI, pointZ);
                        double oldDistance = pointI.distance(pointJ) + pointJ.distance(pointZ);
                        double newDistance = pointI.distance(pointZ);
                        //System.out.println("ANGLES "+angle1 + " "+angle2+ " distance "+angleDistance(angle1, angle2));
                        if ((angleDistance(angle1, angle2) < 0.02 && newDistance >= oldDistance * 0.9) || (newDistance >= oldDistance * 0.98) ) {
                            //if (angleDistance(angle1, angle2) < 0.01) {
                                c.remove(pointJ);
                                pi--;
                            /*} else {
                                System.out.println("ANGLES "+angle1 + " "+angle2+ " distance "+angleDistance(angle1, angle2));
                                System.out.println("  POINT I " + pointI);
                                System.out.println("  POINT J " + pointJ);
                                System.out.println("  POINT Z " + pointZ);
                                System.out.println("  oldDistance " + oldDistance);
                                System.out.println("  newDistance " + newDistance);
                                System.out.println("  ratio " + newDistance/oldDistance);
                            }*/
                        }
                    }
                }
                if (c.size() <= 3) {
                    contours.remove(i);
                    i--;
                }
                System.out.println("ANGLES FINAL SIZE " + c.size());
            }
        }

        System.out.println("PHASE 3 "+contours.size());
        for (List<Point2D_I32> c : contours) {
            System.out.println("* "+c.size());
        }

        final double MAX_ANGLE_DISC = 0.5;

        if (true) {
            mergePointsInLine(contours, PRACTICALLY_SOME_POINT/3);
        }

        System.out.println("PHASE 3b "+contours.size());
        for (List<Point2D_I32> c : contours) {
            System.out.println("* "+c.size());
        }

        //contournsSplitter(contours);
        shortSegmentKiller(contours);
        mergeCloseConsecutivePoints(contours, 10);
        mergePointsInLine(contours, 5);

        shortSegmentKiller(contours);
        mergeCloseConsecutivePoints(contours, 15);
        mergePointsInLine(contours, 10);


        System.out.println("PHASE 3c "+contours.size());
        for (List<Point2D_I32> c : contours) {
            System.out.println("* "+c.size());
        }

        if (false) {
            for (int i = 0; i < contours.size(); i++) {
                List<Point2D_I32> c = contours.get(i);
                boolean running = true;
                for (int pi = 0; pi < c.size() && running; pi++) {
                    Point2D_I32 pointI = c.get(pi);
                    for (int pj = pi + 1; pj < c.size() && running; pj++) {
                        Point2D_I32 pointJ = c.get(pj);
                        double d = pointI.distance(pointJ);
                        if (d < CLOSE) {
                            if (pi == pj - 1) {
                                //c.remove(pj);
                                //i--;
                                //running = false;
                            } else {
                                if (pi == 0 || pj == (c.size() - 1)) {
                                    if (pi == 0 && pj != (c.size() - 1)) {
                                        List<Point2D_I32> partA = new LinkedList<>();
                                        partA.add(c.get(c.size() - 1));
                                        //System.out.println("pi="+pi+" pj="+pj+" csize="+c.size());
                                        partA.addAll(c.subList(pj + 1, c.size() - 1));

                                        List<Point2D_I32> partB = new LinkedList<>();
                                        partB.addAll(c.subList(pi, pj + 1));
                                        if (false) {
                                            double angleI = angle(pointI, c.get((pi - 1 + c.size()) % c.size()));
                                            double angleJ = angle(pointJ, c.get((pj + 1) % c.size()));
                                            double oldDistance = pointI.distance(c.get((pi - 1 + c.size()) % c.size())) + pointJ.distance(c.get((pj + 1) % c.size()));
                                            //double oldDistance = pointI.distance(c.get((pi-1+c.size()) % c.size())) + pointJ.distance(c.get((pj+1) % c.size()));
                                            double angleConnection = angle(pointI, pointJ);
                                            if (angleDistance(angleI, angleConnection) < MAX_ANGLE_DISC && angleDistance(angleJ, angleConnection) < MAX_ANGLE_DISC) {
                                                System.out.println("MERGE1 " + d + " pi=" + pi + " pj=" + pj + " csize=" + c.size());
                                                System.out.println("  POINT I " + pointI);
                                                int preI = (pi - 1 + c.size()) % c.size();
                                                System.out.println("    FROM " + c.get(preI) + " (preI=" + preI + ")");

                                                System.out.println("  ANGLE I " + angleI);
                                                System.out.println("  POINT J " + pointJ);
                                                System.out.println("    FROM " + c.get((pj + 1) % c.size()));

                                                System.out.println("  ANGLE J " + angleJ);
                                                System.out.println("  ANGLE DIST " + angleDistance(angleI, angleJ));

                                            /*contours.remove(i);
                                            contours.add(partA);
                                            contours.add(partB);
                                            i--;
                                            running = false;*/
                                            }
                                        }
                                    } else {
                                        List<Point2D_I32> partA = new LinkedList<>();
                                        partA.addAll(c.subList(0, pi + 1));
                                        partA.addAll(c.subList(pj, c.size()));

                                        List<Point2D_I32> partB = new LinkedList<>();
                                        partB.addAll(c.subList(pi, pj + 1));
                                        if (true) {
                                            double angleI = angle(pointI, c.get((pi - 1 + c.size()) % c.size()));
                                            double angleJ = angle(pointJ, c.get((pj + 1) % c.size()));
                                            double angleConnection = angle(pointI, pointJ);
                                            if (angleDistance(angleI, angleConnection) < MAX_ANGLE_DISC && angleDistance(angleJ, angleConnection) < MAX_ANGLE_DISC) {
                                                System.out.println("MERGE2 " + d + " pi=" + pi + " pj=" + pj + " csize=" + c.size());
                                                System.out.println("  POINT I " + pointI);

                                                System.out.println("  ANGLE I " + angleI);
                                                System.out.println("  POINT J " + pointJ);

                                                System.out.println("  ANGLE J " + angleJ);
                                                System.out.println("  ANGLE DIST " + angleDistance(angleI, angleJ));

                                                contours.remove(i);
                                                contours.add(partA);
                                                contours.add(partB);
                                                i--;
                                                running = false;
                                            }
                                        }
                                    }
                                } else {

                                    List<Point2D_I32> partA = new LinkedList<>();
                                    partA.addAll(c.subList(0, pi));
                                    partA.addAll(c.subList(pj + 1, c.size()));

                                    List<Point2D_I32> partB = new LinkedList<>();
                                    partB.addAll(c.subList(pi, pj + 1));
                                    if (false) {
                                        System.out.println("MERGE3 " + d);
                                        // System.out.println("i="+i+" csize="+contours.size());
                                        contours.remove(i);
                                        contours.add(partA);
                                        contours.add(partB);
                                        i--;
                                        running = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println("PHASE 4 "+contours.size());
        for (List<Point2D_I32> c : contours) {
            System.out.println("* "+c.size());
        }

        /*mergeCloseConsecutivePoints(contours, 20.0);

        System.out.println("PHASE 5 "+contours.size());
        for (List<Point2D_I32> c : contours) {
            System.out.println("* "+c.size());
        }*/
    }

    private static void contrasted(BufferedImage image) {
        RescaleOp rescale = new RescaleOp(2.0f, 0.0f, null);
        image = rescale.filter(image, image);
    }

    public static void contrast(BufferedImage image) {
        RescaleOp rescale = new RescaleOp(3.0f, 0.0f, null);
        BufferedImage contrastedImage = rescale.filter(image, null);
        System.out.println("contrastedImage " + contrastedImage);
        System.out.println("width " + contrastedImage.getWidth());
        System.out.println("height " + contrastedImage.getHeight());
        listPanel.addImage(image, "original");
        image = rescale.filter(image, image);
        listPanel.addImage(image, "contrastedImage");
    }

    public static void mycontrast(BufferedImage image) {
        for (int y=0;y<image.getHeight();y++) {
            for (int x=0;x<image.getWidth();x++) {
                int color = image.getRGB(x, y);
                int red = (color >> 16) & 255;
                int green = (color >> 8) & 255;
                int blue = (color >> 0) & 255;
                //System.out.println("B red "+ red + " green "+ green + " blue "+blue);
                red = red < 128 ? red/2 : 255-((255-red)/2);
                green = green < 128 ? green/2 : 255-((255-green)/2);
                blue = blue < 128 ? blue/2 : 255-((255-blue)/2);
                color = (red << 16) | (green << 8) | blue;
                //System.out.println("A red "+ red + " green "+ green + " blue "+blue);
                image.setRGB(x, y, color);
            }
        }
    }

    public static void main( String args[] ) throws IOException {
        String filename = "sm2.png"; // "rectangle1.png"
        //String filename = "rectangle1.png";

        listPanel.addImage(ImageIO.read(new File(filename)), "original");

       /* filter(UtilImageIO.loadImage(filename,GrayU8.class));

        contrast(ImageIO.read(new File(filename)));
        sharpen(UtilImageIO.loadImage(filename,GrayU8.class), ImageIO.read(new File(filename)));

        controurns(UtilImageIO.loadImage(filename,GrayU8.class), ImageIO.read(new File(filename)));*/

        contrournsContrasted(ImageIO.read(new File(filename)));

        /*segmentation(UtilImageIO.loadImage(filename,GrayU8.class));

        BufferedImage input = ImageIO.read(new File(filename));  //UtilImageIO.loadImage(UtilIO.path("sm1.jpg"));

        detectLines(input, GrayU8.class, GrayS16.class);

        // line segment detection is still under development and only works for F32 images right now
        detectLineSegments(input, GrayF32.class, GrayF32.class);*/

        ShowImages.showWindow(listPanel, "Detected Lines", true);
    }
}
