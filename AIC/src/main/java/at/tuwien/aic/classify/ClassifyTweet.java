package at.tuwien.aic.classify;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.Instances;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.stemmers.LovinsStemmer;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.StringToWordVector;

/**
 *
 * @author Benjamin Fraller
 */
public class ClassifyTweet {

    private static final int UNIGRAM = 1;
    private static final int BIGRAM = 2;
    private static final int TRIGRAM = 3;

    public static final List<Configuration> configurations = Arrays.asList(
            //9800 NBM
            new Configuration("train_9800.arff", "NBM_9800_uni.model", UNIGRAM, 1500), //0
            new Configuration("train_9800.arff", "NBM_9800_bi_ranker5000.model", BIGRAM, 5000), //1
            new Configuration("train_9800.arff", "NBM_9800_bi_ranker2500.model", BIGRAM, 2500), //2
            new Configuration("train_9800.arff", "NBM_9800_tri_ranker5000.model", TRIGRAM, 5000), //3
            new Configuration("train_9800.arff", "NBM_9800_tri_ranker2500.model", TRIGRAM, 2500), //4
            //4000 NBM
            new Configuration("train_4000.arff", "NBM_4000_uni_ranker1500.model", UNIGRAM, 1500), //5
            new Configuration("train_4000.arff", "NBM_4000_bi_ranker2500.model", BIGRAM, 2500), //6
            new Configuration("train_4000.arff", "NBM_4000_tri_ranker5000.model", TRIGRAM, 5000), //7
            new Configuration("train_4000.arff", "NBM_4000_tri_ranker2500.model", TRIGRAM, 2500), //8
            new Configuration("train_4000.arff", "NBM_4000_tri_ranker1000.model", TRIGRAM, 1000), //9
            //1000 NBM
            new Configuration("train_1000.arff", "NBM_1000_uni_ranker1000.model", UNIGRAM, 1000), //10
            new Configuration("train_1000.arff", "NBM_1000_bi_ranker2500.model", BIGRAM, 2500), //11
            new Configuration("train_1000.arff", "NBM_1000_tri_ranker2500.model", TRIGRAM, 2500), //12
            //1000 SMO
            new Configuration("train_1000.arff", "SMO_1000_uni_ranker1500.model", UNIGRAM, 1500), //13
            new Configuration("train_1000.arff", "SMO_1000_bi_ranker2500.model", BIGRAM, 2500), //14
            new Configuration("train_1000.arff", "SMO_1000_bi_ranker1500.model", BIGRAM, 1500), //15
            new Configuration("train_1000.arff", "SMO_1000_tri_ranker2500.model", TRIGRAM, 2500), //16
            new Configuration("train_1000.arff", "SMO_1000_tri_ranker1500.model", TRIGRAM, 1500), //17
            //4000 SMO
            new Configuration("train_4000.arff", "SMO_4000_uni_ranker1000.model", UNIGRAM, 1000), //18
            new Configuration("train_4000.arff", "SMO_4000_bi_ranker2500.model", BIGRAM, 2500), //19
            new Configuration("train_4000.arff", "SMO_4000_bi_ranker1500.model", BIGRAM, 1500), //20
            new Configuration("train_4000.arff", "SMO_4000_tri_ranker5000.model", TRIGRAM, 5000), //21
            new Configuration("train_4000.arff", "SMO_4000_tri_ranker2500.model", TRIGRAM, 2500), //22
            //9800 SMO
            new Configuration("train_9800.arff", "SMO_9800_uni_ranker1500.model", UNIGRAM, 1500), //23
            new Configuration("train_9800.arff", "SMO_9800_bi_ranker5000.model", BIGRAM, 5000), //24
            new Configuration("train_9800.arff", "SMO_9800_bi_ranker2500.model", BIGRAM, 2500), //25
            new Configuration("train_9800.arff", "SMO_9800_tri_ranker5000.model", TRIGRAM, 5000), //26
            new Configuration("train_9800.arff", "SMO_9800_tri_ranker2500.model", TRIGRAM, 2500) //27
    );

