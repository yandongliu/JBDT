package JBDT.classifier.tree;

import JBDT.data.SampleData;
import org.w3c.dom.Node;

import java.io.PrintStream;

public class Split {
    public double _bestSplitValue;
    public double _gain;
    public double _regAmount;
    double tau = 1e-10;
    public Feature _feature;
    String _featn;
    boolean _categorical;// indicates the feature is categorical
    int _setsize; // size of best cats array
    int[] _bestCats;


    //to delete
    double _totalWeight;
    double _leftWeight;
    double _leftResponse;
    double _rightResponse;
    int _len;
    int _leftCnt;

    public Split() {
        _featn = "NOF";
    }

    public Split(double bestGain, double bestRegAmount, double bestSplitValue, Feature feature) {
        this._gain = bestGain;
        this._regAmount = bestRegAmount;
        this._bestSplitValue = bestSplitValue;
        this._feature = feature;
        this._featn = feature._name;
        this._categorical = false;
    }

    public String getFeatureName() {
        return this._featn;
    }

    public boolean worseThan(Split b, SampleData sd) {
        Feature fa = this._feature;
        Feature fb = b._feature;
        double gain_a = fa._weight*this._gain;
        double gain_b = fb._weight * b._gain;
        double gainhigh = gain_b*(1+tau);
        double gainlow = gain_b*(1-tau);
        if (gain_a <= gainlow) return true;
        else return false;
    }

    boolean write(PrintStream out) {
        out .print( "S " + _featn + " " + _bestSplitValue + " " + _gain
                        + " " + _leftResponse +
        " " + _rightResponse + " "
                + _totalWeight + " " + _leftWeight + " " + _len + " "
                + _leftCnt + "\n");

        return true;
    }

    public boolean readXML(org.w3c.dom.Node parent, boolean caseSensitive) {
        _featn = parent.getAttributes().getNamedItem("feature").getNodeValue();
        if(_featn.equals("")) return false;

        //categorical features are not supported
        try {
            _bestSplitValue = Double.parseDouble(parent.getAttributes().getNamedItem("value").getNodeValue());
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            return false;
        }
        try {
            Node e = parent.getAttributes().getNamedItem("gain");
            if(e!=null) {
                _gain = Double.parseDouble(e.getNodeValue());
            } else {
                _gain = 0.0;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            _gain = 0.0;
        }
        return true;
    }

}
