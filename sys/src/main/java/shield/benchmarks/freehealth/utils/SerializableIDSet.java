package shield.benchmarks.freehealth.utils;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.client.DatabaseAbortException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SerializableIDSet {
    private Set<Integer> ids;
    private FreeHealthExperimentConfiguration config;

    public SerializableIDSet(FreeHealthExperimentConfiguration config, String serializedIDs) throws DatabaseAbortException {
        this.config = config;
        ids = new HashSet<>();
        if (serializedIDs.length() % config.ID_SIZE != 0) {
            System.err.println("SerializableIDSet received incorrectly serialized input: '" + serializedIDs+ "'");
//            throw new DatabaseAbortException("SerializableIDSet received incorrectly serialized input: '" + serializedIDs+ "'");
            return;
        }
        try {
            for (int i = 0; i < serializedIDs.length() - config.ID_SIZE; i += config.ID_SIZE) {
                Integer cid = Integer.parseInt(serializedIDs.substring(i, i + config.ID_SIZE));
                ids.add(cid);
            }
        } catch (NumberFormatException e) {
            return;
        }

    }

    public SerializableIDSet(FreeHealthExperimentConfiguration config) {
        this.config = config;
        this.ids = new HashSet<>();
    }

    public void add(Integer id) {
        ids.add(id);
    }

    public int size() {
        return ids.size();
    }

    public List<Integer> toList() {
        List<Integer> res = new ArrayList<>();
        res.addAll(ids);
        return res;
    }

    public void remove(Integer id) {
        ids.remove(id);
    }

    public void difference(SerializableIDSet diffingIDs) {
        ids.removeAll(diffingIDs.getIDs());
    }

    public void intersect(SerializableIDSet intersectingIDs) {
        ids.retainAll(intersectingIDs.getIDs());
    }

    public void union(SerializableIDSet unioningIDs) {
        ids.addAll(unioningIDs.getIDs());
    }

    public Set<Integer> getIDs() {
        return ids;
    }

    public String serialize() {
        StringBuilder res = new StringBuilder();
        for (Integer id : ids) {
            String strID = id.toString();
            for (int i = 0; i < config.ID_SIZE - strID.length(); i++) res.append("0"); // pad with appropriate number of zeroes
            res.append(strID);
        }
        return res.toString();
    }

    @Override
    public String toString() {
        return this.serialize();
    }

}