    /**
     * Classifies a list of tweets
     *
     * @param tweets list of tweets to be classified
     * @param configuration used for trainingdata and filter options
     * @return list containing a number for each tweet -1: negative 0: neutral
     * 1: positive
     */
    public static ArrayList<Integer> classifyTweets(ArrayList<String> tweets, int configuration) {
        try {
            Classifier c = loadModel(configuration);
            Instances test = createArff(tweets);

            //get training data
            Instances train = getInstancesFromARFF(getTrainingDataLocation(configuration));

            //get trainfiltered
            InputStream fis = new FileInputStream("resources/filters/trainfiltered_" + configuration);
            ObjectInputStream oin = new ObjectInputStream(fis);
            Instances trainFiltered = (Instances) oin.readObject();

            fis = new FileInputStream("resources/filters/stwvfilter_" + configuration);
            oin = new ObjectInputStream(fis);
            StringToWordVector filter = (StringToWordVector) oin.readObject();

            Instances testFiltered = Filter.useFilter(test, filter);
            trainFiltered.setClassIndex(0);
            testFiltered.setClassIndex(0);
            
            fis = new FileInputStream("resources/filters/asfilter_" + configuration);
            oin = new ObjectInputStream(fis);
            AttributeSelection attributeSelectionFilter = (AttributeSelection) oin.readObject();
          
            Instances testRanked = Filter.useFilter(testFiltered, attributeSelectionFilter);

            ArrayList<Integer> results = evaluateTestData(c, testRanked);

            return results;

        } catch (Exception ex) {
            Logger.getLogger(ClassifyTweet.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static ArrayList<Integer> saveData(ArrayList<String> tweets, int configuration) {
        try {
            //save filtered traindata and filters

            Classifier c = loadModel(configuration);
            Instances test = createArff(tweets);

            //get training data
            Instances train = getInstancesFromARFF(getTrainingDataLocation(configuration));

            StringToWordVector filter = getSTWVFilter(configuration);
            //calling convention: filter.setInputFormat has to be the last call before the filter is applied
            filter.setInputFormat(train);

            Instances trainFiltered = Filter.useFilter(train, filter);

            //save trainFiltered and the StringToWordVector Object
            OutputStream fos = new FileOutputStream(new File("resources/filters/trainfiltered_" + configuration));
            ObjectOutputStream o = new ObjectOutputStream(fos);
            o.writeObject(trainFiltered);
            o.close();

            fos = new FileOutputStream(new File("resources/filters/stwvfilter_" + configuration));
            o = new ObjectOutputStream(fos);
            o.writeObject(filter);
            o.close();

            Instances testFiltered = Filter.useFilter(test, filter);
            trainFiltered.setClassIndex(0);
            testFiltered.setClassIndex(0);

            AttributeSelection attributeSelectionFilter = getASFilter(configuration);
            attributeSelectionFilter.setInputFormat(trainFiltered);

            Filter.useFilter(trainFiltered, attributeSelectionFilter);
            
            //save AttributeSelection Object
            fos = new FileOutputStream(new File("resources/filters/asfilter_" + configuration));
            o = new ObjectOutputStream(fos);
            o.writeObject(attributeSelectionFilter);
            o.close();
            
            Instances testRanked = Filter.useFilter(testFiltered, attributeSelectionFilter);

            ArrayList<Integer> results = evaluateTestData(c, testRanked);

            return results;
        } catch (Exception ex) {
            Logger.getLogger(ClassifyTweet.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Get StringToWordVector filter with specific options in reference to
     * configuration number. return filter with specific options for given
     * configuration
     */
    private static StringToWordVector getSTWVFilter(int configuration) {
        StringToWordVector filter = new StringToWordVector();

        //filter options
        filter.setWordsToKeep(50000);
        filter.setLowerCaseTokens(true);
        filter.setMinTermFreq(2);
        LovinsStemmer stem = new LovinsStemmer();
        filter.setStemmer(stem);
        filter.setStopwords(new File("resources/stopwords.txt"));
        filter.setTokenizer(getNgramTokenizer(configurations.get(configuration).getNgramCount()));
        return filter;
    }

    /**
     * Create a attribute selection filter
     *
     * @param config number of configuration
     * @return
     */
    private static AttributeSelection getASFilter(int config) {
        AttributeSelection filter = new AttributeSelection();

        InfoGainAttributeEval ev = new InfoGainAttributeEval();
        Ranker ranker = new Ranker();
        ranker.setNumToSelect(configurations.get(config).getRankerWordsToKeep());

        filter.setEvaluator(ev);
        filter.setSearch(ranker);

        return filter;
    }

    /**
     * Get training data from specific configuration
     *
     * @param configuration configuration number
     * @return location of training data
     */
    private static String getTrainingDataLocation(int configuration) {
        String location = "./resources/trainingdata/" + configurations.get(configuration).getTrainingData();
        return location;
    }

    /**
     * Get NGramTokenizer
     *
     * @param maxSize: 1 for unigram, 2 for uni- and bigrams, 3 for trigrams
     * @return NGramTokenizer for STWV filter
     */
    private static NGramTokenizer getNgramTokenizer(int maxSize) {
        NGramTokenizer ngram = new NGramTokenizer();

        ngram.setNGramMinSize(1);
        ngram.setNGramMaxSize(maxSize);

        return ngram;
    }

    /**
     * Creates an ARFF file represented by Instances
     *
     * @param tweets list of tweets return Instances which includes the list of
     * tweets
     */
    private static Instances createArff(ArrayList<String> tweets) {
        ArrayList<Attribute> atts = new ArrayList<>();
        ArrayList<String> classVal = new ArrayList<>();
        classVal.add("pos");
        classVal.add("neg");

        Attribute attribute1 = new Attribute("sentiment", classVal);
        Attribute attribute2 = new Attribute("text", (ArrayList<String>) null);

        atts.add(attribute1);
        atts.add(attribute2);

        //build training data
        Instances data = new Instances("Tweets", atts, 1);
        DenseInstance instance;

        for (String tweet : tweets) {
            instance = new DenseInstance(2);
            instance.setValue((Attribute) atts.get(1), tweet);
            data.add(instance);
        }

        System.out.println("--------------------------------------------------");
        System.out.println("Create ARFF file:");
        System.out.println(data.toString());
        System.out.println("--------------------------------------------------");

        return data;
    }

    /**
     * Get Instances from ARFF file
     *
     * @param fileLocation path to ARFF file
     * @return Instances of given ARFF file
     */
    private static Instances getInstancesFromARFF(String fileLocation) {
        Instances instances = null;

        try {
            DataSource dataSource = new DataSource(fileLocation);
            instances = dataSource.getDataSet();
        } catch (Exception ex) {
            System.out.println("Can't find ARFF file at given location: " + fileLocation);
        }

        return instances;
    }

    /**
     * Evaluate the filtered test data via classifier c
     *
     * @param c used classifier for evaluation
     * @param testFiltered filtered testdata
     * @return result as String
     */
    private static ArrayList<Integer> evaluateTestData(Classifier c, Instances testFiltered) {
        ArrayList<Integer> results = new ArrayList<>();
        String temp = "";

        try {
            for (int i = 0; i < testFiltered.size(); i++) {
                double[] dbl = c.distributionForInstance(testFiltered.get(i));
                if (dbl[0] <= 0.35) {
                    temp = "negative";
                    results.add(-1);
                } else if (dbl[0] >= 0.65) {
                    temp = "positive";
                    results.add(1);
                } else {
                    temp = "neutral";
                    results.add(0);
                }

                temp = temp + " -- positive prob.: " + dbl[0] + " - ";
                temp = temp + "-- negative prob.: " + dbl[1];
                System.out.println(temp);
            }

        } catch (Exception ex) {
            Logger.getLogger(ClassifyTweet.class.getName()).log(Level.SEVERE, null, ex);
        }

        return results;
    }

    /**
     * Load classifier model from given location
     *
     * @param configuration number of configuration
     * @return classifier model for specified configuration
     */
    public static Classifier loadModel(int configuration) {
        Classifier c = null;

        String location = "resources/models/" + configurations.get(configuration).getModelName();

        System.out.println("using model " + location);

        try {
            c = (Classifier) weka.core.SerializationHelper.read(location);
        } catch (Exception ex) {
            Logger.getLogger(ClassifyTweet.class.getName()).log(Level.SEVERE, null, ex);
        }

        return c;
    }
}
