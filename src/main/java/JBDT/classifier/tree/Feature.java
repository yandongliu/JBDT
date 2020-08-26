package JBDT.classifier.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

class Comp implements Comparator<Integer> {

    double[] _vfeat;
    double eps = 1e-10;

    public Comp(double[] vfeat) {
        this._vfeat = vfeat;
    }

    @Override
    public int compare(Integer a, Integer b) {
        double u = _vfeat[a.intValue()];
        double v = _vfeat[b.intValue()];
        if( u > v ) return 1;
        else if(u<v) return -1;
        else return 0;
    }
}

public class Feature {
    public static int NOIDX = 0;
    public static int SHORTIDX = 1;
    public static int MEDIANIDX = 2;
    public static int LONGIDX = 3;
    public double _weight;
    public int _id;
    public int _nValues;//unique values
    public String _name;
    ArrayList<String> ivaluemap;

    public double[] _vfeat;//holds value of this feature for each example
    public double _importance;
    double[] _vFeatValues; //store uniq vals of this feature
    int _modeIdx;
    int[] _vSIdx;//SHORT reverse index. _vSIdx[i] = idx. exmaple i 's feature value's index i idx
    int[] _vMIdx;//MEDIAN reverse index.
    int[] _vLIdx;//MEDIAN reverse index.
    boolean _categorical = false;

    boolean DEBUG =false;

    public Feature(int id, String name, double[] data) {
        this._id = id;
        this._name = name;
        this._nValues = 0;
        this._vfeat = data;
        this._weight = 1.0;
        this._modeIdx = 0;//noIdx by default
    }

    public double[] getVec() {
        return this._vfeat;
    }

    public double importance() {
        return _importance;
    }

    double idxFeatValue(int i) {
        return _vFeatValues[i];
    }

    public double getFeatValue(int i) {
        if (_modeIdx == NOIDX){
            return _vfeat[i];
        } else if (_modeIdx == SHORTIDX)
            return _vFeatValues[_vSIdx[i]];
        else if (_modeIdx == MEDIANIDX)
            return _vFeatValues[_vMIdx[i]];
        else if (_modeIdx == LONGIDX)
            return _vFeatValues[_vLIdx[i]];

        return 0;
    }
    Split split(int[] index, int len, int glen, double[] residue, double[] weight, double totalResidue, double totalWeight,double[] vResidues, double[] vWeights) {
        if(_modeIdx == NOIDX) {
            return _split1(index,  len,  glen,  residue, weight,  totalResidue,  totalWeight);
        } else {
            return _split2(index,  len,  glen,  residue, weight,  totalResidue,  totalWeight, vResidues,  vWeights);
        }
    }

