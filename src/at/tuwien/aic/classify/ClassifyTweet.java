package at.tuwien.aic.classify;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.Instances;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.Debug.Random;
import weka.core.DenseInstance;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

/**
 *
 * @author Benjamin Fraller
 */
public class ClassifyTweet {

    /**
     *
     * @param c
     * @param unlabeledData
     */
    public static void classifyTweetArff(Classifier c, String unlabeledData) {
        try {
            Instances unlabeled = new Instances(new BufferedReader(new FileReader(unlabeledData)));
            unlabeled.setClassIndex(unlabeled.numAttributes() - 1);

            Instances labeled = new Instances(unlabeled);
            
            //set options for filter
            String[] options = {"-R first-last", "-W 1000", "-prune-rate -1.0", "-N 0", "-stemmer weka.core.stemmers.NullStemmer", "-M 1"};

            StringToWordVector stringFilter = new weka.filters.unsupervised.attribute.StringToWordVector();
            stringFilter.setOptions(options);
            stringFilter.setInputFormat(unlabeled);

            unlabeled = Filter.useFilter(unlabeled, stringFilter);
            

            // label each instance
            for (int i = 0; i < unlabeled.numInstances(); i++) {
                double clsLabel = c.classifyInstance(unlabeled.instance(i));
                labeled.instance(i).setClassValue(clsLabel);
            }

            // save labeled data
            BufferedWriter writer;
            writer = new BufferedWriter(
                    new FileWriter("resources/labeled.arff"));
            writer.write(labeled.toString());
            writer.newLine();
            writer.flush();
            writer.close();
            
            System.out.println(labeled.toString());
            System.out.println("successfully labeled " + unlabeledData + " -> resources/labeled.arff");
        } catch (Exception ex) {
            Logger.getLogger(ClassifyTweet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static double classifyTweet(Classifier c, String tweet) {
        double clss = -5.0;
        
        try {

            ArrayList<Attribute> atts = new ArrayList<>();
            ArrayList<String> classVal = new ArrayList<>();
            classVal.add("positive");
            classVal.add("negative");

            Attribute attribute1 = new Attribute("Text", (ArrayList<String>) null);
            Attribute attribute2 = new Attribute("Sentiment", classVal);

            atts.add(attribute1);
            atts.add(attribute2);

            Instances data = new Instances("TestInstances", atts, 1);
            data.setClassIndex(data.numAttributes() - 1);

            DenseInstance instance = new DenseInstance(2);
            instance.setValue(attribute1, tweet);
            instance.setValue((Attribute) atts.get(0), tweet);

            data.add(instance);
            System.out.println(data.toString());

            //set options for filter
            String[] options = {"-R first-last", "-W 1000", "-prune-rate -1.0", "-N 0", "-stemmer weka.core.stemmers.NullStemmer", "-M 1"};

            StringToWordVector stringFilter = new weka.filters.unsupervised.attribute.StringToWordVector();
            stringFilter.setOptions(options);
            stringFilter.setInputFormat(data);

            Instances filtered = Filter.useFilter(data, stringFilter);
            clss = c.classifyInstance(filtered.firstInstance());

            System.out.println(data.toString());

            

        } catch (Exception ex) {
            Logger.getLogger(ClassifyTweet.class.getName()).log(Level.SEVERE, null, ex);
        }

        return clss;
    }

    public static void evaluate(Classifier c, String datasetLocation) {
        try {
            DataSource source = new DataSource(datasetLocation);
            Instances data = source.getDataSet();
            data.setClassIndex(data.numAttributes() - 1);

            Evaluation eval = new Evaluation(data);
            

            
            eval.crossValidateModel(c, data, 10, new Random(1));
            
            System.out.println(eval.toSummaryString());

        } catch (Exception ex) {
            Logger.getLogger(ClassifyTweet.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Create classifier model via training data and save it at given location
     *
     * @param datasetLocation training data location
     * @param saveLocation save location for created model
     */
    public static void saveModel(String datasetLocation, String saveLocation) {
        Classifier c = new NaiveBayes();

        try {
            //train
            DataSource source = new DataSource(datasetLocation);
            Instances data = source.getDataSet();
            data.setClassIndex(data.numAttributes() - 1);

            c.buildClassifier(data);

            //serialize
            weka.core.SerializationHelper.write(saveLocation, c);

        } catch (Exception ex) {
            Logger.getLogger(ClassifyTweet.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Load classifier model from given location
     *
     * @param location location of .model file for classifier
     * @return classifier model
     */
    public static Classifier loadModel(String location) {
        Classifier c = null;

        try {
            c = (Classifier) weka.core.SerializationHelper.read(location);
        } catch (Exception ex) {
            Logger.getLogger(ClassifyTweet.class.getName()).log(Level.SEVERE, null, ex);
        }

        return c;
    }

}
