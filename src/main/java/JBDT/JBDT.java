/**
 * Java implementation of Gradient Boosting Decision Tree
 * Author: Yandong Liu (askjupiter@gmail.com)
 * License: MIT
 */
package JBDT;

import JBDT.classifier.tree.SplitParam;
import JBDT.classifier.tree.TauCmp;
import JBDT.data.Data;
import JBDT.data.DataReader;
import JBDT.errors.ArgumentsError;
import JBDT.regression.boosting.*;
import JBDT.util.Config;
import JBDT.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;


public class JBDT {

    boolean isConfigNull(String s){
        s = s.toLowerCase();
        return (s.equals("") || s.equals("none") || s.equals("null"));
    }

    void train(Config config) {
        Util.log("Training...");
        try {
            String learner = config.get("learner", "dtree");
            String train_fv_file = config.get("train.fv_file", "null");
            String train_delimiter = config.get("train.delimiter", "\t");
            String train_qu_file = config.get("train.qu_file", "null");
            String model_name = config.get("model_name", "gbdt");
            String output_dir = config.get("output_dir", "output");
            String train_exclude_features = config.get("train.exclude_features", "");
            config.train_max_rows = config.getInt("train.max_rows", 0);
            config.label = config.get("label", "LABEL");

            SplitParam reg = new SplitParam();

            HashSet<String> feature_excl = new HashSet<String>();
            String[] s_featureExcl = Util.tokenize(train_exclude_features, ",");
            for(String s:s_featureExcl) {
                feature_excl.add(s.trim().toLowerCase());
            }

            Data data = new Data();

            Util.log("loading feature/label data.");
            if (!train_fv_file.equals("null"))
                DataReader.readFeatureFile(train_fv_file, train_delimiter, feature_excl, config.label, data, config.config_dir);
            if (!train_qu_file.equals("null"))
                DataReader.readQu(train_qu_file, null, data, config.config_dir);

            Util.log("\n");
            Util.log("Train config:");
            Util.log("\t| model_name = " + model_name);
            Util.log("\t| output_dir = " + output_dir);
            Util.log("\t| fv_file = " + train_fv_file);
            if (config.train_max_rows != 0)
                Util.log("\t| max_rows = " + config.train_max_rows);
            Util.log("\t| label = " + config.label);
            Util.log("\t| qu_file = " + train_qu_file);
            if( train_exclude_features!=null )
                Util.log("\t| train.exclude_features = " + train_exclude_features);
            Util.log("\t| learner = " + learner);

            Util.log("\n");
            Util.log("Tree splitting config: ");
            String splitCmpMode = config.get("regtree.splitcmp.mode", "");
            if (! isConfigNull(splitCmpMode)){
                Util.log("\t| regtree.splitcmp.mode = " + splitCmpMode);
            }
            String splitCmp_feature_weight_file = config.get("regtree.splitcmp.feature_weight_file", "");
            if (splitCmpMode == "feature_weight" && ! isConfigNull(splitCmp_feature_weight_file)){
                Util.log("\t| regtree.splitcmp.feature_weight_file = " + splitCmp_feature_weight_file);
            }
            String splitCmp_feature_weight = config.get("regtree.splitcmp.feature_weight", "");
            if (splitCmpMode == "feature_weight" && ! isConfigNull(splitCmp_feature_weight)){
                Util.log("\t| regtree.splitcmp.feature_weight = " + splitCmp_feature_weight);
            }
            if (isConfigNull(splitCmpMode)){
                double tau = config.getDouble("regtree.splitcmp.tau", 1e-10); // sets tau threshold for tie breaking
                if (tau != 1e-6)
                    Util.log("\t| splitcmp.tau = " +tau);
                reg.cmp = new TauCmp(tau);
            }
            Util.log("\t| min_node_sample = " + reg.MIN_SAMPLE);
            reg.MIN_BALANCE_FACTOR = config.getInt("regtree.min_balance_factor", 0);
            Util.log("\t| min_balance_factor = " + reg.MIN_BALANCE_FACTOR);

            QLoss loss = null;
            if(learner.equals("dtree")||learner.equals("gbdt")) {
                loss = new RLoss(config);
            } else if (learner.equals("gbrank")) {
                System.out.println("LOSS pairwise");
                config.put("model_name", "gbrank");

                config.put("gbrank.tau", "1.0");
                config.put("gbrank.alpha", "0.0");
                config.put("gbrank.tied_wt", "1.0");

                loss = new GBRankLoss(config);
            } else {
                Util.die("learner "+learner+" not supported.");
            }

            TreeNet treenet = new TreeNet(config);
            new GradientBoosting().gbdt(data, loss, treenet);

            File f_output_dir = new File(output_dir);
            if(!f_output_dir.exists()) f_output_dir.mkdir();
            treenet.writeModels(output_dir, model_name, "coxe");

            PrintStream fw = new PrintStream(output_dir+"/feature.imp");
            treenet.genFeatureSummary(data);

            System.out.println("\nFEATURE IMPORTANCE:");
            treenet.printFeatureSummary(data, System.out);
            System.out.println();
            treenet.printFeatureSummary(data, fw);
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void test(Config config) {
        Util.log("Testing...");
        try {
            String model_name = config.get("model_name", "gbdt");
            String output_dir = config.get("output_dir", "output");
            String output_file = config.get("output_file", model_name);
            String model_dir = config.get("model_dir", output_dir);
            String test_fv_file = config.get("test.fv_file", "");
            config.label = config.get("label", "LABEL");
            TreeNet bt = new TreeNet(config);
            String m = model_dir + "/" + model_name;
            bt.readXML(m + ".xml", false);
            Data d = new Data();
            HashSet<String> feature_excl = new HashSet<String>();
            DataReader.readFeatureFile(test_fv_file, "\t", feature_excl, config.label, d, config.config_dir);
            String lossname = config.get("regtree.loss", "regression");

            if(lossname.equals("regression")) {
                config.loss = QLoss.REGRESSION;
            } else if(lossname.equals("logistic")) {
                config.loss = QLoss.LOGISTIC;
            } else {
                Util.die("Unknown loss:"+lossname);
            }

            Util.log("\n");
            Util.log("Test config:");
            Util.log("\t| model_name = " + model_name);
            Util.log("\t| model_dir = " + model_dir);
            Util.log("\t| output_dir = " + output_dir);
            Util.log("\t| fv_file = " + test_fv_file);
            if (config.train_max_rows != 0)
                Util.log("\t| max_rows = " + config.train_max_rows);
            Util.log("\t| loss = " + lossname);
            Util.log("\t| label = " + config.label);

            ArrayList<double[]> responses = new ArrayList<double[]>();
            ArrayList<Integer> testPoints = new ArrayList<Integer>();
            ArrayList<ArrayList<Double>> finalResponses;
            if(testPoints.isEmpty()) {
                testPoints.add(bt.size());
            }
            bt.test(d, responses, testPoints, false);
            finalResponses = new ArrayList<ArrayList<Double>>();
            for (int i = 0; i < responses.size(); i++) {
                ArrayList<Double> v= new ArrayList<Double>();
                for (int j = 0; j < d.nrows(); j++) {
                    v.add(0.0);
                }
                finalResponses.add(v);
            }
            for (int i = 0; i < responses.size(); i++) {
                for (int j = 0; j < responses.get(i).length; j++) {
                    ArrayList<Double> v = finalResponses.get(i);
                    double dd = v.get(j);
                    v.set(j, dd+1.0*responses.get(i)[j]);
                }
            }
            for (int i = 0; i < finalResponses.size(); i++) {
                String outBaseName = output_dir + "/" + output_file;
                String fileName = outBaseName + "." + testPoints.get(i) + ".score";
                Util.log("Output " + fileName);
                PrintStream OUT = new PrintStream(fileName);
                for (int j = 0; j < finalResponses.get(i).size(); j++) {
                    if(config.loss == QLoss.LOGISTIC) {
                        double a = 1/(1+Math.exp(-2*finalResponses.get(i).get(j)));
                        finalResponses.get(i).set(j, a);
                    }
                    OUT.println(finalResponses.get(i).get(j));
                }
                OUT.close();
            }
            System.out.println("test finishes");
        } catch(IOException ex) {
            ex.printStackTrace();;
        }
    }

    void run(Config config) {
        if(config.train) {
            train(config);
        }
        if(config.test) {
            test(config);
        }
    }

    public static void main(String[] args) {
        try {
            Config config = Config.parseArgs(args);
            if ( config == null ) {
                Util.die("can't parse config file.");
            } else {
                JBDT main = new JBDT();
                main.run(config);
            }
        } catch (ArgumentsError ex) {
            System.out.println("Usage: java JBDT [--train] [--test] config.txt");
        }
    }

    public static String test() {
        return "Hello, world!";
    }
}

