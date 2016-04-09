package me.tomassetti.sketchmodel;

import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import me.tomassetti.sketchmodel.imageprocessing.Filtering;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ModelBuilderConsole {

    private ListDisplayPanel listPanel;

    public ModelBuilderConsole() {
        listPanel = new ListDisplayPanel();
    }

    private ModelBuilder.ImageShower imageShower() {
        return new ModelBuilder.ImageShower() {
            @Override
            public void show(GrayU8 image, String name) {
                System.out.println("Calculated "+name);
            }

            @Override
            public void show(BufferedImage image, String name) {
                System.out.println("Calculated "+name);
            }

            @Override
            public boolean verbose() {
                return false;
            }
        };
    }

    private void showDerivates(GrayU8 input) {
        imageShower().show(Filtering.drawDerivates(input),"Procedural Fixed Type");
    }

    public static void main(String args[]) throws IOException {
        String imageFilename = "images/sm2.png";
        String keypointsSaveDir = "training/SM2/";
        String shapesSaveDir = "training/SM2/";

        ModelBuilderConsole instance = new ModelBuilderConsole();
        ModelBuilder modelBuilder = new ModelBuilder(instance.imageShower());
        instance.imageShower().show(ImageIO.read(new File(imageFilename)), "original");
        instance.showDerivates(UtilImageIO.loadImage(imageFilename, GrayU8.class));
        modelBuilder.run(imageFilename, keypointsSaveDir, shapesSaveDir);
    }
}
