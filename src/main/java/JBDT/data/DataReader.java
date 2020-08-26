package JBDT.data;


import JBDT.classifier.tree.Feature;
import JBDT.util.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DataReader {

    public static void main(String[] args) {
        try {
            Data d = new Data();
            HashSet<String> featureExcl = new HashSet<String>();
            DataReader.readFeatureFile("/Users/yandong/dev/ml/gbdt/data/sampledata/data.fv", "\t", featureExcl, "label", d, "");
            System.out.println("#features:"+d.nFeatures);
            for (Feature f:d.features) {
                System.out.println(f._id + " " + f._name);
            }
            System.out.println();
            System.out.println("#rows:"+d._nrows);
            for (int i = 0; i < d._nrows; i++) {
                System.out.println(i+":"+d._target[i]);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static int countLines(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return (count == 0 && !empty) ? 1 : count;
        } finally {
            is.close();
        }
    }

    public static void readQujFile(String fn, String label, Data data, String config_dir) throws IOException {
        if (!fn.startsWith("/")) {
            fn = config_dir+"/"+fn;
        }
        BufferedReader br = new BufferedReader(new FileReader(fn));
        String[] aa = br.readLine().split("\t");
        int ncols = aa.length;
        int colLabel = -1;
        for (int i = 0; i < ncols; i++) {
            if (aa[i].toUpperCase().equals(label.toUpperCase())) {
                colLabel = i;
                break;
            }
        }
        assert  colLabel!=-1;
        if(colLabel == -1) {
            Util.die("no judgement column.");
        }

        String s;
        // read content into data, targets
        int nRow = 0;
        while ((s = br.readLine()) != null) {
            if(nRow>=data._nrows) {
                Util.die("lines not same.");
            }
            aa = s.split("\t");
            try {
                data._target[nRow] = Double.parseDouble(aa[colLabel]);
            } catch (NumberFormatException ex) {
                System.out.println("format error");
            }
            ++nRow;
        }
        br.close();

    }

    static double[] getTarget(Data data) {
        return data._target;
    }

    public static void readQu(String quFile, String nvqFile, Data data, String config_dir) {
        data._q2u = new ArrayList<ArrayList<Integer>>();
        data._u2q = new ArrayList<Integer>();
        data._qList = new ArrayList<String>();

        ArrayList<ArrayList<Integer>> _q2u = data._q2u;
        ArrayList<Integer> _u2q = data._u2q;
        ArrayList<String> _qList = data._qList;

        data._vNvQ = new ArrayList<Boolean>();
        data._vRaQ = new ArrayList<Boolean>();
        data._vUnjudgedQ = new ArrayList<Boolean>();
        data._vQrySampled = new ArrayList<Boolean>();
        ArrayList<Boolean> _vNvQ = data._vNvQ;
        ArrayList<Boolean> _vRaQ = data._vRaQ;
        ArrayList<Boolean> _vUnjudgedQ = data._vUnjudgedQ;
        ArrayList<Boolean> _vQrySampled = data._vQrySampled;


        HashMap<String, Integer> qMap = new HashMap<String, Integer>();

        if (!quFile.startsWith("/")) {
            quFile = config_dir+"/"+quFile;
        }

        try {
            BufferedReader br_qu = new BufferedReader(new FileReader(quFile));

            String line = br_qu.readLine();

            int qIdx, nQu = 0;

            String[] tokens = Util.tokenize(line, "\t");

            int qColId = -1;
            int gColId = -1;
            int nTokens = tokens.length;
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equals("query"))
                    qColId = i;
                else if (tokens[i].equals("group"))
                    gColId = i;
                if (gColId != -1 && qColId != -1)
                    break;
            }

            if (qColId == -1) {
                Util.die("[ERROR] query not in " + quFile);
            }

            int lineno = 1;
            while ((line = br_qu.readLine()) != null) {
                // clear line
                lineno++;

                tokens = Util.tokenize(line, "\t");

                if (tokens.length != nTokens) {
                    Util.die("[ERROR] " + quFile + " has wrong format at line " + lineno);
                }

                String Q = tokens[qColId];

                String uniqQ = Q;
                if (gColId != -1)
                    uniqQ = tokens[gColId] + "." + uniqQ;

                if (!qMap.containsKey(uniqQ)) {
                    qIdx = nQu++;
                    qMap.put(uniqQ, qIdx);
                    _q2u.add(new ArrayList<Integer>());
                    _qList.add(Q);
                } else {
                    qIdx = qMap.get(uniqQ);
                    if (qIdx != nQu - 1) {
                        Util.die("[ERROR] The training data is NOT sorted!!!");
                        Util.die(quFile + " has unsorted uniqQ at line " + lineno);
                        Util.die("uniqQ=" + uniqQ);
                        Util.die("existing uniqQ=" + uniqQ + " with qMap position=");
                    }
                }

                _q2u.get(qIdx).add(lineno - 2);
                _u2q.add(qIdx);
            }
            br_qu.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            Util.die("ERROR "+quFile);
        }

        Util.log("Read " + _q2u.size() + " queries and " + _u2q.size() + " qu pairs (" + String.format("%.4f",(double)_u2q.size() / _q2u.size()) + " u/q from " + quFile);

        assert(data._nrows == _u2q.size());


        _vQrySampled = new ArrayList<Boolean>(_q2u.size());

        for (int i = 0; i < _vQrySampled.size(); i++) {
            _vQrySampled.set(i, true);
        }

        _vUnjudgedQ = new ArrayList<Boolean>(_q2u.size());

        // mark navigationsl queries
        _vNvQ = new ArrayList<Boolean>(_q2u.size());
        int nNvQs = 0, nLines = 0;
        if (nvqFile !=null) {
            try {
                BufferedReader br_nvq = new BufferedReader(new FileReader(nvqFile));
                String line = null;

                while ((line = br_nvq.readLine()) != null) {

                    String Q = line;
                    if (Q.length() > 0) {
                        ++nLines;
                        if (qMap.containsKey(Q)) {
                            _vNvQ.set(qMap.get(Q), true);
                            ++nNvQs;
                        }
                    }
                }

                br_nvq.close();
            } catch ( IOException ex) {
                ex.printStackTrace();
                Util.die("ERROR "+nvqFile);
            }

            Util.log("read " + nLines + "queries from " + nvqFile);
            Util.log(", among which " + nNvQs + " are in " + quFile + ".");
        }

        // mark non-regular queries
        _vRaQ = new ArrayList<Boolean>(_q2u.size());
        int n_non_regular_queries = 0;
        int n_non_regular_qurls = 0;
        double[] target = getTarget(data);
        for(int q=0; q<_q2u.size(); q++) {
            double val = target[_q2u.get(q).get(0)];
            if(val != 0 && val != 1 && val != 2 && val != 3 && val != 4) {
                _vRaQ.set(q, true);
                n_non_regular_queries ++;
                n_non_regular_qurls += _q2u.get(q).size();
            }
        }
        if(n_non_regular_queries>0)
            Util.log("There are " + n_non_regular_queries + " non-regular queries and "
                    + n_non_regular_qurls + " non-regular qurls in training data!" );
    }

    public static void readFeatureFile(
            String fn, String delimiter, HashSet<String> feature_excl, String label, Data data, String config_dir
    ) throws IOException {
        if (!fn.startsWith("/")) {
            fn = config_dir + "/" + fn;
        }

        int nRows = DataReader.countLines(fn);
        --nRows;
        if(nRows<=2) {
            System.out.println("No data!");
            System.exit(1);
        }

        data._nrows = nRows;

        HashSet<Integer> featureIndexSkip = new HashSet<Integer>();

        BufferedReader br = new BufferedReader(new FileReader(fn));
        String[] rawline = br.readLine().split(delimiter);
        int nFeatExcl = 0;

        //get skipping features that exist in this dataset
        for (int i = 0; i < rawline.length; i++) {
            if(feature_excl.contains(rawline[i].trim().toLowerCase())) {
                featureIndexSkip.add(i);
                nFeatExcl++;
            }
        }
        String[] aa = new String[rawline.length - nFeatExcl];
        int ncols = rawline.length - nFeatExcl; //real # of columns

        data.nFeatures = ncols - 1; //assume one column is 'label'. all others are feature columns
        data.featureIds = new int[ncols-1];
        data.v = new double[ncols-1][];
        data._target = new double[nRows];

        data.features = new Feature[data.nFeatures];

        int cnt = 0;
        for (int i = 0; i < rawline.length; i++) {
            if(featureIndexSkip.contains(i)) {
                Util.log("Skipping feature:"+rawline[i]);
                continue;
            }
            aa[cnt++] = rawline[i];
        }

        int featCnt = 0;
        for (int i = 0; i < aa.length; i++) {
            //one todo here is to first check is label column exists
            if (!aa[i].toUpperCase().equals(label.toUpperCase())) {
                data.featureIds[featCnt] = i; //so feature id is col index
                data.v[featCnt] = new double[nRows];
                data.features[featCnt] = new Feature(i, aa[i], data.v[featCnt]);
                data.mapFeatureId2Feature.put(i, data.features[featCnt]);
                featCnt++;
            }
            else {
                data._labelCol = i;
            }
        }


        if (data._labelCol == -1) {
            System.out.println("No label column!");
            br.close();
            System.exit(1);
        }
        System.out.println("label col:" + data._labelCol);

        String s;
        // read content into data, targets
        int nRow = 0;
        while ((s = br.readLine()) != null) {
            rawline = s.split(delimiter);

            cnt = 0;
            for (int i = 0; i < rawline.length; i++) {
                if(featureIndexSkip.contains(i)) {
                    continue;
                }
                aa[cnt++] = rawline[i].trim();
            }

            try {
                for (int i = 0; i < data.nFeatures; i++) {
                    try {
                        data.v[i][nRow] = Double.parseDouble(aa[data.featureIds[i]]);
                    } catch (NumberFormatException ex) {
                        System.out.println("feature data format error. are you using categorical features?");
                        data.v[i][nRow] = 0.0;
                    }
                }
                data._target[nRow] = Double.parseDouble(aa[data._labelCol]);
            } catch (NumberFormatException ex) {
                System.out.println("format error");
            }
            ++nRow;
        }
        br.close();
        data.genIdx();
        if(data._weight == null) {
            data.setWeightAll(1.0);
        }
    }
}
