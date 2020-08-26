package JBDT.regression.boosting;

import JBDT.data.Data;
import JBDT.util.Config;
import JBDT.util.Util;

public class RLoss extends QLoss {

    public double _uniformBias = 0, tau = 0;
    int mode; // mode = 1 means multiplying scale to the input target values
    double scale;


    public RLoss(Config config) {
        super(config);

        String lossname = config.get("regtree.loss", "regression");

        if(lossname.equals("regression")) {
            this.loss_type = REGRESSION;
        } else if(lossname.equals("logistic")) {
            this.loss_type = LOGISTIC;
        } else {
            Util.die("Unknown loss:"+lossname);
        }

        mode = config.getInt("regtree.mode",0);
        scale = config.getDouble("regtree.scale",1.0);
//        config.loss = lossname;
        config.put("loss", "regression");
        System.out.println();
        Util.log("RLoss config:");
        Util.log("\t| bias = " + bias );
        Util.log("\t| tau = " + tau );
        Util.log("\t| mode = " + mode );
        Util.log("\t| scale = " + scale );
        Util.log("\t| loss = " + lossname );
    }

    @Override
    void compGradient(Data data, double[] predictions, double[] gradient) {
        super.compGradient(data,predictions,gradient);
        double[] target = data._target;
        double r = 0.0, mse = 0.0;
        if(this.loss_type == REGRESSION) {
//            bias = 0.0;
            for (int i = 0; i < data._nrows; i++) {
                r = target[i] - predictions[i] - _uniformBias;
                gradient[i] = r;
                mse += r * r;
            }
        } else if (this.loss_type == LOGISTIC) {
            for (int i = 0; i < data._nrows; i++) {
                double y = target[i];
                r = 0.5*y/(1.0+Math.exp(y*predictions[i]));
                gradient[i] = r;
                mse += r * r;
            }
        } else {
            System.out.println("Unknown QLoss function.");
        }

        if(bias > 0)
            rebias(data, gradient, bias);

        // tolerance
        if (tau > 0) {
            for (int j = 0; j < data._nrows; j++) {
                if (Math.abs(gradient[j]) < tau) {
                    gradient[j] = 0;
                }
            }
        }

        // update uniformBias
        if (useUniformBias){
            double s = 0;
            for(int j = 0; j < data._nrows; j ++){
                s += gradient[j];
            }
            _uniformBias += s/data._nrows;
        }

        progress_content = ""+String.format("%.5f",mse/data._nrows);

        postGradient(data, predictions, gradient);
    }
}
