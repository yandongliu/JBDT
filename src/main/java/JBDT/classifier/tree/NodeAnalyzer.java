package JBDT.classifier.tree;

import JBDT.data.Data;
import org.w3c.dom.Node;

import java.util.ArrayList;

public class NodeAnalyzer {

    ArrayList<Double> sampleDistribution;
    double avgResponse;

    public NodeAnalyzer() {
        sampleDistribution = new ArrayList<Double>();
    }

    void clear() {
        sampleDistribution.clear();
    }

    void add(Data d, int i){
        int grade = (int) d._target[i];
        double weight = d.getWeight(i);
        assert(grade < 100);

        if (grade >= sampleDistribution.size()){
            for (int j = sampleDistribution.size(); j < grade+1; j++) {
                sampleDistribution.add(0.0);
            }
        }
        double t = sampleDistribution.get(grade);
        sampleDistribution.set(grade, t+weight);
    }

    void readXML(org.w3c.dom.Node eChild, boolean caseSensitive) {
        try {
            Node t = eChild.getAttributes().getNamedItem("response");
            if(t!=null)
                avgResponse = Double.parseDouble(t.getNodeValue());
        } catch (Exception ex) {
            ex.printStackTrace();;
        }
    }
}