    Split _split1(int[] index, int len, int glen, double[] residue,
                  double[] weight, double totalResidue, double totalWeight) {

        //sort index by feature values
        Comp cmp = new Comp(_vfeat);
        Integer[] sorted = new Integer[len];
        for (int i = 0; i < len; i++) {
            sorted[i] = index[i];
        }
        Arrays.sort(sorted, cmp);
        for (int i = 0; i < len; i++) {
            index[i] = sorted[i];
        }


        // index has been sorted
        //double response = totalResidue/totalWeight;
        double bestSplitValue = 0;//_vfeat[index[0]] -1;
        double bestGain = 0;
        double leftWeight = 1e-10;

        double leftResidue = 0;
        double bestlResponse = 0;
        double bestrResponse = 0;
        double bestLeftWeight = 0;
        int bestLeftCnt = 1;
        int idx = index[0];

        if (weight!=null) {
            leftWeight += weight[idx];
            leftResidue += residue[idx] * weight[idx];
        } else {
            leftWeight += 1;
            leftResidue += residue[idx];
        }

        double curFeat = _vfeat[idx];
        bestLeftWeight = leftWeight;


        // Change the gain calculation to
        double lam1 = SplitParam.lamL1, lam2 = SplitParam.lamL2;

        double lResponse, rResponse, rightWeight, rightResidue, lr2, rr2, gain, w,
                regAmount, bestRegAmount;
        double leftPct;
        bestRegAmount = 0;
        for (int i = 1; i < len; i++) {
            idx = index[i];
            double nextFeat = _vfeat[idx];

            if (curFeat != nextFeat) {
                // BALANCED CUT
                leftPct = leftWeight / totalWeight;
                // END of BALANCED CUT

                lResponse = leftResidue / leftWeight;
                rightWeight = totalWeight - leftWeight;
                rightResidue = totalResidue - leftResidue;
                rResponse = rightResidue / rightWeight;
                lr2 = lResponse * lResponse;
                rr2 = rResponse * rResponse;
                gain = (lr2 * leftWeight + rr2 * rightWeight);
                regAmount = lam1 * (Math.abs(lResponse) + Math.abs(rResponse)) + lam2 * (lr2 + rr2);
                gain -= regAmount;

                //if (gain > bestGain and min(leftWeight,rightWeight) >= reg.MIN_SAMPLE and min(leftPct,1-leftPct) > reg.MIN_BALANCE_FACTOR)
                if (gain > bestGain && Math.min(leftPct,1-leftPct) > SplitParam.MIN_BALANCE_FACTOR)
                {
                    bestGain = gain;
                    bestRegAmount = regAmount;
                    bestSplitValue = 0.5 * ((double) curFeat + (double) nextFeat);
                    bestlResponse = lResponse;
                    bestrResponse = rResponse;
                    bestLeftWeight = leftWeight;
                    bestLeftCnt = i;
                }
                curFeat = nextFeat;
            }
            if (weight!=null)
                w = weight[idx];
            else
                w = 1;
            leftWeight += w;
            leftResidue += residue[idx] * w;
        }

        // gain of the previous node should be subtracted
        bestGain -= totalResidue * totalResidue / totalWeight;
        bestGain = Math.max(bestGain, 0.0);
        //END

        return new Split(bestGain, bestRegAmount, bestSplitValue, this);
    }

    Split _split2(int[] index, int len, int glen, double[] residue, double[] weight, double totalResidue, double totalWeight,double[] vResidues, double[] vWeights) {
        double w;
        int idx, vIdx;

        if(totalWeight<=0.0) {
            System.out.println("WRONG. tweight = 0.0");
        }

        // initialization
        Arrays.fill(vResidues, 0.0);
        Arrays.fill(vWeights, 0.0);

        // fill in the values

        if (_modeIdx == SHORTIDX) { //here
            for (int i = 0; i < len; i++) {
                idx = index[i];
                vIdx = _vSIdx[idx];
                w = weight[idx];
                vWeights[vIdx] += w;
                vResidues[vIdx] += w * residue[idx];
            }
        } else if (_modeIdx == MEDIANIDX) {
            for (int i = 0; i < len; i++) {
                idx = index[i];
                vIdx = _vMIdx[idx];
                w = weight[idx];
                vWeights[vIdx] += w;
                vResidues[vIdx] += w * residue[idx];
            }
        } else if (_modeIdx == LONGIDX) {
            for (int i = 0; i < len; i++) {
                idx = index[i];
                vIdx = _vLIdx[idx];
                w = weight[idx];
                vWeights[vIdx] += w;
                vResidues[vIdx] += w * residue[idx];
            }
        }

        double bestSplitValue = 0;//_vfeat[index[0]] -1;
        double bestGain = 0;
        double leftWeight = 1e-10;

        double leftResidue = 0;
        double bestlResponse = 0;
        double bestrResponse = 0;
        double bestLeftWeight = 0;
        int bestSplitIdx = 0;

        // split


        double lam1 = SplitParam.lamL1, lam2 = SplitParam.lamL2;

        double lResponse, rResponse, rightWeight, rightResidue, lr2, rr2, gain,
                regAmount, bestRegAmount;

        double leftPct;

        bestRegAmount = 0;
        int prevIdx = -1;
        for (int i = 0; i < _nValues; i++) {
            if (vWeights[i] == 0)
                continue;
            if (prevIdx == -1) {
                leftWeight += vWeights[i];
                leftResidue += vResidues[i];
                bestLeftWeight = leftWeight;
                prevIdx = i;
                continue;
            }

            // BALANCED CUT
            leftPct = leftWeight / totalWeight;
            // END of BALANCED CUT

            lResponse = leftResidue / leftWeight;
            rightWeight = totalWeight - leftWeight;
            rightResidue = totalResidue - leftResidue;
            rResponse = rightResidue / rightWeight;
            lr2 = lResponse * lResponse;
            rr2 = rResponse * rResponse;
            gain = (lr2 * leftWeight + rr2 * rightWeight);
            regAmount = lam1 * (Math.abs(lResponse) + Math.abs(rResponse)) + lam2 * (lr2 + rr2);
            gain -= regAmount;

            //if (gain > bestGain and min(leftWeight,rightWeight)>=reg.MIN_SAMPLE and min(leftPct,1-leftPct) > reg.MIN_BALANCE_FACTOR)
            if (gain > bestGain && Math.min(leftPct, 1 - leftPct) > SplitParam.MIN_BALANCE_FACTOR)
            {
                bestGain = gain;
                bestRegAmount = regAmount;
                bestlResponse = lResponse;
                bestrResponse = rResponse;
                bestLeftWeight = leftWeight;
                bestSplitIdx = i; // upper idx
                bestSplitValue = 0.5 * (idxFeatValue(prevIdx) + idxFeatValue(i));
            }

            prevIdx = i;
            leftResidue += vResidues[i];
            leftWeight += vWeights[i];
        }

        // gain of the previous node should be substracted
        bestGain -= totalResidue * totalResidue / totalWeight;
        bestGain = Math.max(bestGain, 0.0);

        return new Split(bestGain, bestRegAmount, bestSplitValue, this);
    }

