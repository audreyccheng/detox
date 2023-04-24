import numpy as np
import workload

from scipy import stats

MAX_VALUE = 100000

class BeladyCache:
    def __init__(self):
        self.cache = {}
        self.hits = 0
        self.key_info = {}
        self.req_num = 0
        self.present_counts = []
        self.distinct_keys = set()
        self.partial_hits = 0
        self.incre_hits = 0
        self.level_row_hits = 0
        self.first_keys = set()
        self.total_levels = 0
        self.warmup = 100000
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

    # return string with elements in arrays separated by comma
    def serializeRequest(self, arr, arr2, arr3):
        requestStr = ""
        assert len(arr) == len(arr2)
        for val, val2, val3 in zip(arr, arr2, arr3):
            requestStr += str(val) + "." + str(val2) + "." + str(val3) + ","
        requestStr += "\n"
        return requestStr

    def parseWorkload(self, filename, new_file):
        f = open(filename, "r")
        num_reqs = 0
        ids = [] # list of list of keys
        all_levels = []
        for line in f:
            all_keys = []
            vals = line.split(";")[:-1]
            for v in vals:
                keys = v.split(",")[:-1]
                all_keys.append(keys)
            levels = []
            level = 1
            for j in range(len(all_keys)):
                for i in range(len(all_keys[j])):
                    levels.append(level)
                level += 1
            all_levels.append(levels)
            keys = self.flatten_list(all_keys)
            ids.append(keys)
            num_reqs += 1
            for k in keys:
                self.distinct_keys.add(k)
            for first_level_key in all_keys[0]:
                self.first_keys.add(first_level_key)
        f.close()
        assert len(ids) == len(all_levels)

        next_seq = [] # list of list of indices
        last_seen = {} # map of <id, index>
        for i in range(num_reqs-1, -1, -1):
            current_keys = ids[i]
            indices = []
            for key in current_keys:
                if key in last_seen:
                    indices.append(last_seen[key])
                else:
                    indices.append(MAX_VALUE)
                last_seen[key] = i
            next_seq.append(indices)

        nf = open(new_file, "w")
        for i in range(len(ids)):
            requestStr = self.serializeRequest(ids[i], next_seq[len(ids) - i - 1], all_levels[i])
            nf.write(requestStr)
        nf.close()

        print("# distinct keys: ", len(self.distinct_keys))

    def evict(self, sorted_cache):
        # make sure dict is sorted based on descending access time
        if not sorted_cache:
            self.cache = dict(sorted(self.cache.items(), key=lambda item: item[1], reverse=True))
        # remove key with access furthest in the future
        self.cache.pop(list(self.cache.keys())[0], None)

    def read_keys(self, keys_and_indicies):
        all_present = True
        present_count = 0
        level_map = {} # map of <level, key>
        all_present_level = False
        for (key, index, level) in keys_and_indicies:
            if not key in self.cache:
                all_present = False
            else:
                present_count += 1
                if self.req_num > self.warmup:
                    self.pure_hits += 1
            if self.req_num > self.warmup:
                self.total_keys += 1

            if int(level) in level_map:
                level_map[int(level)].append(key)
            else:
                level_map[int(level)] = [key]

        if self.req_num > self.warmup:
            if all_present:
                self.hits += 1

            steps = np.max(list(level_map.keys()))
            for l in level_map.keys():
                all_present_l = True
                for k in level_map[l]:
                    if not k in self.cache:
                        all_present_l = False
                if all_present_l:
                    self.incre_hits += 1.0#(1.0 / steps)
                    all_present_level = True
                    self.hit_keys.update(level_map[l])
                self.total_levels += 1.0

            if all_present_level:
                self.level_row_hits += 1

            self.partial_hits += (present_count * 1.0) / len(keys_and_indicies)
            self.present_counts.append((present_count * 1.0) / len(keys_and_indicies))

            self.total_levels += 1.0

        for (key, index, level) in keys_and_indicies:
            self.cache[key] = int(index) # update next indices
        sorted_cache = False
        while len(self.cache) > self.cache_size:
            self.evict(sorted_cache)
            sorted_cache = True
        assert len(self.cache) <= self.cache_size


        self.req_num += 1

        if self.req_num == 150000 and self.int_keys:
            for k in self.cache:
                if k in self.hit_keys:
                    self.hit_ratio += 1
            self.hit_ratio /= len(self.cache)


    def run_belady(self, filename, cache_size):
        self.cache_size = cache_size
        f = open(filename, "r")
        for line in f:
            vals = line.split(",")
            keys_and_indicies = []
            for v in vals[:-1]:
                pair = v.split(".") # format is key.index
                assert len(pair) == 3
                keys_and_indicies.append((pair[0], pair[1], pair[2]))
            self.read_keys(keys_and_indicies)

        f.close()


if __name__ == "__main__":

    i_s = [10e4,10e5,10e6]

    for i in i_s:
        cache = BeladyCache()
        cache.parseWorkload("test3.txt","annotated_test.txt")
        cache.run_belady("annotated_test.txt", i)
