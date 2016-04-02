package me.tomassetti.fhp;

import boofcv.abst.denoise.FactoryImageDenoise;
import boofcv.abst.denoise.WaveletDenoiseFilter;
import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.border.FactoryImageBorderAlgs;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by federico on 28/03/16.
 */
public class Main {

    final static int WHITE = 255;
    final static int BLACK = 0;

    // adjusts edge threshold for identifying pixels belonging to a line
    private static final float edgeThreshold = 25;
    // adjust the maximum number of found lines in the image
    private static final int maxLines = 10;

    private static ListDisplayPanel listPanel = new ListDisplayPanel();

    static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    static BufferedImage copyImage(BufferedImage bi) {
        BufferedImage copyOfImage =
                new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = copyOfImage.createGraphics();
        g.drawImage(bi, 0, 0, null);
        return copyOfImage;
    }

    private static GrayU8 removeNoise(GrayU8 binary) {
        // How many levels in wavelet transform
        int numLevels = 8;
        // Create the noise removal algorithm
        WaveletDenoiseFilter<GrayF32> denoiser =
                FactoryImageDenoise.waveletBayes(GrayF32.class,numLevels,0,255);
        // remove noise from the image
        GrayF32 denoised = new GrayF32(binary.width,binary.height);
        GrayF32 binaryF32 = ConvertImage.convert(binary,(GrayF32)null);
        denoiser.process(binaryF32, denoised);
        return ConvertImage.convert(denoised,(GrayU8)null);
    }

    public static GrayU8 histogram(GrayU8 gray) {
        GrayU8 adjusted = gray.createSameShape();

        int histogram[] = new int[256];
        int transform[] = new int[256];


        ImageStatistics.histogram(gray, histogram);
        EnhanceImageOps.equalize(histogram, transform);
        EnhanceImageOps.applyTransform(gray, transform, adjusted);

        EnhanceImageOps.equalizeLocal(gray, 50, adjusted, histogram, transform);
        return adjusted;
    }

    /**
     * When an image is sharpened the intensity of edges are made more extreme while flat regions remain unchanged.
     */
    public static GrayU8 sharpen(GrayU8 gray) {
        GrayU8 adjusted = gray.createSameShape();


        EnhanceImageOps.sharpen4(gray, adjusted);

        EnhanceImageOps.sharpen8(gray, adjusted);
        return adjusted;
    }

    private static GrayU8 binaryToDrawable(GrayU8 image) {
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

    private static List<List<Point2D_I32>> identifyKeyPoints(BufferedImage image) {
        exaltColorDifferences(image);

        GrayU8 input = ConvertBufferedImage.convertFrom(image,(GrayU8)null);
        GrayU8 binary = new GrayU8(input.width,input.height);

        listPanel.addImage(input, "Phase I");

        input = derivateCleanser(derivateCleanser(input));

        listPanel.addImage(input, "Phase Ib");

        // the mean pixel value is often a reasonable threshold when creating a binary image
        double mean = ImageStatistics.mean(input);

        // create a binary image by thresholding
        ThresholdImageOps.threshold(input, binary, (int)mean, true);

        listPanel.addImage(binaryToDrawable(binary), "Phase II");

        //binary = sharpen(binary);

        // reduce noise with some filtering
        GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
        listPanel.addImage(binaryToDrawable(filtered), "Phase IIb");
        filtered = BinaryImageOps.dilate8(filtered, 1, null);


        listPanel.addImage(binaryToDrawable(filtered), "Phase III");

        // Find the contour around the shapes
        List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT,null);

        BufferedImage image2 = deepCopy(image);

        Graphics2D g2 = image.createGraphics();
        g2.setStroke(new BasicStroke(3));
        g2.setColor(Color.RED);

        for( Contour c : contours ) {
            List<PointIndex_I32> polygon = ShapeFittingOps.fitPolygon(c.external,true,0.05,0.01,50);
            VisualizeShapes.drawPolygon(polygon, true,g2);
        }

        listPanel.addImage(image, "Contours");

        // because the existing list does not support some operations
        contours = new LinkedList<>(contours);
        contours.removeIf(c -> Geometry.length(c) < 1500);

        List<List<Point2D_I32>> contours2 = contours.stream().map(c -> c.external).collect(Collectors.<List<Point2D_I32>>toList());
        simplifyContourns(contours2);

        drawKeyPoints(image2, contours2);
        listPanel.addImage(image2, "Key points");

        return contours2;
    }

