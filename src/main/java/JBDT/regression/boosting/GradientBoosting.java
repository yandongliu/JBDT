package JBDT.regression.boosting;

import JBDT.classifier.tree.SplitParam;
import JBDT.classifier.tree.Tree;
import JBDT.data.Data;
import JBDT.data.SampleData;

import java.util.Arrays;

public class GradientBoosting {

    void printArray(double[] a) {
        for (int i = 0; i < a.length; i++) {
            System.out.println("gradient:"+i+":"+a[i]);
        }
    }

    void setRegulator(Data data, double lamL1, double lamL2) {
        double ws = data.getWeightSum();
        SplitParam.lamL1 = lamL1*ws;
        SplitParam.lamL2 = lamL2*ws;
    }

    public void gbdt(Data data, QLoss loss, TreeNet tn) {
        double[] prediction = new double[data._nrows];
        double[] score = new double[data._nrows];
        double[] gradient = new double[data._nrows];
        double shrinkage = loss.shrinkage;
        int[] index = new int[data._nrows];

        System.out.println("\nGradient Boosting:\n\tTree #\t"+loss.get_progress_header()+"\tTime\tETF\tMemory");

        SampleData sd = new SampleData(data, index, gradient, score);
        setRegulator(data, loss.lamL1, loss.lamL2);

        for (int i = 0; i < loss.nTrees; i++) {
            loss.compGradient(data, prediction, gradient);
            Arrays.fill(score, 0.0);
            sd.sampleData(loss.samplingRate);
            sd.sampleFeatures(loss.featureSamplingRate);
            //compute residual
            sd.calcTotal();

            Tree tree = new Tree();
            tree.grow(loss.nnodes, sd);

            double stepsize = loss.compStepsize(gradient, score, data._nrows);
            shrinkage *= stepsize;
            tn.add(tree, shrinkage);

            for (int j = 0; j < data._nrows; j++) {
                prediction[j]+= (score[j]*shrinkage);
            }

            double watch = 0.0;
            long memused = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            memused = (long)(memused/1000.0);
            System.out.println("\t" + (i + 1) + "\t" + loss.get_progress_content() + "\t" + watch + "\t" + (loss.nTrees - i - 1) + "\t" + memused + "k");
        }
    }
}
