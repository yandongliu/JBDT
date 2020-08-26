package JBDT.regression.boosting;

import JBDT.data.Data;
import JBDT.data.Pair;
import JBDT.util.Config;
import JBDT.util.Util;

import java.util.ArrayList;


public class QLoss {

    public int nTrees;
    public int nnodes;
    int ncalls;
    ArrayList<Integer> idealRank;
    int loss_type;
    public double mse, bias, shrinkage=1.0;
    public double samplingRate =0.5, featureSamplingRate = 0.5;
    public double lamL1, lamL2;
    public double stepsize = 1.0;
    String progress_content;
    boolean useUniformBias = false;
    boolean isCompIdealRank = false;
    boolean isResort = false;
    double[] origWeights;
    String progress_header;
    int stepsizeMode;
    boolean gradient_descent_mode;// 0: gradient descent, 1: conjugate gradient descent
    ArrayList<Pair<Integer, Integer>> _nnodes;
    ArrayList<Pair<Integer, Double>> _shrinkage;

    public static final int REGRESSION = 1;
    public static final int LOGISTIC = 2;

    public QLoss(Config config) {
        this.ncalls = 0;
        this.loss_type = QLoss.REGRESSION;
        this.nTrees = config.getInt("regtree.ntrees",20);
        this.samplingRate = config.getDouble("regtree.samplingrate", 1.0);
        this.featureSamplingRate = config.getDouble("featureSamplingRate", 1.0);
//        this.nnodes = config.getInt("nnodes", 10);
        this.lamL1 = config.getDouble("lamL1", 1e-4);
        this.lamL2 = config.getDouble("lamL1",1e-4);
        this.useUniformBias = config.get("regtree.useUniformBias", "false").equals("false")?false:true;
        this.gradient_descent_mode = config.getBool("gradient_descent_mode", false);

        //support multiple nnodes in one line
        this._nnodes = new ArrayList<Pair<Integer, Integer>>();
        String s_nnodes = config.get("regtree.nnodes", "12");
        String[] ss_nnodes = s_nnodes.split(" ");
        for (int i = 0; i < ss_nnodes.length; i++) {
            String _a = ss_nnodes[i];
            _nnodes.add((new Pair<Integer, Integer>(i,Integer.parseInt(_a))));
        }
        this.nnodes = _nnodes.get(0).second;

        this._shrinkage = new ArrayList<Pair<Integer, Double>>();
        s_nnodes = config.get("regtree.shrinkage", "1.0");
        ss_nnodes = s_nnodes.split(" ");
        for (int i = 0; i < ss_nnodes.length; i++) {
            String _a = ss_nnodes[i];
            _shrinkage.add((new Pair<Integer, Double>(i,Double.parseDouble(_a))));
        }
        this.shrinkage = _shrinkage.get(0).second;

        System.out.println();
        Util.log("QLoss config:");
        Util.log("\t| gradient_descent_mode = " + gradient_descent_mode);
        Util.log("\t| ntrees = " +nTrees);
        if (_nnodes.size() == 1)
            Util.log("\t| nnodes = "+nnodes);
        else
            Util.log("\t| nnodes = "+Util.<Integer, Integer>PairVec2String(_nnodes));
        if (_shrinkage.size() == 1)
            Util.log("\t| shrinkage = "+shrinkage);
        else
            Util.log("\t| shrinkage = "+Util.<Integer, Double>PairVec2String(_shrinkage));

        Util.log("\t| samplginrate = " + samplingRate);
        Util.log("\t| reg.lamL1 = " + lamL1);
        Util.log("\t| reg.lamL2 = " + lamL2);

    }

    public QLoss() {
    }
    public String get_progress_header() { return progress_header;}

    public String get_progress_content() {
        return progress_content;
    }

    void compGradient(Data data, double[] predictions, double[] gradient) {
        ncalls++;

        if(_nnodes.size()>0 && ncalls>=_nnodes.get(0).first) {
            nnodes = _nnodes.get(0).second;
            if(ncalls>1) {
                Util.log("\t[GBDT] Changed nnodes to " + nnodes);
                _nnodes.remove(0);
            }
        }

        if(_shrinkage.size()>0 && ncalls>=_shrinkage.get(0).first) {
            shrinkage = _shrinkage.get(0).second;
            if(ncalls>1) {
                Util.log("\t[GBDT] Changed shrinkage to " + shrinkage);
                _shrinkage.remove(0);
            }
        }

        if (isCompIdealRank)
            compIdealRank(data);
    }

    double compStepsize(double g[], double f[], int n) {
        switch(stepsizeMode){
            case 0:
                return 1;
            case 1:
                double e = Util.cosine(g, f, n);
                double l2_g = Util.l2norm(g, n);
                double l2_f = Util.l2norm(f, n);
                if (l2_f == 0)
                    return 0;
                progress_content += "\t"+e+"\t"+(e*l2_g/l2_f);
                return e*l2_g/l2_f;
        }
        return 1;
    }
    void postGradient(Data data, double[] predictions, double[] gradient) {
    }

    void rebias(Data d, double[] gradient, double bias) {

    }


    void compIdealRank(Data data) {
        Util.die("Not fully implemented");
        ArrayList<ArrayList<Integer>> q2u = data._q2u;
        int q;
        double[] target = data._target;

        idealRank =new ArrayList<Integer>();

        if (idealRank.size() == 0) {
            // initialize the vector
            idealRank = new ArrayList<Integer>(data.nrows());
        }

        for (q = 0; q < q2u.size(); q++) {
            int j, k;
            ArrayList<Pair<Double, Integer>> topK = new ArrayList<Pair<Double, Integer>>(q2u.get(q).size());

            for (k = 0; k < q2u.get(q).size(); k++) {
                // sort document by ideal scores
                topK.set(k, new Pair<Double, Integer>(target[q2u.get(q).get(k)] + 1e-5 * k, k));
            }

            //TODO implement a reverse comparator
//            Arrays.sort(topK);

            // compute the best ideal rank of documents ranked below
            j = 0;
            for (k = 0; k < q2u.get(q).size(); k++) {
                while (k > topK.get(j).first && j < q2u.get(q).size())
                j++;
                idealRank.set(q2u.get(q).get(k), j);
            }
        }
    }
}
