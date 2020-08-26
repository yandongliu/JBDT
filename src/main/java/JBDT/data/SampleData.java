package JBDT.data;

import JBDT.classifier.tree.Feature;
import JBDT.classifier.tree.Split;
import JBDT.util.Util;

enum SamplingMethod {
    Uniform
}

/**
 * data -> original dataset
 * index -> effect data index in this sample
 * nRows -> size of effect data
 */
public class SampleData {
    public static int MIN_SAMPLE = 10;
    public Data _data;
    public int[] _index, _featureIndex; //_index points to index in original data. could be 3,0,1,5...
    public int _nRows;
    public int _nSampCols;
    public double _totalResidual, _totalWeight;
    public double[] _residue;
    public double[] _score;

    void init(Data d) {
        _data = d;
        _nSampCols = _data.nFeatures;//_nSampCols . init v = all features size. will srhink after sampling
        _featureIndex = new int[_nSampCols];
        for (int i = 0; i < _nSampCols; i++) {
            _featureIndex[i] = i;
        }
    }

    /**
     * init. only called once to initialize on whole data.
     * make a copy of index
     * @param d
     * @param _index
     * @param _residue
     * @param _score
     */
    public SampleData(Data d, int[] _index, double[] _residue, double[] _score) {
        init(d);
        this._index = _index;
        this._nRows = d._nrows;
        this._residue = _residue;
        this._score = _score;
        for (int i = 0; i < _nRows; i++) {
            _index[i] = i;
        }
    }

    /**
     * index points to the starting address of int*. so make sure it does that
     * @param sd
     * @param index
     * @param len
     */
    public SampleData(SampleData sd, int[] index, int len) {
        init(sd._data);
        this._index = index;
        this._nRows = len;
        this._residue = sd._residue;
        this._score = sd._score;
    }

    //each time, sample again from the very original data
    public void sampleData(double rate) {
        this._nRows = 0;
        for (int i = 0; i < this._data._nrows; i++) {
            if(Math.random()<rate) {
                this._index[_nRows++] = i;
            }
        }
    }
    public void sampleFeatures(double rate) {
        this._nSampCols = 0;
        for (int i = 0; i < this._data.nFeatures; i++) {
            if(Math.random()<rate) {
                this._featureIndex[_nSampCols++] = i;
            }
        }
    }

    public void calcTotal() {
        _totalWeight = 0.0;
        _totalResidual = 0.0;
        double[] _w = _data._weight.getVec();
        for (int i = 0; i < _nRows; i++) {
            _totalWeight += _w[_index[i]];
            _totalResidual+=(_residue[_index[i]]*_w[_index[i]]);
        }
        Util.debug("_totalWeight:"+_totalWeight);
        Util.debug("_totalResidual:"+_totalResidual);
    }

    /**
     * get mean residue from each example
     * subtract from each example
     * add that to score of each example
     * @return
     */
    public double rebias() {
        _totalResidual = 0.0;
        _totalWeight = 0.0;
        double[] _w = _data._weight.getVec();
        for (int i = 0; i < _nRows; i++) {
            _totalWeight += _w[_index[i]];
            _totalResidual += _residue[_index[i]] * 1.0;
        }
        double response = _totalResidual/_totalWeight; //expected response
        _totalResidual -= response*_totalWeight;
        updateResidue(response, _index, _nRows);
        updateScore(response, _index, _nRows);
        return response;
    }

    void updateResidue(double delta, int[] index, int len) {
        for (int i = 0; i < len; i++) {
            _residue[index[i]] -= delta;
        }
    }

    void updateScore(double delta, int[] index, int len) {
        for (int i = 0; i < len; i++) {
            _score[index[i]] += delta;
        }
    }

    /**
     * partition/sort the data by a feature val
     * index will be re-arranged
     * @param split size of left partition
     * @return
     */
    public int partition(Split split, int[] leftIndex, int[] rightIndex) {
        Feature feat = split._feature;
        int leftCnt = feat.partition(_index, _nRows, split, null, leftIndex, rightIndex);
        return leftCnt;
//        gleftCnt = feat->partition(_gindex, _gnrows, split);
    }


    public Feature[] getFeatures() {
        Feature[] r = new Feature[_nSampCols];
        for (int i = 0; i < _nSampCols; i++) {
            r[i] = _data.mapFeatureId2Feature.get(_featureIndex[i]);
        }
        return r;
    }

//    public Feature findFeatureById(int _fid) {
//        return new Feature(-1, "");
//    }

//    public Feature findFeatureByName(String _name) {
//        return new Feature(-1, "");
//    }

//    Feature findFeature(int fid) {
//        for (int i = 0; i < _nFeatures; i++) {
//            if(_featureIndex[i]==fid) return
//        }
//    }

    public boolean isConstant() {
        for (int i = 0; i < _nRows; i++) {
            if(_residue[_index[0]]!= _residue[_index[i]]) return false;
        }
        return true;
    }

    public int nrows() {return _nRows;}
}