    private static void drawKeyPoints(BufferedImage image2, List<List<Point2D_I32>> contours2) {
        Graphics2D g3= image2.createGraphics();
        g3.setStroke(new BasicStroke(3));

        int keyPoints = 0;

        g3.setColor(Color.RED);
        for( List<Point2D_I32> c : contours2 ) {
            int i=0;
            for (Point2D_I32 p : c) {
                Point2D_I32 b = c.get((i + 1)%c.size());
                g3.drawLine(p.x, p.y, b.x, b.y);
                keyPoints++;
                i++;
            }
        }
        System.out.println("KEY POINTS DRAWN "+keyPoints);

        g3.setColor(Color.BLUE);
        for( List<Point2D_I32> c : contours2 ) {
            for (Point2D_I32 p : c) {
                g3.fillOval(p.getX()-3, p.getY()-3, 6, 6);
            }
        }
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

    // When a segment is way shorter than the segment following and preeceding that segment can be removed.
    // The extreme of that segment are replaced with a single point representing the average of the extremes of the segment
    private static void shortSegmentKiller(List<List<Point2D_I32>> contours, float factor) {
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

                //System.out.println("lengthOfPrevSegment " + lengthOfPrevSegment);
                //System.out.println("lengthOfMidSegment " + lengthOfMidSegment);
                //System.out.println("lengthOfNextSegment " + lengthOfNextSegment);
                if (lengthOfMidSegment < lengthOfPrevSegment/factor && lengthOfMidSegment < lengthOfNextSegment/factor) {
                    Point2D_I32 midPoint = new Point2D_I32((pa.x+pb.x)/2, (pa.y+pb.y)/2);
                    c.remove(pi);
                    c.add(pi, midPoint);
                    c.remove((pi + 1) % c.size());
                }
                //System.out.println();
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

    private static void simplifyContourns(List<List<Point2D_I32>> contours) {
        double PRACTICALLY_SOME_POINT = 5.0;

        mergeCloseConsecutivePoints(contours, PRACTICALLY_SOME_POINT);

        mergePointsInLine(contours, PRACTICALLY_SOME_POINT/3);
        mergeCloseConsecutivePoints(contours, 20);
        mergePointsInLine(contours, 5);
    }

    public static void exaltColorDifferences(BufferedImage image) {
        int factor = 5;
        for (int y=0;y<image.getHeight();y++) {
            for (int x=0;x<image.getWidth();x++) {
                int color = image.getRGB(x, y);
                int red = (color >> 16) & 255;
                int green = (color >> 8) & 255;
                int blue = (color >> 0) & 255;
                //System.out.println("B red "+ red + " green "+ green + " blue "+blue);
                red = red < 128 ? red/factor : 255-((255-red)/factor);
                green = green < 128 ? green/factor : 255-((255-green)/factor);
                blue = blue < 128 ? blue/factor : 255-((255-blue)/factor);
                color = (red << 16) | (green << 8) | blue;
                //System.out.println("A red "+ red + " green "+ green + " blue "+blue);
                image.setRGB(x, y, color);
            }
        }
    }

    static class Point {
        int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false;

            Point point = (Point) o;

            if (x != point.x) return false;
            return y == point.y;

        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }
    }

    /**
     * Consider only the points with strong derivates and remove the rest.
     */
    private static GrayU8 derivateCleanser( GrayU8 input )
    {
        int blurRadius = 5;

        listPanel.addImage(input,"Derivates Input");
        GrayU8 original = input.clone();

        GrayU8 blurred = new GrayU8(input.width,input.height);
        GrayS16 derivX = new GrayS16(input.width,input.height);
        GrayS16 derivY = new GrayS16(input.width,input.height);

        // Gaussian blur: Convolve a Gaussian kernel
        BlurImageOps.gaussian(input,blurred,-1,blurRadius,null);

        // Calculate image's derivative
        GradientSobel.process(blurred, derivX, derivY, FactoryImageBorderAlgs.extend(input));

        // First I save on a matrix if the point has a strong derivative
        int derivThreshold = 100;
        Map<Point, Boolean> pointsWithStrongDerivates = new HashMap<>();
        for (int y=0; y<input.getHeight(); y++) {
            for (int x=0; x<input.getWidth(); x++) {
                int dx = derivX.get(x, y);
                int dy = derivY.get(x, y);
                int totalDeriv = Math.abs(dx) + Math.abs(dy);
                if (totalDeriv > derivThreshold) {
                    pointsWithStrongDerivates.put(new Point(x, y), true);
                    //input.set(x, y, WHITE);
                }
            }
        }

        GrayU8 pointsToKeep = new GrayU8(input.width,input.height);

        // Second: I remove points with strong derivatives if they have not enough other points with strong derivates
        //         near them
        int exploreRadius = 5;
        int exploreTh = 20;
        for (int y=0; y<input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                //if (pointsWithStrongDerivates.containsKey(new Point(x, y))) {
                    int total = 0;
                    for (int dy = Math.max(0, y - exploreRadius); dy < Math.min(input.getHeight(), y + exploreRadius + 1); dy++) {
                        for (int dx = Math.max(0, x - exploreRadius); dx < Math.min(input.getWidth(), x + exploreRadius + 1); dx++) {
                            if (pointsWithStrongDerivates.containsKey(new Point(dx, dy))) {
                                total++;
                            }
                        }
                    }
                    //System.out.println(total);
                    if (total < exploreTh) {
                        //input.set(x, y, WHITE);
                        pointsToKeep.set(x, y, WHITE);
                    } else {
                        pointsToKeep.set(x, y, BLACK);
                    }
                /*} else {
                    input.set(x, y, WHITE);
                }*/
            }
        }
        listPanel.addImage(pointsToKeep,"Derivates pointsToKeep");

        // display the results
        BufferedImage outputImage = VisualizeImageData.colorizeGradient(derivX, derivY, -1);
        listPanel.addImage(outputImage,"Derivates");
        listPanel.addImage(input,"Derivates Cleansed");
        listPanel.addImage(input,"Derivates Output");

        return pointsToKeep;
    }

