/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.tuwien.aic.classify;

/**
 *
 * @author Benjamin Fraller
 */
public class Configuration {

    private final String trainingData;
    private final String modelName;
    private final int ngramCount;
    private final int rankerWordsToKeep;

    public Configuration(String trainingData, String modelName, int ngramCount, int rankerWordsToKeep) {
        this.modelName = modelName;
        this.trainingData = trainingData;
        this.ngramCount = ngramCount;
        this.rankerWordsToKeep = rankerWordsToKeep;
    }

    public String getModelName() {
        return modelName;
    }
    public String getTrainingData() {
        return trainingData;
    }

    public int getNgramCount() {
        return ngramCount;
    }

    public int getRankerWordsToKeep() {
        return rankerWordsToKeep;
    }

}
