package me.tomassetti.sketchmodel;

import boofcv.gui.ListDisplayPanel;
import boofcv.struct.image.GrayU8;
import com.beust.jcommander.JCommander;
import me.tomassetti.sketchmodel.imageprocessing.Filtering;

import java.awt.image.BufferedImage;
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
        Options options = new Options();
        JCommander commander = new JCommander(options, args);

        if (options.getParameters().size() !=1 ){
            commander.usage();
            System.err.println("One parameter expected");
            System.exit(1);
        }

        String imageFilename = options.getParameters().get(0);

        ModelBuilderConsole instance = new ModelBuilderConsole();
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
        modelBuilder.run(imageFilename, options.getKeypointsSaveDir(), options.getShapesSaveDir(), options.getHighlightedImage());
    }
}
