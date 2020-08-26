/**
 * Java implementation of Gradient Boosting Decision Tree
 * Contact: Yandong Liu (askjupiter@gmail.com)
 * License: MIT
 */
package JBDT.data;

import JBDT.classifier.tree.Feature;
import JBDT.util.Util;

import java.util.ArrayList;
import java.util.HashMap;

public class Data {
    public double[][] v; //vertical access v[i] = data for column i
    public int[] featureIds; //list of features ids. id = col index
    public Feature _weight;
    public HashMap<Integer, Feature> mapFeatureId2Feature; //id -> name
    public double[] _target; //target data. doing classification here
    public int _labelCol; //column for feature
    public int nFeatures;
    public Feature[] features;
    public int _nrows;
    public ArrayList<Integer> _u2q;        //  from url to a query index
    public ArrayList<ArrayList<Integer>> _q2u;  // from query to urls
    public ArrayList<String> _qList;
    public ArrayList<Boolean> _vNvQ;
    public ArrayList<Boolean> _vRaQ;  // is it a rank aggregation query?
    public ArrayList<Boolean> _vUnjudgedQ;
    public ArrayList<Boolean> _vQrySampled;
    public Data() {
        mapFeatureId2Feature = new HashMap<Integer, Feature>();
        _labelCol = -1;
        _weight = null;
        _target = null;
    }

    public int nrows() {
        return this._nrows;
    }

    public int ncols() {return nFeatures;}

    public void genIdx() {
        for (int i = 0; i < nFeatures; i++) {
            features[i].genIdx(_nrows);
        }
    }

    public double[] getWeight() {
        return _weight._vfeat;
    }

    public double getWeight(int i) {
        if (_weight == null || i >= _nrows) {
            Util.die("[ERROR] access of weight: out of bound\n");
        }
        return _weight._vfeat[i];
    }

    public double getWeightSum()
    {
        double ws = 0;
        double[] w = _weight._vfeat;

        for (int i = 0; i < _nrows; i++)
            ws += w[i];
        return ws;
    }

    void setWeightAll(double w)
    {
        if (_weight == null) {
            double[] v = new double[_nrows];
            _weight = new Feature(-1, "WEIGHT", v);
        }

        for (int i = 0; i < _nrows; i++)
            _weight._vfeat[i] = w;

    }

    public void clearFeatureSummary()
    {
        for (int i = 0; i < nFeatures; i++)
            this.features[i]._importance = 0;
    }
}
