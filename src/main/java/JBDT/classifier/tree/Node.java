package JBDT.classifier.tree;

import JBDT.data.Data;
import JBDT.data.SampleData;
import org.w3c.dom.NodeList;
import JBDT.util.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

public class Node {
    SampleData _sd;
    public double _response;
    int _nSamples;
    Split _split;
    public Node _left, _right, _parent;
    int _nodeId;
    public double _agg_response;
    boolean _analysisMode;
    NodeAnalyzer analyzer;

    public Node(SampleData sd, Node parent) {
        this._sd = sd;
        this._left = null;
        this._right = null;
        _parent = parent;
        _nodeId = -1;
        _response = _agg_response = 0.0;
        _analysisMode = false;
        this._split = new Split();
        this.analyzer = new NodeAnalyzer();
    }

    /**
     * compute mean residual from each example
     * @param parent_agg_resp
     */
    public void calcResponse(double parent_agg_resp) {
        _response = _sd.rebias(); //mean response
        _agg_response = _response + parent_agg_resp;
    }

    void computeSplits(Feature[] features, ArrayList<Split> allSplits) {
        for (int i = 0; i < features.length; i++) {
            Feature f = features[i];
            int maxLen = _sd._data._nrows;
            double[] vResidues = new double[maxLen];
            double[] vWeights = new double[maxLen];

            Split s = f.split(_sd._index, _sd._nRows, maxLen, _sd._residue, _sd._data.getWeight(), _sd._totalResidual, _sd._totalWeight, vResidues, vWeights);
            allSplits.add(s);
        }
    }

    /**
     * after this _split = bestSplit
     */
    public void findSplit() {

        _nSamples = _sd._nRows;

        if(_sd.isConstant() || _sd._nRows<=SplitParam.MIN_SAMPLE)
            return;

        ArrayList<Split> allSplits = new ArrayList<Split>();

        //find best feature with best cut
        computeSplits(_sd.getFeatures(), allSplits);

        for (int i = 0; i < allSplits.size(); i++) {
//            System.out.println("split["+i+"]:"+allSplits.get(i)._feature._name+"/"+allSplits.get(i)._bestSplitValue+" gain:"+allSplits.get(i)._gain);
        }

        if(allSplits.size()==0) return;

        Split bestSplit = allSplits.get(0);
        for (int i = 1; i < allSplits.size(); i++) {
            if(bestSplit.worseThan(allSplits.get(i), _sd)) {
                bestSplit = allSplits.get(i);
            }
        }

        //split has feature name, cut value
        _split = bestSplit;
    }



    void split() {
        int leftCnt = 0;
        int[] leftIndex = new int[_sd._nRows];
        int[] rightIndex = new int[_sd._nRows];
        leftCnt = _sd.partition(_split, leftIndex, rightIndex);
        int[] _leftIndex= new int[leftCnt];
        int[] _rightIndex= new int[_sd._nRows - leftCnt];
        for (int i = 0; i < leftCnt; i++) {
            _leftIndex[i] = leftIndex[i];
        }
        for (int i = 0; i < _sd._nRows - leftCnt; i++) {
            _rightIndex[i] = rightIndex[i];
        }

        if(leftCnt == 0 || leftCnt == _sd.nrows()) return;

        if(leftCnt != 0) {
            SampleData leftd = new SampleData(_sd, _leftIndex, leftCnt);
            _left = new Node(leftd, this);
            _left.calcResponse(_agg_response);
            _left.findSplit();
        } else {

        }

        int rightCnt = _sd._nRows - leftCnt;
        if(rightCnt>0) {
            int[] right_index;
            SampleData rightd = new SampleData(_sd, _rightIndex, rightCnt);
            _right = new Node(rightd, this);
            _right.calcResponse(_agg_response);
            _right.findSplit();
        }
    }

    public Split getSplit() {
        return this._split;
    }

    boolean isTerm() {
        return _left==null&& _right==null;
    }

    double gain() {
        return _split._gain;
    }

    boolean write(PrintStream out) {
        out .print("BN: " + _nodeId + ' '+ (_left != null) + ' ' + (_right != null) + ' ' + _response + ' ' +_agg_response + ' ' + _nSamples +'\n');
        _split.write(out);

        if(_left!=null) {
            _left.write(out);
        }
        if(_right!=null) {
            _right.write(out);
        }
        return true;
    }

    void writeXML(PrintStream out, int tn, double shrinkage, String indent, boolean extended){
        String additionalInfo ="";

        if (extended){
            additionalInfo = " nSamples=\"" + _nSamples +"\"";
            if (!isTerm())
            additionalInfo += " response=\"" + (_agg_response * shrinkage) +"\"";
            additionalInfo += " gain=\"" + _split._gain + "\" reg=\"" + _split._regAmount +"\"";
        }

        if (_analysisMode){
            additionalInfo += analyzer.toString();
        }

        if (isTerm()){
            out .print(indent + "<Response value=\"" + _agg_response * shrinkage  + "\" id=\"" + 'T' +tn + '_' + _nodeId + "\"" + additionalInfo + "/>\n");
        } else {
            if (_split._categorical) {
                out .print(indent + "<Node feature=\"" + _split._featn + "\" value=\"");

                for (int i=0; i<_split._setsize; i++) {
                    out .print( _split._feature.ivaluemap.get(_split._bestCats[i]));
                    if (i != _split._setsize-1)
                        out .print( ",");
                }

                out .print("\" id=\"" + 'N' + tn + '_' + _nodeId + "\"" + additionalInfo + ">\n");
            } else {
                out .print( indent + "<Node feature=\"" + _split._featn + "\" value=\"" + _split._bestSplitValue +"\" id=\"" + 'N' + tn + '_' + _nodeId + "\"" + additionalInfo + ">\n");
            }

            _left.writeXML(out, tn, shrinkage, indent + "    ", extended);
            _right.writeXML(out, tn, shrinkage, indent + "    ", extended);
            out.print(indent + "</Node>\n");
        }
    }

