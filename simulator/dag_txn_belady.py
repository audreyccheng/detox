import numpy as np
import workload

from scipy import stats

class TransactionalBeladyCache:
    def __init__(self):
        self.warmup = 100000
        self.cache = set()
        self.hits = 0
        self.key_info = {}
        self.id_map = {} # map of <key, list of requests which transactional hit can occur>
        self.req_num = 0
        self.present_counts = []
        self.partial_hits = 0
        self.incre_hits = 0
        self.total_levels = 0
        self.int_keys = True
        self.hit_keys = set()
        self.hit_ratio = 0.0
        self.pure_hits = 0.0
        self.total_keys = 0.0

    def flatten_list(self, _2d_list):
        flat_list = []
        # Iterate through the outer list
        for element in _2d_list:
            if type(element) is list:
                # If the element is of type list, iterate through the sublist
                for item in element:
                    flat_list.append(item)
            else:
                flat_list.append(element)
        return flat_list

    def parseWorkload(self, filename):
        f = open(filename, "r")
        num_reqs = 0
        reqs = [] # list of transactions
        ids = [] # list of list of keys
        read_set = set() # read set of all keys of previous transactions
        all_levels = []
        for line in f:
            all_keys = []
            vals = line.split(";")[:-1]
            for v in vals:
                keys = v.split(",")[:-1]
                all_keys.append(keys)

            reqs.append(self.flatten_list(all_keys))
            # check if this transaction can be a transactional hit
            all_present = True
            for key in keys:
                if not key in read_set:
                    all_present = False
            # if it is, record down the request number for each key
            if all_present:
                for key in keys:
                    if key in self.id_map:
                        self.id_map[key].append(num_reqs)
                    else:
                        self.id_map[key] = [num_reqs]
            # add all keys to read set
            for key in keys:
                read_set.add(key)
            num_reqs += 1

        f.close()

    # evict keys until cache size is under limit
    def evict(self, req_num):
        no_hit_keys = set()
        key_map = {}
        for key in self.cache:
            # remove requests we've already gone past
            if key in self.id_map:
                vals = self.id_map[key]
                if len(vals) > 0:
                    if vals[0] <= req_num:
                        updated_vals = []
                        for v in vals:
                            if v > req_num:
                                updated_vals.append(v)
                        if len(updated_vals) == 0:
                            self.id_map.pop(key, None)
                        else:
                            self.id_map[key] = updated_vals
            # add next hit request number or add to group of no hits
            if key in self.id_map and len(self.id_map[key]) > 0:
                key_map[key] = self.id_map[key][0] # get the next hit index
            else:
                no_hit_keys.add(key)

        # evict keys based on descending request number of next transactional hit
        sorted_key_map = dict(sorted(key_map.items(), key=lambda item: item[1], reverse=True))

        # first evict from keys with no hits
        while len(no_hit_keys) > 0 and len(self.cache) > self.cache_size:
                key = no_hit_keys.pop()
                self.cache.remove(key)

        sorted_keys = list(sorted_key_map.keys())
        while len(self.cache) > self.cache_size:
            key = sorted_keys.pop(0)
            self.cache.remove(key)
            # print("remove ", key)

    def read_keys(self, all_keys):
        all_present = True
        present_count = 0
        steps = len(all_keys)
        for l in range(len(all_keys)):
            keysl = all_keys[l]
            all_present_l = True
            for k in keysl:
                if not k in self.cache:
                    all_present_l = False
                else:
                    if self.req_num > self.warmup:
                        self.pure_hits += 1
                if self.req_num > self.warmup:
                    self.total_keys += 1
            if all_present_l and self.req_num > self.warmup:
                self.incre_hits += 1
                self.hit_keys.update(keysl)


        keys = self.flatten_list(all_keys)
        for key in keys:
            if not key in self.cache:
                all_present = False
            else:
                present_count += 1
            self.cache.add(key)
        self.present_counts.append((present_count * 1.0) / len(keys))

        if self.req_num > self.warmup:
            self.partial_hits += (present_count * 1.0) / len(keys)
            if all_present:
                self.hits += 1
        if len(self.cache) > self.cache_size:
            self.evict(self.req_num)
        assert len(self.cache) <= self.cache_size
        self.req_num += 1

        if self.req_num > self.warmup:
            self.total_levels += 1.0

        if self.req_num % 10000 == 0:
            print("req_num: ", self.req_num)

        if self.req_num == 150000 and self.int_keys:
            for k in self.cache:
                if k in self.hit_keys:
                    self.hit_ratio += 1
            self.hit_ratio /= len(self.cache)

    def run_txn_belady(self, filename, cache_size):
        self.cache_size = cache_size
        f = open(filename, "r")
        for line in f:
            all_keys = []
            vals = line.split(";")[:-1]
            for v in vals:
                keys = v.split(",")[:-1]
                all_keys.append(keys)
            self.read_keys(all_keys)
        f.close()

if __name__ == "__main__":

    i_s = [10e4,10e5,10e6]

    for i in i_s:
        cache = TransactionalBeladyCache()
        cache.parseWorkload("test3.txt")
        cache.run_txn_belady("test3.txt", i)
