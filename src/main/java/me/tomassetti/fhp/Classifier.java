package me.tomassetti.fhp;

import weka.classifiers.trees.RandomTree;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by federico on 05/04/16.
 */
public class Classifier {

    public static void main(String[] args) throws Exception {

        File fileToClassify = new File("training/SM4/data.csv");

        List<Object> headerFound = new LinkedList<>();
        List<Instance> instancesToClassify = new LinkedList<>();
        List<String>names = new LinkedList<>();
        try (Stream<String> lines = Files.lines(Paths.get(fileToClassify.getPath()), Charset.defaultCharset())) {
            lines.forEachOrdered( l -> {
                if (headerFound.isEmpty()) {
                    headerFound.add(0);
                } else {
                    String[] values = l.split(",");
                    DenseInstance instance = new DenseInstance(25);
                    //instance.setClassIndex(instance.numAttributes() - 1);
                    for (int i=0;i<24;i++) {
                        instance.setValue(i,Double.parseDouble(values[i+1]));
                    }
                    names.add(values[0]);
                    instancesToClassify.add(instance);
                }
            });
        }

        BufferedReader reader = new BufferedReader(
                new FileReader("training/all_data_classified.arff"));
        Instances data = new Instances(reader);
        reader.close();
        // setting class attribute
        data.setClassIndex(data.numAttributes() - 1);

        RandomTree randomTree = new RandomTree();

        randomTree.buildClassifier(data);

        for (Instance myInstance : data) {
            double expected = data.get(data.numAttributes() - 1).classValue();
            double actual = randomTree.classifyInstance(myInstance);
            if (expected != actual) {
                System.out.println("expected " + expected + ", actual " + actual);
                System.out.println(""+myInstance);
            }
        }

        int i=0;
        for (Instance myInstance : instancesToClassify) {
            myInstance.setDataset(data);
            double actual = randomTree.classifyInstance(myInstance);
            if (actual!=-1) {
                System.out.println("name "+names.get(i));
                System.out.println("  myInstance " + myInstance + ", actual " + actual);
            }
            i++;
        }

    }
}
