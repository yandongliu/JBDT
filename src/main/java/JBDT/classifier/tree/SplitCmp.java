package JBDT.classifier.tree;

import JBDT.data.SampleData;

public abstract class SplitCmp {
    public boolean better(Split a, Split b, SampleData _sd) {return a._gain > b._gain;};
}
