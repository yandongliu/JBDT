package JBDT.regression.boosting;

import JBDT.data.Data;
import JBDT.data.Pair;
import JBDT.util.Config;
import JBDT.util.Util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

class pIterator implements Iterator {

    Data _data = null;
    ArrayList<Pair<Pair<Integer,Integer>, Double>> _preference;
    Pair<Pair<Integer, Integer>, Double> _readOnly;
    double[] _target;
    int _q = 0, _k = 0, _j = 0;
    boolean _isEnd = false;

    public pIterator(Data data) {
        this._data = data;
        Pair<Integer, Integer> _pair = new Pair<Integer, Integer>(0,0);
        this._readOnly = new Pair<Pair<Integer, Integer>, Double>(_pair, 0.0);
        _target = _data._target;

        if (_preference == null){
            _j = 0;
//            next();
        }
    }

    public pIterator(ArrayList<Pair<Pair<Integer,Integer>, Double>> preference) {
        this._preference = preference;
        if (_preference.size() > 0)
            _readOnly = preference.get(0);
    }

    @Override
    public boolean hasNext() {
        return !_isEnd;
    }

    @Override
    public Pair<Pair<Integer, Integer>, Double> next() {
        if (_preference!=null){
            //get next pref pair
            _k ++;
            _isEnd = (_k >= _preference.size());
            if (! _isEnd)
            _readOnly = _preference.get(_k);
        }else{
            _j ++;
            while(_j >= _data._q2u.get(_q).size()){
                _k ++; //counter of urls of one query
                if (_k+1 >= _data._q2u.get(_q).size()){
                    _q ++; //counter for query
                    _k = 0;
                    if (_q >= _data._q2u.size()){
                        _isEnd = true;
                        break;
                    }
                }
                _j = _k + 1;
            }

            if (! _isEnd){
                _readOnly.first.first = _data._q2u.get(_q).get(_k);
                _readOnly.first.second = _data._q2u.get(_q).get(_j);
                _readOnly.second = _target[_readOnly.first.first] - _target[_readOnly.first.second];
            }
        }

        return _readOnly;
    }

    @Override
    public void remove() {
    }
}

public class GBRankLoss extends QLoss {

    double[] gradient_weights;
    ArrayList<Pair<Pair<Integer,Integer>, Double>> preference;
    double[] interPWeight;
    double[] intraPWeight;
    int init_phase;
    int mode;
    boolean isRankWeighting;
    double tau, alpha, tied_wt;

    public  GBRankLoss(Config config) {
        super(config);
        config.put("loss", "regression");
        preference = new ArrayList<Pair<Pair<Integer, Integer>, Double>>();
        tau = config.getDouble("gbrank.tau", 1);
        alpha = config.getDouble("gbrank.alpha", 0.0);
        tied_wt = config.getDouble("gbrank.tied_wt", 0.0);

        mode = config.getInt("gbrank.mode", 0);
        init_phase = config.getInt("gbrank.init_phase", 0);

        Util.log("GBRank config:\n");
        Util.log("\t| tau = " + tau);
        Util.log("\t| alpha = " + alpha);
        Util.log("\\t| tied_wt = "+tied_wt);

        if(mode!=0)
            Util.log("\t| mode = "+mode);
        if(init_phase>0)
            Util.log("\t| init_phase = "+init_phase);

        isRankWeighting = config.getBool("gbrank.useRankWeighting", false);
        if(isRankWeighting) {
            isResort = true;
            isCompIdealRank = true;
        }
        gradient_weights = new double[0];

        String pFile = config.get("gbrank.pfile", "");
        String quFile = config.get("train.qu_file", "");

        if (!(pFile.equals(""))){
            // Read pair file
            Util.log("\t| pfile = " +pFile);
            if(quFile.equals("")) {
                Util.die(quFile+" error");
            }
            if(!readPreference(pFile, quFile)) {
                Util.log("Error reading "+pFile);
            }
        }

        String pwfile = config.get("gbrank.pwfile", "");
        String intraPWfile = config.get("gbrank.intrapwfile", pwfile);
        String interPWfile = config.get("gbrank.interpwfile", "");

        intraPWeight = new double[0];
        interPWeight = new double[0];

        if (!(intraPWfile.equals(""))){
            Util.log("\t intrapwfile = " + intraPWfile );
            if(!readPWeight(intraPWeight, intraPWfile)) {
                Util.die("Error reading "+intraPWfile+"!");
            }
        }
        if (!(interPWfile.equals(""))){
            Util.log("\t | interpwfile = " + interPWfile);
            if(!readPWeight(interPWeight, interPWfile)) {
                Util.die("Error reading "+interPWfile);
            }
        }

        progress_header="pairs";
        if (stepsizeMode == 1)
            progress_header += "\tcosine\tstepsize";
    }

