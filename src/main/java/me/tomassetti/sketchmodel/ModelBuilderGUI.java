package me.tomassetti.sketchmodel;

import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import com.beust.jcommander.JCommander;
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
        Options options = new Options();
        JCommander commander = new JCommander(options, args);

        if (options.getParameters().size() !=1 ){
            commander.usage();
            System.err.println("One parameter expected");
            System.exit(1);
        }

        String imageFilename = options.getParameters().get(0);

        ModelBuilderGUI instance = new ModelBuilderGUI();
        ModelBuilder modelBuilder = new ModelBuilder(instance.imageShower());
        if (options.getKeypointsSaveDir() != null) {
            modelBuilder.setSaveKeyPoints(true);
        }
        if (options.getShapesSaveDir() != null) {
            modelBuilder.setSaveRectangles(true);
        }
        if (options.getHighlightedImage() != null) {
            modelBuilder.setDrawRectanglesOnOriginal(true);
        }

        ShowImages.showWindow(instance.listPanel, "Shape detector", true);
        instance.imageShower().show(ImageIO.read(new File(imageFilename)), "original");
        instance.showDerivates(UtilImageIO.loadImage(imageFilename, GrayU8.class));
        modelBuilder.run(imageFilename, options.getKeypointsSaveDir(), options.getShapesSaveDir(), options.getHighlightedImage());
    }
}
