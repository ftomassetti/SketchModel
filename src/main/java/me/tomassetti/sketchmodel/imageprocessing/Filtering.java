package me.tomassetti.sketchmodel.imageprocessing;


import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.core.image.border.FactoryImageBorderAlgs;
import boofcv.gui.image.VisualizeImageData;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

import java.awt.image.BufferedImage;

public class Filtering {

    public static BufferedImage drawDerivates(GrayU8 input) {
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
        return outputImage;
    }

    public static void exaltColorDifferences(BufferedImage image) {
        int factor = 5;
        for (int y=0;y<image.getHeight();y++) {
            for (int x=0;x<image.getWidth();x++) {
                int color = image.getRGB(x, y);
                int red = (color >> 16) & 255;
                int green = (color >> 8) & 255;
                int blue = (color >> 0) & 255;
                red = red < 128 ? red/factor : 255-((255-red)/factor);
                green = green < 128 ? green/factor : 255-((255-green)/factor);
                blue = blue < 128 ? blue/factor : 255-((255-blue)/factor);
                color = (red << 16) | (green << 8) | blue;
                image.setRGB(x, y, color);
            }
        }
    }
}
