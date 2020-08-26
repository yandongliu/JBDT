package JBDT.classifier.tree;


import JBDT.data.Data;
import JBDT.data.SampleData;
import org.w3c.dom.NodeList;
import JBDT.util.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

class TreeNetTraversal{
    int _nDepths;
    int _sumDepths;
    void clear(){
        clearDepth();
    }
    void clearDepth() {_sumDepths = 0; _nDepths = 0;};
    void addDepth(int d) {
        _nDepths ++;
        _sumDepths += d;
    }
    double AverageDepth() {
        if (_nDepths == 0)
            return 0;
        return (double)_sumDepths / _nDepths;
    }
}

public class Tree {
    Node _root;
    int _nnodes;
    boolean _analysisMode;
    TreeNetTraversal _analyzer;

    public Tree() {
        _root = null;
        _nnodes = 0;
    }

    public Node getRoot() {
        return _root;
    }

    public void grow(int nnodes, SampleData sd) {
        _nnodes = nnodes;
        _root = new Node(sd, null);

        //update residue and score
        _root.calcResponse(0);
        //find a good split
        _root.findSplit();
        Util.debug("best split:"+_root.getSplit().getFeatureName()+" "+_root.getSplit()._bestSplitValue);

        ArrayList<Node> bf_list = new ArrayList<Node>();
        bf_list.add(_root);

        while(bf_list.size()>0 && bf_list.size()<_nnodes) {
            int bi = 0;
            for (int i = 1; i < bf_list.size(); i++) {
                if(bf_list.get(i).gain() > bf_list.get(bi).gain()) {
                    bi = i;
                }
            }
            Node node = bf_list.get(bi);

            if(node.gain()<=0) break;

            node.split();
            if(node.isTerm()) {
                break;
            }

            bf_list.set(bi, node._left);
            bf_list.add(node._right);
        }
    }

    public double applyAgg(Data d, int i){
        if (_root == null)
            return 0;

        // use agg_response now
        Node p = _root, p1;
        int depth = 1;


        while((p1= p.nextNode(d, i)) !=  null) {
            p = p1;
            depth ++;
        }

        // This is wrong when there is multi-thread
        // Need to add analysis mode to turn off multi-thread
        if (_analysisMode && _analyzer!=null){
            Util.die("really?");
            _analyzer.addDepth(depth);
        }

        return p._agg_response;
    }

    public boolean write(PrintStream out){
        out.print("BT:\n");
        _root .write(out);
        return true;
    }

    public void writeXML(PrintStream out, int treeID, double shrinkage, boolean extended){

        out .print("            <Tree id=\"" + treeID);
        if (extended)
            out .print( "\" shrinkage=\"" +shrinkage);
        out.print( "\">\n");
        _root.writeXML(out, treeID, shrinkage, "                 ", extended);
        out .print( "            </Tree>\n");
    }

    public boolean writeTN(PrintStream out, int tn, double shrinkage){
        int nodeId =0;
        int termNodeId = 0;
        _root.numberingNodes(nodeId, termNodeId);
        _root.writeTN(out, tn, 0, shrinkage);
        out .print('D' + tn + ":\n\ntnscore = tnscore + response;\n\n");
        return true;
    }

    public void collectFeatureSummary (Data d, double shrinkage){
        _root.collectFeatureSummary(d, shrinkage);
    }

    public void collectFeatureNames (HashSet<String> features){
        _root.collectFeatureNames(features);
    }

    public boolean readXML(org.w3c.dom.Node parent, boolean caseSensitive) {
        _root = new Node(null, null);
        boolean isFound = false;
        //get all direct "node" or "response" nodes
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node node = nodes.item(i);
            if(node.getNodeType()== org.w3c.dom.Node.ELEMENT_NODE && node.getNodeName().equals("Node") || node.getNodeName().equals("Response")) {
                parent = node;
                isFound = true;
                break;
            }
        }
        if(!isFound) return false;
        return _root.readXML(parent, caseSensitive);
    }
}
