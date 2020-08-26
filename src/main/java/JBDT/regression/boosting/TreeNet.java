package JBDT.regression.boosting;

import JBDT.classifier.tree.Feature;
import JBDT.classifier.tree.Split;
import JBDT.classifier.tree.Tree;
import JBDT.data.Data;
import JBDT.data.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import JBDT.util.Config;
import JBDT.util.Util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

class PairDoubleIntegerReverseComparator implements  Comparator<Pair<Double, Integer>> {
    @Override
    public int compare(Pair<Double, Integer> o1, Pair<Double, Integer> o2) {
        if(o1.first>o2.first) return 1;
        if(o1.first < o2.first) return -1;
        else return 0;
    }
}

class argResponse {
    public Data d;
    double[] v;
    public TreeNet bt;
    public int j;
}

class EarlyExits {
    enum OP {LT,GT};
    OP op;
    ArrayList<Pair<Integer, Double>>  thresholds;
}


public class TreeNet {
    public ArrayList<Tree> _trees;
    public ArrayList<Double> _shrinkage;
    public int nTrees;
    Config _config;
    EarlyExits ee;
    public TreeNet(Config config) {
        nTrees = 0;
        this._trees = new ArrayList<Tree>();
        this._shrinkage = new ArrayList<Double>();
        this._config = config;
        this.ee = new EarlyExits();
    }
    public void add(Tree tree, double shrinkage) {
        this._trees.add(tree);
        this._shrinkage.add(shrinkage);
        ++nTrees;
    }

    public int size() { return _trees.size();}

    public void genFeatureSummary(Data d){
        int i=0;
        d.clearFeatureSummary();
        for (i=0; i<_trees.size(); i++){
            _trees.get(i).collectFeatureSummary(d, _shrinkage.get(i));
        }
    }

    public void printFeatureSummary(Data d, PrintStream out) {

        int n = d.ncols();
        int i;
        int f = n;
        ArrayList<Pair<Double, Integer>> imp =new ArrayList<Pair<Double, Integer>>(f);
        for (i = 0; i < f; i++) {
            imp.add(null);
        }
        Feature[] feats = new Feature[f];

        for (i =0; i< n; i++){
            imp.set(i, new Pair<Double, Integer>(d.features[i].importance(), i));
            feats[i] = d.features[i];
        }

        Collections.sort(imp, new PairDoubleIntegerReverseComparator());
        Collections.reverse(imp);

        double max_imp = imp.get(0).first;

        if (max_imp == 0){
            System.out.println("Maximum importance is zero!");
        }

        out.println("RANK\tFEATURE\tIMPORTANCE\n");

        for(i = 0; i < f; i ++){
            if (imp.get(i).first == 0)
                break;
            out.println(i+1 + "\t" + feats[imp.get(i).second]._name + "\t" + String.format("%.4f",Math.sqrt(imp.get(i).first)/Math.sqrt(max_imp)*100));
        }

    }

