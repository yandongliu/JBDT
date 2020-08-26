package JBDT.classifier.tree;

import JBDT.data.SampleData;

public class TauCmp extends SplitCmp {
    double tau;

    public TauCmp() {
        this.tau = 1e-10;
    }

    public TauCmp(double tau) {
        this.tau = tau;
    }

    @Override
    public boolean better(Split a, Split b, SampleData _sd){
        double gainhigh = b._gain*(1+tau);
        double gainlow = b._gain*(1-tau);
        return (a._gain > gainhigh || (a._gain >= gainlow ));// && b._featn > a._featn));
    }
}