    @Override
    void compGradient(Data data, double[] prediction, double[] gradient) {
        super.compGradient(data, prediction, gradient);

        double[] target = data._target;
        double[] weight = data._weight.getVec();

        if (gradient_weights.length < data.nrows()) {
            gradient_weights = new double[data.nrows()];
        }


        double g, w1, w2, gdiff, intraPW, interPW;
        int j;
        int url1, url2, num_swapped;

        System.out.println("ncalls:"+ncalls);

        if (ncalls <= init_phase){
            // train regression trees in the initial phase
            double e = 0;
            for(j = 0; j < data.nrows(); j ++){
                gradient[j] = target[j] - prediction[j];
                e += gradient[j]*gradient[j];
            }
            progress_content = e/data.nrows()+"";
            return;
        }

        for (j = 0; j < data.nrows(); j ++){
            gradient_weights[j] = 0;
            gradient[j] = 0;
        }

        num_swapped = 0;
        pIterator it;
        if (preference.size() > 0){
            it = new pIterator(preference);
        } else {
            it = new pIterator(data);
        }

        int k = 0;
        while(it.hasNext()) {
            Pair<Pair<Integer,Integer>, Double> pref = it.next();
            url1 = pref.first.first;
            url2 = pref.first.second;
            gdiff = pref.second;

            if (gdiff == 0 && tied_wt == 0)
            continue;
            k++;

            intraPW = 1; interPW = 1;
            if (intraPWeight.length > k)
                intraPW = intraPWeight[k];
            if (interPWeight.length > k)
                interPW = interPWeight[k];
            w1 = weight[url1] * intraPW;
            w2 = weight[url2] * intraPW;

            g = prediction[url1] - gdiff*tau - prediction[url2];

            if (gdiff == 0){
                gradient[url1] -= interPW*tied_wt*w2*g;
                gradient[url2] += interPW*tied_wt*w1*g;

                if (mode == 1){
                    gradient_weights[url1] += w2;
                    gradient_weights[url2] += w1;
                }
            } else if (g*gdiff < 0 ){
                // conflicting pairs
                gradient[url1] -= interPW*w2*g;
                gradient[url2] += interPW*w1*g;

                if (mode == 1){
                    gradient_weights[url1] += w2;
                    gradient_weights[url2] += w1;
                }
            }

            if ((prediction[url1] - prediction[url2])*gdiff <= 0 && gdiff != 0)
            num_swapped ++;

            if (mode != 1){
                gradient_weights[url1] += w2;
                gradient_weights[url2] += w1;
            }
        }

        // Regression regularization
        if(alpha>0) {
            Util.die("alpha not supported");
            for(j=0; j < data.nrows();j++) {
                //TODO
                if(!data._vRaQ.contains(data._u2q.get(j))) {
                    gradient[j] += alpha*weight[j]*(target[j]-prediction[j]);
                    gradient_weights[j] += alpha*weight[j];
                }
            }
        }

        // Weighted Average
        for (j = 0; j < data.nrows(); j ++){
            if (gradient_weights[j] > 0)
                gradient[j] = (gradient[j] / gradient_weights[j]);
            else
                gradient[j] = 0;
        }

        if (mode == 1){
            weight = data._weight.getVec();
            for (j = 0; j < data.nrows(); j ++)
                weight[j] = origWeights[j] * gradient_weights[j];
        }

        // adjust the gradients
        if (isRankWeighting){
            for(j = 0; j < data.nrows(); j ++){
                gradient[j] *= Math.log(2) / Math.log(idealRank.get(j) + 2) * 3;
            }
        }

        if (ncalls == 1)
            num_swapped = k; // k is the total number of preferences

        progress_content = num_swapped+"";
        // Post Gradient Adjustment
        postGradient(data, prediction, gradient);

    }

    boolean readPreference(String pFile, String quFile) {
        try {

            BufferedReader br_qu = new BufferedReader(new FileReader(quFile));
            BufferedReader br_p = new BufferedReader(new FileReader(pFile));

            String line = "";

            // Read query-url pairs first
            HashMap<String, Integer> queryurl = new HashMap<String, Integer>();


            // Get Headers
            line = br_qu.readLine();
            String[] tokens = Util.tokenize(line, "\t");
            int nTokens = tokens.length;

            int qColId = -1;
            int gColId = -1;
            int uColId = -1;
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equals("query")) {
                    qColId = i;
                } else if (tokens[i].equals("url"))
                    uColId = i;
                else if (tokens[i].equals("group"))
                    gColId = i;
                if (gColId != -1 && qColId != -1 && uColId != -1)
                    break;
            }