    public static void main( String args[] ) throws IOException {
        String filename = "images/state-flowchart.png";

        listPanel.addImage(ImageIO.read(new File(filename)), "original");

        derivates(UtilImageIO.loadImage(filename, GrayU8.class));

        List<List<Point2D_I32>> keyPoints = identifyKeyPoints(ImageIO.read(new File(filename)));
        //saveKeyPoints(keyPoints, ImageIO.read(new File(filename)), "training/SM2/");

        ShowImages.showWindow(listPanel, "Detected Lines", true);
    }

    private static void derivates( GrayU8 input )
    {
        int blurRadius = 3;

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
    }

    private static void saveKeyPoints(List<List<Point2D_I32>> keyPoints, BufferedImage image, String path) throws IOException {
        drawKeyPoints(image, keyPoints);

        final int around = 200;
        int cindex = 0;
        for (List<Point2D_I32> contour : keyPoints) {
            cindex++;
            int pindex = 0;
            for (Point2D_I32 p : contour) {
                pindex++;
                int left = Math.max(0, p.x - around);
                int right = Math.min(image.getWidth(), p.x + around);
                int top = Math.max(0, p.y - around);
                int bottom = Math.min(image.getHeight(), p.y + around);

                BufferedImage portion = copyImage(image.getSubimage(left, top, right-left, bottom-top));

                // highlight the point
                int highlightArea = 7;
                Graphics2D g = (Graphics2D) portion.getGraphics();
                g.setColor(Color.GREEN);
                g.drawOval(p.x - left - highlightArea, p.y - top - highlightArea, highlightArea*2, highlightArea*2);

                ImageIO.write(portion, "png", new File(path+"/point_"+cindex+"_"+pindex+".png"));
            }
        }
    }
}
