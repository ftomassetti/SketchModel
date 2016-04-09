package me.tomassetti.sketchmodel;

import com.beust.jcommander.Parameter;

import java.util.LinkedList;
import java.util.List;

class Options {
    public List<String> getParameters() {
        return parameters;
    }

    public String getHighlightedImage() {
        return highlightedImage;
    }

    public String getKeypointsSaveDir() {
        return keypointsSaveDir;
    }

    public String getShapesSaveDir() {
        return shapesSaveDir;
    }

    @Parameter
    private List<String> parameters = new LinkedList<>();

    @Parameter(names = {"--highlight"}, description = "Generate highlighted image")
    private String highlightedImage;

    @Parameter(names = {"--keypoints"}, description = "Generate keypoints images")
    private String keypointsSaveDir;

    @Parameter(names = {"--shapes"}, description = "Generate shapes images")
    private String shapesSaveDir;
}
