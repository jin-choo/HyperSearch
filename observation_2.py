import torch
from collections import defaultdict
import numpy as np
from tqdm import tqdm

def compute_hyperedge_size_dist(hyperedge_list, dataset):
    hyperedge_size_dist = defaultdict(int)
    for hyperedge in set(frozenset(edge) for edge in hyperedge_list):
        hyperedge_size_dist[len(hyperedge)] += 1
    if 1 in hyperedge_size_dist:
        del hyperedge_size_dist[1]
    total = sum(dist for dist in hyperedge_size_dist.values())
    if dataset in ['dblpA', 'email-Eu']:
        for size in [size for size in hyperedge_size_dist.keys() if size > 5]:
            del hyperedge_size_dist[size]
    else:
        for size in [size for size, dist in hyperedge_size_dist.items() if (size > 5 and float(dist) / total < 0.01) or size > 10]:
            del hyperedge_size_dist[size]
    total = sum(hyperedge_size_dist.values())
    
    return {size: float(dist) / total for size, dist in hyperedge_size_dist.items()}

dataset_list = ['email-Enron', 'email-Eu', 'contact-high-school', 'contact-primary-school', 'tags-ask-ubuntu', 'tags-math-sx']

y = [[[] for _ in range(5)] for _ in range(len(dataset_list))]
for i_dataset, dataset in enumerate(dataset_list):
    print(dataset)
    data_split = torch.load(f'./data/splits/{dataset}.pt')
    hyperedge_list = [set(he) for he in data_split['hyperedge_list']]
    he_size = compute_hyperedge_size_dist(hyperedge_list, dataset).keys()

    hyperedge_size_list = []
    time_list = data_split['time_list']
    time_size_list = []
    for i_hyperedge, hyperedge in enumerate(hyperedge_list):
        if len(hyperedge) in he_size:
            hyperedge_size_list.append(hyperedge)
            time_size_list.append(time_list[i_hyperedge])

    time_split = np.array_split(time_size_list, 5)

    hyperedge_split = [[] for _ in range(5)]
    for i_hyperedge, hyperedge in enumerate(hyperedge_size_list):
        for i_split, time_split_ in enumerate(time_split):
            if time_split_[0] <= time_size_list[i_hyperedge] <= time_split_[-1]:
                hyperedge_split[i_split].append(hyperedge)
                break

    for group1 in range(5):
        for he1 in tqdm(hyperedge_split[group1]):
            len_he1 = len(he1)
            for group2 in range(group1, 5):
                sum_overlap_ratio = 0
                count = 0
                for he2 in hyperedge_split[group2]:
                    sum_overlap_ratio += len(he1 & he2) / len_he1
                    count += 1
                y[i_dataset][group2-group1].append(sum_overlap_ratio / count)
    
    for k in range(5):
        y[i_dataset][k] = np.mean(y[i_dataset][k])
    
    print(y)