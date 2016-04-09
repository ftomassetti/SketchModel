package me.tomassetti.sketchmodel;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.border.FactoryImageBorderAlgs;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import me.tomassetti.sketchmodel.imageprocessing.Utils;
import me.tomassetti.sketchmodel.modeling.ClassifiedPoint;
import me.tomassetti.sketchmodel.modeling.PointType;
import me.tomassetti.sketchmodel.modeling.RecognizedRectangle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static me.tomassetti.sketchmodel.Geometry.intersections;
import static me.tomassetti.sketchmodel.imageprocessing.Drawing.*;
import static me.tomassetti.sketchmodel.imageprocessing.Filtering.exaltColorDifferences;
import static me.tomassetti.sketchmodel.modeling.Recognizer.classifyPoint;
import static me.tomassetti.sketchmodel.modeling.Recognizer.reconstructFigures;

public class ModelBuilder {

    private long startTime;

    public interface ImageShower {
        boolean verbose();
        void show(GrayU8 image, String name);
        void show(BufferedImage image, String name);
    }

    private ImageShower imageShower;
    private boolean saveKeyPoints = false;

    public void setSaveRectangles(boolean saveRectangles) {
        this.saveRectangles = saveRectangles;
    }

    private boolean saveRectangles = false;

    public void setSaveKeyPoints(boolean saveKeyPoints) {
        this.saveKeyPoints = saveKeyPoints;
    }

    public ModelBuilder(ImageShower imageShower) {
        this.imageShower = imageShower;
    }

    private List<List<Point2D_I32>> identifyKeyPoints(BufferedImage image) {
        exaltColorDifferences(image);

        GrayU8 input = ConvertBufferedImage.convertFrom(image,(GrayU8)null);
        GrayU8 binary = new GrayU8(input.width,input.height);

        imageShower.show(input, "Phase I");
        logTime("Phase I");

        input = derivateCleanser(derivateCleanser(input));

        logTime("Phase Ib");

        imageShower.show(input, "Phase Ib");

        // the mean pixel value is often a reasonable threshold when creating a binary image
        double mean = ImageStatistics.mean(input);

        // create a binary image by thresholding
        ThresholdImageOps.threshold(input, binary, (int)mean, true);

        // To avoid the image conversion
        if (imageShower.verbose()) {
            imageShower.show(binaryToDrawable(binary), "Phase II");
        }
        logTime("Phase II");

        // reduce noise with some filtering
        GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
        // To avoid the image conversion
        if (imageShower.verbose()) {
            imageShower.show(binaryToDrawable(filtered), "Phase IIb");
        }
        filtered = BinaryImageOps.dilate8(filtered, 1, null);

        // To avoid the image conversion
        if (imageShower.verbose()) {
            imageShower.show(binaryToDrawable(filtered), "Phase III");
        }
        logTime("Phase III");

        // Find the contour around the shapes
        List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT,null);

        // To avoid the image conversion
        if (imageShower.verbose()) {
            imageShower.show(drawContourns(contours, filtered.getWidth(), filtered.getHeight()), "Contours Pure");
        }

        contours = new LinkedList<>(contours);
        contours.removeIf(c -> Geometry.length(c) < 1000);

        // To avoid the image conversion
        if (imageShower.verbose()) {
            imageShower.show(drawContourns(contours, filtered.getWidth(), filtered.getHeight()), "Contours Filtered");
        }

        BufferedImage image2 = Utils.deepCopy(image);

        double minContourLength = 500;

        // because the existing list does not support some operations
        contours = new LinkedList<>(contours);
        contours.removeIf(c -> Geometry.length(c) < minContourLength);

        List<List<Point2D_I32>> contours2 = new LinkedList<>();
        contours.forEach(c -> {
            contours2.add(c.external);
            contours2.addAll(c.internal);
        } );
        contours2.removeIf(c -> Geometry.length(c) < minContourLength);

        // To avoid the image conversion
        if (imageShower.verbose()) {
            imageShower.show(drawKeyPoints(image2, contours2, false), "Key points (not simplified)");
        }

        logTime("Phase IV");

        simplifyContourns(contours2);

        logTime("Phase V");

        // To avoid the image conversion
        if (imageShower.verbose()) {
            imageShower.show(drawKeyPoints(image2, contours2, false), "Key points");
        }