    void swap(int[] index, int i, int j) {
        int tmp = index[i];
        index[i] = index[j];
        index[j] = tmp;
    }

    int qs_partition(int[] index, double[] _vfeat, int start, int end) {
        int store = start;
        double pn = _vfeat[index[end]];
        for(int i=start;i<end;i++) {
            if(_vfeat[index[i]]<pn) {
                swap(index, i, store);
                ++store;
            }
        }
        swap(index, store, end);
        return store;
    }

    void qsort(int[] index, double[] _vfeat, int start, int end) {
        if(start >= end) return;
        int pivot = qs_partition(index, _vfeat, start, end);
        qsort(index, _vfeat, start, pivot-1);
        qsort(index, _vfeat, pivot+1, end);
    }

    /**
     * create index for values on this feature
     * @param n
     */
    public void genIdx(int n) {

        if(_vfeat == null) return;

        int[] index = new int[n];
        for (int i = 0; i < n; i++)
            index[i] = i;

        if(DEBUG) {
            System.out.println(this._name+" before sort");
            for (int i = 0; i < 1000&&i<n; i++) {
                System.out.println("["+i + "]: index:" + index[i] + " val:" + _vfeat[index[i]]);
            }
            System.out.println();
        }
        //sort index by feature values
        Comp cmp = new Comp(_vfeat);
        Integer[] sorted = new Integer[index.length];
        for (int i = 0; i < index.length; i++) {
            sorted[i] = index[i];
        }
//        System.arraycopy(index,0,sorted,0,index.length);
        for (int i = 0; i < sorted.length; i++) {
//            System.out.println(sorted[i]);
        }
        Arrays.sort(sorted, cmp);
        for (int i = 0; i < index.length; i++) {
            index[i] = sorted[i];
        }
        for (int i = 0; i < index.length; i++) {
//            System.out.println(index[i]);
        }
//        System.arraycopy(sorted,0,index,0,index.length);
//        qsort(index, _vfeat, 0, n - 1);

        if(DEBUG) {
            System.out.println(this._name+" after sort");
            for (int i = 0; i < 1000 && i < n; i++) {
                System.out.println("["+i + "]: index" + index[i] + " val:" + _vfeat[index[i]]);
            }
        }

        // #of uniq values
        ArrayList<Double> vFeatValues = new ArrayList<Double>();
        vFeatValues.add(_vfeat[index[0]]);
        for (int i = 1; i < n; i++) {
            if (_vfeat[index[i]] != vFeatValues.get(vFeatValues.size()-1))
                vFeatValues.add(_vfeat[index[i]]);
        }

        _nValues = vFeatValues.size();

        // whether to convert it into index
        if (_nValues > 65536) {
//            Util.debug("_nValues > 65536");
            return;
        }

        _vFeatValues = new double[_nValues];
        for (int i = 0; i < _nValues; i++)
            _vFeatValues[i] = vFeatValues.get(i);

        if (vFeatValues.size() <= 256) {
            // short index
            _modeIdx = SHORTIDX;
            _vSIdx = new int[n];

            int idx = 0;
            for (int i = 0; i < n; i++) {
                if (_vfeat[index[i]] != _vFeatValues[idx])
                    idx++;
                _vSIdx[index[i]] = idx;
            }
        } else if (_nValues <= 65536) {
            _modeIdx = MEDIANIDX;
            _vMIdx = new int[n];
            int idx = 0;
            for (int i = 0; i < n; i++) {
                if (_vfeat[index[i]] != _vFeatValues[idx])
                    idx++;
                _vMIdx[index[i]] = idx;
            }
        } else {
            // long index
            //assert(_nValues <= 4294967296);
            _modeIdx = LONGIDX;
            _vLIdx = new int[n];
            int idx = 0;
            for (int i = 0; i < n; i++) {
                if (_vfeat[index[i]] != _vFeatValues[idx])
                    idx++;
                _vLIdx[index[i]] = idx;
            }
        }
    }

