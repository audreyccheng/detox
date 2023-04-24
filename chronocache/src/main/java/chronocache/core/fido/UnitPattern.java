package chronocache.core.fido;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitPattern {
	private Logger logger = LoggerFactory.getLogger(UnitPattern.class);

    public List<String> prefix;
    public List<String> suffix;

    private double numCorrectPredictions;
    private double totalNumPredictions;

    public UnitPattern(List<String> prefix, List<String> suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.numCorrectPredictions = 0;
        this.totalNumPredictions = 0;
    }

    public boolean withinThresholdDistance(List<String> comparePrefix,
            List<String> compareSuffix, double threshold) {
        if (!((comparePrefix.size() == prefix.size()) && (compareSuffix.size() == suffix.size()))) {
          logger.warn("incorrect size");
          return false;
        }

        double totalSize = prefix.size() + suffix.size();
        int correct = countCorrect(prefix, comparePrefix) + countCorrect(suffix, compareSuffix);

        double ratio = correct / totalSize;
        logger.trace("ratio {}, threshold {}", ratio, threshold);
        return (ratio >= threshold);
    }

    public static int countCorrect(List<String> expected, List<String> compare) {
        int correct = 0;

        for(int i = 0; i < expected.size(); i++) {
            if (expected.get(i).equals(compare.get(i))) {
                correct++;
            }
        }

        return correct;
    }

    public double getAccuracy() {
        return numCorrectPredictions / totalNumPredictions;
    }

    public void addCorrectPrediction() {
        numCorrectPredictions++;
        totalNumPredictions++;
    }

    public void addIncorrectPrediction() {
        totalNumPredictions++;
    }
}