        return contours2;
    }

    private void mergeCloseConsecutivePoints(List<List<Point2D_I32>> contours, double threshold) {
        // When consecutive points are very close let's merge them
        for (int i=0;i<contours.size();i++) {
            List<Point2D_I32> c = contours.get(i);
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
        }
    }

    // When a segment is way shorter than the segment following and preeceding that segment can be removed.
    // The extreme of that segment are replaced with a single point representing the average of the extremes of the segment
    private void shortSegmentKiller(List<List<Point2D_I32>> contours, float factor) {
        double shortTh = 100;
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

                Point2D_I32 midPoint = new Point2D_I32((pb.x+pc.x)/2, (pb.y+pc.y)/2);
                double anglePrevBefore = Geometry.angle(pa, pb);
                double anglePrevAfter = Geometry.angle(pa, midPoint);
                double angleNextBefore = Geometry.angle(pc, pd);
                double angleNextAfter = Geometry.angle(midPoint, pd);
                double distanceAnglePrev = Geometry.angleDistance(anglePrevBefore, anglePrevAfter);
                double distanceAngleNext = Geometry.angleDistance(angleNextBefore, angleNextAfter);
                double angleDiffTh = 0.18;

                boolean isShort = lengthOfMidSegment < shortTh;
                boolean isShortComparedToSorrounding = lengthOfMidSegment < lengthOfPrevSegment/factor && lengthOfMidSegment < lengthOfNextSegment/factor;
                if ((isShort || isShortComparedToSorrounding)
                        && distanceAnglePrev < angleDiffTh
                        && distanceAngleNext < angleDiffTh) {
                    c.remove(pi);
                    c.add(pi, midPoint);
                    c.remove((pi + 1) % c.size());
                }
            }
        }
    }

    private void mergePointsInLine(List<List<Point2D_I32>> contours, double threshold) {
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
                double d = Geometry.distToSegment(pointJ, pointI, pointZ);
                if (d < threshold) {
                    c.remove(pj);
                    running = false;
                    i--;
                }
            }
        }
    }

    private void simplifyContourns(List<List<Point2D_I32>> contours) {
        double PRACTICALLY_SOME_POINT = 5.0;

        mergeCloseConsecutivePoints(contours, PRACTICALLY_SOME_POINT);
        mergePointsInLine(contours, PRACTICALLY_SOME_POINT/3);
        shortSegmentKiller(contours, 3);
        mergeCloseConsecutivePoints(contours, 20);
        mergePointsInLine(contours, 5);
        shortSegmentKiller(contours, 3);
    }

    private int countAllIn(Map<Point, Boolean> pointsWithStrongDerivates, int minY, int maxY, int minX, int maxX) {
        int total = 0;
        for (int dy = minY; dy < maxY; dy++) {
            for (int dx = minX; dx < maxX; dx++) {
                if (pointsWithStrongDerivates.containsKey(new Point(dx, dy))) {
                    total++;
                }
            }
        }
        return total;
    }

    private GrayU8 removeIsolatedPoints(Map<Point, Boolean> pointsWithStrongDerivates, int width, int height) {
        GrayU8 pointsToKeep = new GrayU8(width, height);
        int exploreRadius = 5;
        int exploreTh = 20;
        for (int y=0; y<height; y++) {
            int minY = Math.max(0, y - exploreRadius);
            int maxY = Math.min(height, y + exploreRadius + 1);
            int minX = 0;
            int maxX = Math.min(width, exploreRadius + 1);

            int total = countAllIn(pointsWithStrongDerivates, minY, maxY, minX, maxX);
            if (total < exploreTh) {
                pointsToKeep.set(0, y, WHITE);
            } else {
                pointsToKeep.set(0, y, BLACK);
            }

            for (int x = 1; x < width; x++) {
                // As we move on the right we should consider one more column on the right and one less on the left
                int newMinX = Math.max(0, x - exploreRadius);
                int newMaxX = Math.min(width, x + exploreRadius + 1);
                if (newMinX != minX) {
                    total -= countAllIn(pointsWithStrongDerivates, minY, maxY, minX, newMinX);
                    minX = newMinX;
                }
                if (newMaxX != maxX) {
                    total += countAllIn(pointsWithStrongDerivates, minY, maxY, maxX, newMaxX);
                    maxX = newMaxX;
                }
                if (total < exploreTh) {
                    pointsToKeep.set(x, y, WHITE);
                } else {
                    pointsToKeep.set(x, y, BLACK);
                }
            }
        }
        return pointsToKeep;
    }

    /**
     * Consider only the points with strong derivates and remove the rest.
     */
    private GrayU8 derivateCleanser( GrayU8 input ) {
        int blurRadius = 5;

        imageShower.show(input,"Derivates Input");

        GrayU8 blurred = new GrayU8(input.width,input.height);
        GrayS16 derivX = new GrayS16(input.width,input.height);
        GrayS16 derivY = new GrayS16(input.width,input.height);

        // Gaussian blur: Convolve a Gaussian kernel
        BlurImageOps.gaussian(input,blurred,-1,blurRadius,null);

        logTime("derivateCleanser a");

        // Calculate image's derivative
        GradientSobel.process(blurred, derivX, derivY, FactoryImageBorderAlgs.extend(input));

        logTime("derivateCleanser b");

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
                }
            }
        }

        logTime("derivateCleanser c");

        // Second: I remove points with strong derivatives if they have not enough other points with strong derivates
        //         near them
        GrayU8 pointsToKeep = removeIsolatedPoints(pointsWithStrongDerivates, input.getWidth(), input.getHeight());

        logTime("derivateCleanser d");
        imageShower.show(pointsToKeep,"Derivates pointsToKeep");

        // display the results
        if (imageShower.verbose()) {
            BufferedImage outputImage = VisualizeImageData.colorizeGradient(derivX, derivY, -1);
            imageShower.show(outputImage, "Derivates");
        }
        imageShower.show(input,"Derivates Cleansed");
        imageShower.show(input,"Derivates Output");

        logTime("derivateCleanser e");

        return pointsToKeep;
    }

    private void saveRectangles(List<RecognizedRectangle> rectangles, BufferedImage originalImage, String path) throws IOException {
        int i = 0;
        for (RecognizedRectangle rectangle : rectangles) {
            BufferedImage img = originalImage.getSubimage(
                    rectangle.getLeft(), rectangle.getTop(), rectangle.getWidth(), rectangle.getHeight());
            ImageIO.write(img, "png", new File(path+"/rectangle"+i+".png"));
            i++;
        }
    }

    private void saveKeyPointsImages(List<List<Point2D_I32>> keyPoints, BufferedImage image, String path) throws IOException {
        List<ClassifiedPoint> classifiedPoints = new LinkedList<>();

        StringBuffer data = new StringBuffer();
        final int around = 200;
        int cindex = 0;
        for (List<Point2D_I32> contour : keyPoints) {
            BufferedImage imageWithOnlyThisContour = drawKeyPoints(image, keyPoints.subList(cindex, cindex+1), true);
            cindex++;
            int pindex = 0;
            for (Point2D_I32 p : contour) {
                pindex++;
                int left = Math.max(0, p.x - around);
                int right = Math.min(image.getWidth(), p.x + around);
                int top = Math.max(0, p.y - around);
                int bottom = Math.min(image.getHeight(), p.y + around);

                BufferedImage keyPointImage = new BufferedImage(around*2+1, around*2+1, BufferedImage.TYPE_3BYTE_BGR);
                Graphics2D g = (Graphics2D) keyPointImage.getGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, around*2+1, around*2+1);
                g.drawImage(imageWithOnlyThisContour.getSubimage(left, top, right-left, bottom-top), around + left - p.x, around + top - p.y, null);

                // highlight the point
                int highlightArea = 7;
                //Graphics2D g = (Graphics2D) portion.getGraphics();
                g.setColor(Color.GREEN);
                drawCircle(around, g, highlightArea);
                drawCircle(around, g, 30);
                drawCircle(around, g, 100);

                String pointName = "point_"+cindex+"_"+pindex;
                ImageIO.write(keyPointImage, "png", new File(path+"/"+pointName+".png"));
            }
        }
    }

    private List<ClassifiedPoint> processKeyPoints(List<List<Point2D_I32>> keyPoints, BufferedImage image, String path) throws IOException {
        List<ClassifiedPoint> classifiedPoints = new LinkedList<>();

        int cindex = 0;
        for (List<Point2D_I32> contour : keyPoints) {
            cindex++;
            int pindex = 0;
            for (Point2D_I32 p : contour) {
                pindex++;

                String pointName = "point_"+cindex+"_"+pindex;
                int nBuckets = 12;
                double anglePortion = (Math.PI*2)/nBuckets;

                List<Point2D> points30 = intersections(contour, p, 30.0);
                int[] buckets30 = new int[nBuckets];
                points30.forEach(i -> {
                    int bucket = (int)(Geometry.angle(p, new Point2D_I32((int)i.getX(),(int)i.getY()))/anglePortion);
                    buckets30[bucket]++;
                });

                List<Point2D> points100 = intersections(contour, p, 100.0);
                int[] buckets100 = new int[nBuckets];
                points100.forEach(i -> {
                    int bucket = (int)(Geometry.angle(p, new Point2D_I32((int)i.getX(),(int)i.getY()))/anglePortion);
                    buckets100[bucket]++;
                });
                PointType res = classifyPoint(buckets30, buckets100);
                if (res != null) {
                    System.out.println("Point "+ pointName + " is "+ res+ " at " + p);
                    classifiedPoints.add(new ClassifiedPoint(pointName, p, res));
                }
            }
        }
        return classifiedPoints;
    }

    private void logTime(String desc) {
        long deltaTime = System.currentTimeMillis() - startTime;
        System.out.println("[Timing] "+desc+ " : "+ deltaTime);
    }

    public void run(String imageFilename, String keypointsSaveDir, String shapesSaveDir) throws IOException {
        startTime = System.currentTimeMillis();
        List<List<Point2D_I32>> keyPoints = identifyKeyPoints(ImageIO.read(new File(imageFilename)));
        logTime("key points identified");

        List<ClassifiedPoint> classifiedPoints = processKeyPoints(keyPoints, ImageIO.read(new File(imageFilename)), keypointsSaveDir);
        logTime("key points processed");
        if (saveKeyPoints) {
            saveKeyPointsImages(keyPoints, ImageIO.read(new File(imageFilename)), keypointsSaveDir);
        }
        List<RecognizedRectangle> rectangles = reconstructFigures(classifiedPoints);
        logTime("Shapes reconstructed");

        // TODO remove me
        saveRectangles = true;
        if (saveRectangles) {
            saveRectangles(rectangles, ImageIO.read(new File(imageFilename)), shapesSaveDir);
        }
        System.out.println("Done.");
    }

}
