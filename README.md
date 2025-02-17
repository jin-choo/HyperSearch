# HyperSearch: Prediction of New Hyperedges through Unconstrained yet Efficient Search

This repository contains the source code for the paper [HyperSearch: Prediction of New Hyperedges through Unconstrained yet Efficient Search]().

In this work, we propose HyperSearch, a search-based algorithm for hyperedge prediction that efficiently evaluates unconstrained candidate sets, by incorporating two key components:
* **Empirically justified scores based on observations**: An empirically grounded scoring function derived from observations in real-world hypergraphs.
* **Efficient search with an anti-monotonic upper bound**: We derive and use an anti-monotonic upper bound of the original scoring function (which is not anti-monotonic) to prune the search space.

## Datasets

All datasets are available at this [link](https://www.cs.cornell.edu/~arb/data/) and this [link](https://drive.google.com/drive/folders/1KKwkrZ2mMcc098pqwtpQrByWmTEigwzC?usp=sharing).

| Domain       | Dataset    |   # Nodes  | # Hyperedges | Timestamps |
|--------------|------------|:----------:|:------------:|:----------:|
| Co-citation  | Citeseer   |    1,457   |    1,078     |            |
|              | Cora       |    1,434   |   1,579      |            |
| Authorship   | Cora-A     |    2,388   |    1,072     |            |
|              | DBLP-A     |    39,283  |   16,483     |            |
| Email        | Enron      |    143     |    10,883    |     ✔     |
|              | Eu         |    998     |   234,760    |     ✔     |
| Contact      | High       |    327     |   172,035    |     ✔     |
|              | Primary    |    242     |   106,879    |    ✔      |
| Tags         | math.sx    |   1,629    |   822,059    |     ✔     |
|              | ubuntu     |   3,029    |   271,233    |    ✔      |

## Execution

To execute HyperSearch, run this command:

```
mvn compile -B
mvn exec:java -Dexec.args="dataset name"
mvn exec:java -Dexec.args="citeseer"
```

## Observations

To get the results from the observations in real-world hypergraphs, run this command:

```
python observation_1.py
python observation_2.py
```

## Reference

This code is free and open source for only academic/research purposes (non-commercial). If you use this code as part of any published research, please acknowledge the following paper.
```
```