    public void writeModels(String outputdir, String filename, String mode){
        try {
            String baseFileName = outputdir + "/" + filename;

            if (mode.indexOf("o") != -1) {
                PrintStream OUT = new PrintStream(baseFileName);
                write(OUT);
                OUT.close();
            }

            //output XML file
            if (mode.indexOf("x") != -1) {
                PrintStream OUTXML = new PrintStream(baseFileName + ".xml");
                writeXML(OUTXML, false);
                OUTXML.close();
            }

            if (mode.indexOf("e") != -1) {
                PrintStream OUTEXTXML = new PrintStream(baseFileName + ".ext.xml");
                writeXML(OUTEXTXML, true);
                OUTEXTXML.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();;
        }
    }

    boolean write(PrintStream out) {
        int _nnodes = 20;

        out .print( //fixed << setprecision(10);
                "BDT:\n"
                        + "v0.0\n"
                        + "NumTrees: " + _trees.size() + " NumTermNodes: " + _nnodes + "\n");


        int i;

        ArrayList<String> fn = genFeatureNames();

        out.print( "NumFeatures: " + fn.size() + '\n');

        for (i = 0; i < fn.size(); ++i) {
            out .print(fn.get(i));
            if (i != fn.size() - 1)
                out .print( ' ');
        }

        out.print( '\n');

        out .print( "Shrinkages:\n");
        if (_shrinkage.size() > 0) {
            out .print( _shrinkage.get(0));
        }
        for (i = 1; i < _shrinkage.size(); i++) {
            out .print( ' ' + _shrinkage.get(i));
        }
        out .print( '\n');


        for (i = 0; i < _trees.size(); i++) {
            out .print( "Tree " + i + '\n');
            _trees.get(i).write(out);
        }

        return true;
    }

    public void writeXML(PrintStream out, boolean extended) {
        out .print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out .print( "<MlrFunction name=\"CHANGE THIS\" featuredef=\"featuredefs.xml\" version=\"1.0\">\n");
        out .print( "\n  <!-- ADD SCORE STANDARDIZATION OR CALIBRATION HERE -->\n\n");
        out .print( "    <DecisionTree loss=\"" + _config.get("loss", "default loss") + "\">\n");
        out .print( "\n    <!-- ADD EARLY EXIT HERE -->\n\n");

        out .print( "        <Forest>\n");

        for (int i = 0; i < _trees.size(); i++) {
            _trees.get(i).writeXML(out, i, _shrinkage.get(i), extended);
        }

        out .print( "        </Forest>\n");
        out .print( "    </DecisionTree>\n");
        out .print( "</MlrFunction>\n");

    }

    ArrayList<String> genFeatureNames(){
        HashSet<String> sFeatures = genFeatureSet();
        ArrayList<String> vFeatures = new ArrayList<String>(sFeatures);
        return vFeatures;
    }

    HashSet<String> genFeatureSet(){
        HashSet<String> sFeatures = new HashSet<String>();
        for (int i=0; i<_trees.size(); i++){
            _trees.get(i).collectFeatureNames(sFeatures);
        }

        return sFeatures;
    }

    void outputIndent(int n) {
        for (int i = 0; i < n; i++) {
            System.out.print(" ");
        }
    }
    void outputNode(JBDT.classifier.tree.Node node, int l) {
        if(node!=null) {
            outputIndent(l);
            Split sp = node.getSplit();
            System.out.println(sp.getFeatureName()+" "+sp._bestSplitValue+" "+node._agg_response+" "+node._response);
            outputNode(node._left,l+2);
            outputNode(node._right,l+2);
        }
    }

    public void printTrees() {
        for (int i = 0; i < _trees.size(); i++) {
            System.out.println("Tree#:"+i);
            Tree t = _trees.get(i);
            outputNode(t.getRoot(),0);
        }
    }

    public boolean readXML(String xmlFile, boolean caseSensitive) {
        try {
            File file = new File(xmlFile);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(file);
            ArrayList<String> path = new ArrayList<String>();
            path.add("MlrFunction");
            path.add("DecisionTree");
            Node parent = Util.findElement(document, path, 0);
            if(parent!=null) {
                Node node_loss = parent.getAttributes().getNamedItem("loss");
                System.out.println(node_loss.getNodeValue());
            }
            path.add("Forest");
            parent = Util.findElement(document, path, 0);
            //get tree nodes
            NodeList nodes = parent.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if(node.getNodeType() == Node.ELEMENT_NODE&&node.getNodeName().equals("Tree")) {
                    Tree tmp = new Tree();
                    tmp.readXML(node, false);
                    _trees.add(tmp);
                    _shrinkage.add(1.0);
                }
            }
            //we can also do following:
//            NodeList treeNodes = ((Element)parent).getElementsByTagName("Tree");
//            System.out.println("treeNodes#:"+treeNodes.getLength());
        } catch ( Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public void test(Data d, ArrayList<double[]> responses, ArrayList<Integer> testPoints, boolean isEarlyExit){
        if (_trees.size() == 0)
            return;

        ArrayList<Integer> _testPoints = new ArrayList<Integer>(testPoints);

        int nThreads = _config.getInt("nThreads", 1); // config is a global variable
        ArrayList<Integer> seg = Util.divideLoop(d.nrows(), nThreads);
        double[] v = new double[d.nrows()];


        ArrayList<Boolean> mark = new ArrayList<Boolean>();
        for (int i = 0; i < d.nrows(); i++) {
            mark.add(i, false);
        }

        argResponse arg = new argResponse();
        arg.d = d;
        arg.v = v;
        arg.bt = this;
//        arg.mark = mark;

        ArrayList<Pair<Integer, Double>> th = ee.thresholds;


        responses.clear();
        int i, j;
        for(j = 0; j < _trees.size() && !_testPoints.isEmpty(); j ++){
            arg.j = j;
            compResponses(arg, d.nrows());

    /* Dump Responses */
            if (_testPoints.size() > 0 && j+1 == _testPoints.get(0)){
                responses.add(new double[d.nrows()]);
                int k = responses.size()-1;
                for(i = 0; i < d.nrows(); i ++){
                    responses.get(k)[i] = v[i];
                }
                _testPoints.remove(_testPoints.get(0));
            }

    /* Set early exit */
            if (isEarlyExit){
                if (th.size() > 0 && j == th.get(0).first){
                    for(i = 0; i < d.nrows(); i ++){
                        if (mark.get(i))
                            continue;
                        mark.set(i,  ( ((ee.op == EarlyExits.OP.GT) && (v[i] > th.get(0).second))
                        || ((ee.op == EarlyExits.OP.LT) && (v[i] < th.get(0).second)) ));
                    }
                    th.remove(th.get(0)); // // remove the first element from th
                }
            }
        }
    }
    void compResponses(argResponse __arg, int n){
        int start =0;
        int end = n;

        for(int i = start;i < end; i ++){
            __arg.v[i]+= (__arg.bt._trees.get(__arg.j).applyAgg(__arg.d, i)* __arg.bt._shrinkage.get(__arg.j));
        }

        return ;
    }
}
