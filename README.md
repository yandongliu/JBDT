# Gradient Boosting Decision Trees

Java implementation of GBDT/GBRank.
This work is from when I was at Yahoo Labs back in 2013. Opening source it in case there is some value.

It supports two loss functions:
 * [GBDT](https://en.wikipedia.org/wiki/Gradient_boosting) (regression loss)
 * [GBRank](http://www.cc.gatech.edu/~zha/papers/fp086-zheng.pdf) (pairwise loss)

This software is provided as-is. You are very welcome to continue the work on top of this.

## Author
 * Yandong Liu (askjupiter at gmail)

## Build
```
./gradlew build
```
## Usage
Training:
   ```
    java -jar JBDT.jar --train config.txt
   ```
Testing:
   ```
    java -jar JBDT.jar --test config.txt
   ```
## Example runs
### Training
```
$ java -cp build/libs/JBDT.jar JBDT.JBDT --train examples/gbdt.cfg
2020/08/26 12:33:05: Training...
2020/08/26 12:33:05: loading feature/label data.
examples/sampledata/data.fv
label col:50
2020/08/26 12:33:06:

2020/08/26 12:33:06: Train config:
2020/08/26 12:33:06:    | model_name = gbdt
2020/08/26 12:33:06:    | output_dir = examples/result
2020/08/26 12:33:06:    | fv_file = sampledata/data.fv
2020/08/26 12:33:06:    | label = label
2020/08/26 12:33:06:    | qu_file = null
2020/08/26 12:33:06:    | train.exclude_features = feature1,feature2
2020/08/26 12:33:06:    | learner = gbdt
2020/08/26 12:33:06:

2020/08/26 12:33:06: Tree splitting config:
2020/08/26 12:33:06:    | splitcmp.tau = 1.0E-10
2020/08/26 12:33:06:    | min_node_sample = 2
2020/08/26 12:33:06:    | min_balance_factor = 0

2020/08/26 12:33:06: QLoss config:
2020/08/26 12:33:06:    | gradient_descent_mode = false
2020/08/26 12:33:06:    | ntrees = 100
2020/08/26 12:33:06:    | nnodes = 20
2020/08/26 12:33:06:    | shrinkage = 0.08
2020/08/26 12:33:06:    | samplginrate = 1.0
2020/08/26 12:33:06:    | reg.lamL1 = 1.0E-4
2020/08/26 12:33:06:    | reg.lamL2 = 1.0E-4

2020/08/26 12:33:06: RLoss config:
2020/08/26 12:33:06:    | bias = 0.0
2020/08/26 12:33:06:    | tau = 0.0
2020/08/26 12:33:06:    | mode = 0
2020/08/26 12:33:06:    | scale = 1.0
2020/08/26 12:33:06:    | loss = regression

Gradient Boosting:
        Tree #  null    Time    ETF     Memory
        1       2.64358 0.0     99      28577k
2020/08/26 12:33:06:    [GBDT] Changed nnodes to 20
2020/08/26 12:33:06:    [GBDT] Changed shrinkage to 0.08
        2       2.28532 0.0     98      23035k
        3       1.98013 0.0     97      59220k
        4       1.72167 0.0     96      93290k
        ...
        100     0.06776 0.0     0       158501k

FEATURE IMPORTANCE:
RANK    FEATURE IMPORTANCE

1       F49     100.0000
2       F109    91.8546
3       F55     79.5483
4       F34     67.3158
...
```
### Testing
```
$ java -cp build/libs/JBDT.jar JBDT.JBDT --test examples/gbdt.cfg
2020/08/26 12:37:27: Testing...
regression
examples/sampledata/data.fv
label col:50
2020/08/26 12:37:27:

2020/08/26 12:37:27: Test config:
2020/08/26 12:37:27:    | model_name = gbdt
2020/08/26 12:37:27:    | model_dir = examples/result
2020/08/26 12:37:27:    | output_dir = examples/result
2020/08/26 12:37:27:    | fv_file = sampledata/data.fv
2020/08/26 12:37:27:    | loss = regression
2020/08/26 12:37:27:    | label = label
2020/08/26 12:37:27: Output examples/result/gbdt.100.score
test finishes
```

## Input
### Config file
Starting point of the toolkit. A sample one might look like:
```
# TRAINING INPUTS
train.fv_file = sampledata/data.fv          # training file
label = label # label column

# TESTING INPUTS
test.fv_file = sampledata/data.fv

# MODELING
learner = gbdt       # you can choose your algorithm between: gbdt,gbrank
model_name = gbdt     # your model file name

# OUTPUT DIR
output_dir = examples/result   # the directory containing your model and result files. The default is './output'

# REGRESSION (these parameters are shared by many algorithms including dtree,rtree,sdcg,gbrank)
regtree.ntrees = 100           # number of trees
regtree.nnodes = 20           # number of leaf nodes per tree
regtree.shrinkage = 0.08      # learning rate
regtree.samplingrate = 1.0    # sampling rate
```
### Feature file(.fv)
File format:
```
feature1, feature2, ..., label
```
### Query-URL file(.quj)
You can provide a query-url pair file if you are training a model to rank search result:
```
query url grade
```
## Output
### Feature importance file (feature.imp)
```
RANK	FEATURE	IMPORTANCE

1	F49	100.0000
2	F109	91.8546
3	F55	79.5483
...
```
### XML Model (mode.xml)
```
<?xml version="1.0" encoding="UTF-8"?>
<MlrFunction name="CHANGE THIS" featuredef="featuredefs.xml" version="1.0">

  <!-- ADD SCORE STANDARDIZATION OR CALIBRATION HERE -->

    <DecisionTree loss="regression">

    <!-- ADD EARLY EXIT HERE -->

        <Forest>
            <Tree id="0">
                 <Node feature="F55" value="2.13234575" id="N0_-1">
                     <Node feature="F34" value="1.89551985" id="N0_-1">
                         ...
                     </Node>
                 </Node>
            </Tree>
        </Forest>
    </DecisionTree>
</MlrFunction>
```
### Score file(.score)
After you apply the model to a feature file.

## License
MIT

## Acknowledgments
 * Yahoo Labs