            if (qColId == -1) {
                Util.die("[ERROR] query not in " + quFile);
            }
            if (uColId == -1) {
                Util.die("[ERROR] url not in " + quFile);
            }

            int lineno = 1;

            while ((line = br_qu.readLine()) != null) {
                lineno++;

                tokens = Util.tokenize(line, "\t");
                if (tokens.length != nTokens) {
                    Util.die("[ERROR] Wrongly formatted qu file at row " + lineno);
                }

                String uniqQU = tokens[qColId] + "\t" + tokens[uColId];
                if (gColId != -1)
                    uniqQU = tokens[gColId] + "\t" + uniqQU;

                queryurl.put(uniqQU, lineno - 2);//starting from 0
            }
            br_qu.close();

            // Read preference data
            // Get Headers
            qColId = -1;
            gColId = -1;
            uColId = -1;
            int u2ColId = -1;
            int gdiffColId = -1;
            line = br_p.readLine();
            tokens = Util.tokenize(line, "\t");

            nTokens = tokens.length;
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equals("query"))
                    qColId = i;
                else if (tokens[i].equals("url1"))
                    uColId = i;
                else if (tokens[i].equals("url2"))
                    u2ColId = i;
                else if (tokens[i].equals("gdiff"))
                    gdiffColId = i;
                else if (tokens[i].equals("group"))
                    gColId = i;
                if (gColId != -1 && qColId != -1 && uColId != -1 && u2ColId != -1 && gdiffColId != -1)
                    break;
            }

            if (qColId == -1) {
                Util.die("[ERROR] query not in " + pFile);
            }
            if (uColId == -1) {
                Util.die("[ERROR] url1 not in " + pFile);
            }
            if (u2ColId == -1) {
                Util.die("[ERROR] url2 not in " + pFile);
            }
            if (gdiffColId == -1) {
                Util.die("[ERROR] gdiff not in " + pFile);
            }
            lineno = 1;
            while ((line = br_p.readLine()) != null) {
                lineno++;

                String query, url1, url2;
                int idx1, idx2;
                double gdiff = 0;
                tokens = Util.tokenize(line, "\t");

                if (tokens.length != nTokens) {
                    Util.die("[ERROR] Wrongly formatted preference file at row " + lineno);
                }

                query = tokens[qColId];
                url1 = tokens[uColId];
                url2 = tokens[u2ColId];
                gdiff = Double.parseDouble(tokens[gdiffColId]);

                String qu1 = query + '\t' + url1, qu2 = query + '\t' + url2;
                if (gColId != -1) {
                    qu1 = tokens[gColId] + "\t" + qu1;
                    qu2 = tokens[gColId] + "\t" + qu2;
                }

                if (!queryurl.containsKey(qu1)) {
                    Util.die("[ERROR] Can't find " + qu1);
                }

                if (!queryurl.containsKey(qu2)) {
                    Util.die("[ERROR] Can't find " + qu2);
                }

                idx1 = queryurl.get(qu1);
                idx2 = queryurl.get(qu2);
                preference.add(new Pair<Pair<Integer, Integer>, Double>(new Pair<Integer, Integer>(idx1, idx2), gdiff));
            }
            br_p.close();

            Util.log("Loaded " + preference.size() + " pairs of preference data from " + pFile);

            return true;
        } catch ( IOException ex) {
            ex.printStackTrace();;
            return false;
        }
    }

    boolean readPWeight(double[] pweight, String pwfile){
        pweight = new double[preference.size()];

        int len =  loadVector(pwfile, pweight, preference.size(), true);
        if (len < 0)
            return false;

        return len == preference.size();
    }

    int loadVector(String dataFile, double[] data, int max, boolean hasHeader) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(dataFile));
            if (hasHeader) {
                br.readLine();
            }

            int i;
            String line = null;
            for (i = 0; i < max && (line=br.readLine())!=null; i++)
                data[i] = Double.parseDouble(line);

            if (br.readLine()==null)
                i = i - 1;
            br.close();
            Util.log("Loaded " + i + " units from " + dataFile );
            return i;
        } catch ( IOException ex) {
            Util.die("[ERROR] Can't open vector file " + dataFile);
            return -1;
        }
    }
}
