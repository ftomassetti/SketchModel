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

public class ModelBuilderGUI {

    private ListDisplayPanel listPanel;

    public ModelBuilderGUI() {
        listPanel = new ListDisplayPanel();
    }

    private ModelBuilder.ImageShower imageShower() {
        return new ModelBuilder.ImageShower() {
            @Override
            public boolean verbose() {
                return true;
            }

            @Override
            public void show(GrayU8 image, String name) {
                listPanel.addImage(image, name);
            }

            @Override
            public void show(BufferedImage image, String name) {
                listPanel.addImage(image, name);
            }
        };
    }

    private void showDerivates(GrayU8 input) {
        imageShower().show(Filtering.drawDerivates(input),"Procedural Fixed Type");
    }

    public static void main( String args[] ) throws IOException {
        String imageFilename = "images/sm3.png";
        String keypointsSaveDir = "training/SM3/";
        String shapesSaveDir = "training/SM3/";

        ModelBuilderGUI instance = new ModelBuilderGUI();
        ModelBuilder modelBuilder = new ModelBuilder(instance.imageShower());
        ShowImages.showWindow(instance.listPanel, "Shape detector", true);
        instance.imageShower().show(ImageIO.read(new File(imageFilename)), "original");
        instance.showDerivates(UtilImageIO.loadImage(imageFilename, GrayU8.class));
        modelBuilder.run(imageFilename, keypointsSaveDir, shapesSaveDir);
    }
}