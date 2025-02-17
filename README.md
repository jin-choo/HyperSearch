# HyperSearch: Prediction of New Hyperedges through Unconstrained yet Efficient Search

This repository contains the source code for the paper [HyperSearch: Prediction of New Hyperedges through Unconstrained yet Efficient Search]().

In this work, we propose HyperSearch, a search-based algorithm for hyperedge prediction that efficiently evaluates unconstrained candidate sets.
RASP incorporates two key components:
* **Empirically justified scores based on observations**: An empirically grounded scoring function derived from observations in real-world hypergraphs.
* **Efficient search with an anti-monotonic upper bound**: We derive and use an anti-monotonic upper bound of the original scoring function (which is not anti-monotonic) to prune the search space.

## Datasets

All datasets are available at this [link](https://www.cs.cornell.edu/~arb/data/) and [link](https://www.cs.cornell.edu/~arb/data/).

| Domain       | Dataset    |   # Nodes  | # Hyperedges | Time Range | Time Unit |
|--------------|------------|:----------:|:------------:|:----------:|:---------:|
| Email        | Enron      |    143     |    10,883    |     43     |  1 Month  |
|              | Eu         |    998     |   234,760    |     75     |  2 Weeks  |
| Contact      | High       |    327     |   172,035    |     84     |   1 Day   |
|              | Primary    |    242     |   106,879    |    649     |  6 Hours  |
| Tags         | math.sx    |   1,629    |   822,059    |     89     |  1 Month  |
|              | ubuntu     |   3,029    |   271,233    |    104     |  1 Month  |

## Requirements

To install requirements, run the following command on your terminal:
```setup
pip install -r requirements.txt
```

## RASP on Neuron Activity Datasets

To execute RASP on neuron activity datasets, run this command:

```
./run.sh
```

## RASP on an E-commerce Dataset

To execute RASP on an e-commerce dataset, run this command:

```
./run_case.sh
```

## Evaluation

To evaluate the result TSPs, run this command:

```
python main.py -a read_ndcg_rc_exp
```

## Reference

This code is free and open source for only academic/research purposes (non-commercial). If you use this code as part of any published research, please acknowledge the following paper.
```
```