    void numberingNodes(Integer nodeId, Integer termNodeId)
    {
        if (isTerm()) {
            _nodeId = ++termNodeId;
        }
        else {
            _nodeId = ++nodeId;
            if(_left!=null) {
                _left.numberingNodes(nodeId, termNodeId);
            }
            if(_right!=null) {
                _right.numberingNodes(nodeId, termNodeId);
            }
        }
    }

    Feature findFeature(Data d) {
        for (int i = 0; i < d.nFeatures; i++) {
            if(d.features[i]._name.equals(_split._featn)) {
                return d.features[i];
            }
        }
        return null;
    }

    Node nextNode(Data d, int i){
        if (_analysisMode){
            analyzer.add(d, i);
        }

        if (isTerm())
            return null;

        Feature f = findFeature(d);

        // Fillin 0 when the feature is not in the fv file
        double v = 0.0;

        if (f!=null)
            v = f.getFeatValue(i);


        if (f!=null && f._categorical) {
            //some work to be done here
            Util.die("categorical not supported.");
        } else {
            if (v < _split._bestSplitValue)
                return _left;
            else
                return _right;
        }
        return null;
    }

    boolean writeTN(PrintStream out, int tn, double response, double shrinkage)
    {
        response +=  _response;
        if(isTerm()) {
            out.print('T'+tn+'_'+_nodeId+": \n  response = "+response * shrinkage+";\n goto D"+tn+";\n\n");
        }
        else {
            if (_split._categorical) {
                out.print('N'+tn+'_'+_nodeId+": \n  if "+_split._featn+" in (");

                for (int i=0; i<_split._setsize; i++) {
                    out.print( _split._feature.ivaluemap.get(_split._bestCats[i]));

                    if (i != _split._setsize-1)
                        out.print( ", ");
                }
                out.print( ") then goto ");
            } else {
                out.print('N'+tn+'_'+_nodeId+": \n  if "+_split._featn+" < "+ _split._bestSplitValue+ " then goto ");
            }
            out.print( (_left.isTerm()?'T':'N')+tn+'_'+_left._nodeId+";\n else goto "+ (_right.isTerm()?'T':'N')+tn+'_'+_right._nodeId+";\n\n");

            _left.writeTN(out, tn, response, shrinkage);
            _right.writeTN(out, tn, response, shrinkage);
        }
        return true;
    }

    void turnOnAnalysis(){
        _analysisMode = true;
        analyzer.clear();
        if (_left!=null)
            _left.turnOnAnalysis();
        if(_right!=null)
            _right.turnOnAnalysis();
    }

    public double collectFeatureSummary (Data d, double shrinkage){
        // Terminal node
        if (_left==null && _right==null)
            return 0;

        Feature feat;
        if (_split._featn!="NOF"){
            feat = _split._feature;
            feat._importance += shrinkage * _split._gain+1e-10;

            if (_left!=null)
                _left.collectFeatureSummary(d, shrinkage);

            if (_right!=null)
                _right.collectFeatureSummary(d, shrinkage);

            return feat._importance;
        }

        return 0;
    }

    //catmap is for categorical features.
    public void collectFeatureNames (HashSet<String> features){
        if (_left==null && _right==null)
            return;

        if (!_split._featn.equals("NOF")) {
            features.add(_split._featn);

            if (_split._categorical ) {//&& catmap!=null) { // rebuild mapping during testing
                Util.die("not supported");
            }
        }

        if (_left!=null)
            _left.collectFeatureNames(features);
        if (_right!=null)
            _right.collectFeatureNames(features);
    }

    public boolean readXML(org.w3c.dom.Node parent, boolean caseSensitive) {
        if(parent.getNodeName().equals("Response")) {
            try {
                _agg_response = _response = Double.parseDouble(parent.getAttributes().getNamedItem("value").getNodeValue());
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                return false;
            }
        } else {
            if(!_split.readXML(parent, caseSensitive)) {
                return  false;
            }
            try {
                org.w3c.dom.Node t = parent.getAttributes().getNamedItem("response");
                if(t!=null)
                    _agg_response = Double.parseDouble(t.getNodeValue());
                if(_parent!=null)
                    _response = _agg_response - _parent._agg_response;
            } catch (Exception ex) {
                ex.printStackTrace();
                _agg_response = 0.0;
            }

            try {
                org.w3c.dom.Node t = parent.getAttributes().getNamedItem("nSamples");
                if(t!=null)
                    _nSamples = Integer.parseInt(t.getNodeValue());
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                _nSamples = 0;
            }
            int nChildren = 0;
            NodeList nodes = parent.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Node node = nodes.item(i);
                if(node.getNodeType()== org.w3c.dom.Node.ELEMENT_NODE && node.getNodeName().equals("Node") || node.getNodeName().equals("Response")) {
                    if(nChildren == 0) {
                        _left = new Node(null, null);
                        _left.readXML(node, caseSensitive);
                        _left._parent = this;
                    } else if(nChildren == 1) {
                        _right = new Node(null, null);
                        _right.readXML(node, caseSensitive);
                        _right._parent = this;
                    } else {
                        return false;
                    }
                    nChildren++;
                }
            }
            analyzer.readXML(parent, caseSensitive);
        }
        int termId = 0, nodeId = 0;
        numberingNodes(termId, nodeId);
        return true;
    }

}
