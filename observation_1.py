import torch
from collections import defaultdict
import numpy as np
import random
from collections import Counter


def hyperedge_size_dist(hyperedge_list, dataset):
    hyperedge_size_dist = defaultdict(int)
    for hyperedge in set(frozenset(edge) for edge in hyperedge_list):
        hyperedge_size_dist[len(hyperedge)] += 1
    if 1 in hyperedge_size_dist:
        del hyperedge_size_dist[1]
    total = sum(hyperedge_size_dist.values())
    if dataset in ['dblpA', 'email-Eu']:
        for size in [size for size in hyperedge_size_dist.keys() if size > 5]:
            del hyperedge_size_dist[size]
    else:
        for size in [size for size, dist in hyperedge_size_dist.items() if (size > 5 and float(dist) / total < 0.01) or size > 10]:
            del hyperedge_size_dist[size]
    total = sum(hyperedge_size_dist.values())
    for size, dist in hyperedge_size_dist.items():
        hyperedge_size_dist[size] = float(dist) / total
    
    return hyperedge_size_dist

def get_node_set(hyperedge_list):
    node_set = []
    for e in hyperedge_list:
        node_set.extend(e)
    
    return set(node_set), np.array([v for k, v in sorted(Counter(node_set).items())])


for i_d, dataset in enumerate(['email-Enron', 'email-Eu', 'contact-high-school', 'contact-primary-school', 'tags-ask-ubuntu', 'tags-math-sx']):
    print(dataset)
    data_split = torch.load(f'./data/splits/{dataset}.pt')
    
    hyperedge_list = data_split['hyperedge_list']
    node_set, degrees = get_node_set(hyperedge_list)
    node_list = sorted(node_set)
    probs = degrees / degrees.sum()
       
    sum_overlap_list_0, sum_overlap_cl_list_0 = [], [], []
    sum_overlap_list_12, sum_overlap_cl_list_12 = [], [], []
    sum_overlap_list_23, sum_overlap_cl_list_23 = [], [], []
    sum_overlap_list_34, sum_overlap_cl_list_34 = [], [], []
    sum_overlap_list_45, sum_overlap_cl_list_45 = [], [], []
    sum_overlap_list_1, sum_overlap_cl_list_1 = [], [], []

    GP_train_list = list(data_split['ground_data'])
    GP_train = set(frozenset(hyperedge) for hyperedge in GP_train_list)   
    
    he_size_dist = hyperedge_size_dist(data_split['unique_ground_data'], dataset)
    
    test_data = []
    test_data_count = 0

    for hyperedge in data_split['test_data']:
        if len(hyperedge) in he_size_dist.keys():
            hyperedge_frozenset = frozenset(hyperedge)
            test_data.append(hyperedge_frozenset)
            test_data_count += 1
    
    he_size_dist_test = hyperedge_size_dist(test_data, dataset)    
    sum_test_data_count = test_data_count * len(GP_train_list)

    sum_overlap_0, sum_overlap_cl_0 = 0, 0, 0
    sum_overlap_12, sum_overlap_cl_12 = 0, 0, 0
    sum_overlap_23, sum_overlap_cl_23 = 0, 0, 0
    sum_overlap_34, sum_overlap_cl_34 = 0, 0, 0
    sum_overlap_45, sum_overlap_cl_45 = 0, 0, 0
    sum_overlap_1, sum_overlap_cl_1 = 0, 0, 0
    
    for j in range(test_data_count):
        sample_hyedge_degree = np.random.choice(list(he_size_dist_test.keys()), replace=True, p=list(he_size_dist_test.values()))
        
        hyperedge_cl = frozenset(np.random.choice(node_list, size=sample_hyedge_degree, replace=False, p=probs))
        while hyperedge_cl in GP_train:
            hyperedge_cl = frozenset(np.random.choice(node_list, size=sample_hyedge_degree, replace=False, p=probs))
        
        for hyperedge_tr in GP_train_list:
            len_hyperedge_tr = len(hyperedge_tr)
            
            overlap = len(test_data[j] & hyperedge_tr)
            overlap_ratio = overlap / len_hyperedge_tr
            if overlap_ratio > 0:
                sum_overlap_0 += 1
                if overlap_ratio >= 0.5:
                    sum_overlap_12 += 1
                    if overlap_ratio >= 2/3:
                        sum_overlap_23 += 1
                        if overlap_ratio >= 3/4:
                            sum_overlap_34 += 1
                            if overlap_ratio >= 4/5:
                                sum_overlap_45 += 1
                                if overlap_ratio >= 1:
                                    sum_overlap_1 += 1
                
            overlap_cl = len(hyperedge_cl & hyperedge_tr)
            overlap_ratio_cl = overlap_cl / len_hyperedge_tr
            if overlap_ratio_cl > 0:
                sum_overlap_cl_0 += 1
                if overlap_ratio_cl >= 0.5:
                    sum_overlap_cl_12 += 1
                    if overlap_ratio_cl >= 2/3:
                        sum_overlap_cl_23 += 1
                        if overlap_ratio_cl >= 3/4:
                            sum_overlap_cl_34 += 1
                            if overlap_ratio_cl >= 4/5:
                                sum_overlap_cl_45 += 1
                                if overlap_ratio_cl >= 1:
                                    sum_overlap_cl_1 += 1
    
    sum_overlap_list_0.append(sum_overlap_0/sum_test_data_count)
    sum_overlap_cl_list_0.append(sum_overlap_cl_0/sum_test_data_count)
    sum_overlap_list_12.append(sum_overlap_12/sum_test_data_count)
    sum_overlap_cl_list_12.append(sum_overlap_cl_12/sum_test_data_count)
    sum_overlap_list_23.append(sum_overlap_23/sum_test_data_count)
    sum_overlap_cl_list_23.append(sum_overlap_cl_23/sum_test_data_count)
    sum_overlap_list_34.append(sum_overlap_34/sum_test_data_count)
    sum_overlap_cl_list_34.append(sum_overlap_cl_34/sum_test_data_count)
    sum_overlap_list_45.append(sum_overlap_45/sum_test_data_count)
    sum_overlap_cl_list_45.append(sum_overlap_cl_45/sum_test_data_count)
    sum_overlap_list_1.append(sum_overlap_1/sum_test_data_count)
    sum_overlap_cl_list_1.append(sum_overlap_cl_1/sum_test_data_count)
        
    print(f"Target\n{np.mean(sum_overlap_list_0)}\n{np.mean(sum_overlap_list_12)}\n{np.mean(sum_overlap_list_23)}\n{np.mean(sum_overlap_list_34)}\n{np.mean(sum_overlap_list_45)}\n{np.mean(sum_overlap_list_1)}\n")
    print(f"CL\n{np.mean(sum_overlap_cl_list_0)}\n{np.mean(sum_overlap_cl_list_12)}\n{np.mean(sum_overlap_cl_list_23)}\n{np.mean(sum_overlap_cl_list_34)}\n{np.mean(sum_overlap_cl_list_45)}\n{np.mean(sum_overlap_cl_list_1)}\n")