    /**
     * new version of partition() since Java doesnt support array pointers
     * @param index
     * @param len
     * @param split
     * @param pmap
     * @param leftIndex
     * @param rightIndex
     * @return
     */
    public int partition(int[] index, int len, Split split, boolean[] pmap, int[] leftIndex, int[] rightIndex) {
        int lsize = 0, rsize = 0;
        int tmp;

        if (_modeIdx == NOIDX) {
            for (int i = 0; i < len; i++) {
                if (_vfeat[index[i]] < split._bestSplitValue) {
                    if (pmap!=null) pmap[i] = true;
                    //new code
                    leftIndex[lsize] = index[i];

                    tmp = index[i];
                    index[i] = index[lsize];
                    index[lsize] = tmp;
                    lsize++;
                } else {
                    rightIndex[rsize] = index[i];
                    rsize++;
                }
            }
        } else if (_modeIdx == SHORTIDX) {
            for (int i = 0; i < len; i++) {
                if (_vFeatValues[_vSIdx[index[i]]] < split._bestSplitValue) {
                    //i still keep the swapping code since i dont know if this sorted index will be used in other functions
                    if (pmap!=null) pmap[i] = true;

                    //new code
                    leftIndex[lsize] = index[i];

                    tmp = index[i];
                    index[i] = index[lsize];
                    index[lsize] = tmp;
                    lsize++;
                } else {
                    rightIndex[rsize] = index[i];
                    rsize++;
                }
            }
        } else if (_modeIdx == MEDIANIDX) {
            for (int i = 0; i < len; i++) {
                if (_vFeatValues[_vMIdx[index[i]]] < split._bestSplitValue) {
                    if (pmap!=null) pmap[i] = true;

                    //new code
                    leftIndex[lsize] = index[i];

                    tmp = index[i];
                    index[i] = index[lsize];
                    index[lsize] = tmp;
                    lsize++;
                } else {
                    rightIndex[rsize] = index[i];
                    rsize++;
                }
            }
        } else if (_modeIdx == LONGIDX) {
            for (int i = 0; i < len; i++) {
                if (_vFeatValues[_vLIdx[index[i]]] < split._bestSplitValue) {
                    if (pmap!=null) pmap[i] = true;

                    //new code
                    leftIndex[lsize] = index[i];

                    tmp = index[i];
                    index[i] = index[lsize];
                    index[lsize] = tmp;
                    lsize++;
                } else {
                    rightIndex[rsize] = index[i];
                    rsize++;
                }
            }
        }
        return lsize;
    }
}
