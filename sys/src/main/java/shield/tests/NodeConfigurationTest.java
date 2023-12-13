package shield.tests;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import shield.config.NodeConfiguration;

import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class NodeConfigurationTest {
    // Shouldn't there be public static constants for these values?
    private final double DEFAULT_PREFETCH_FREQ_THRESH = 0.2;
    private final double DEFAULT_PREFETCH_LEN_THRESH = 5;
    private final boolean DEFAULT_USE_SQL = false;

    @org.junit.jupiter.api.Test
    public void testLoadPropertiesDefault() {
        NodeConfiguration ng = new NodeConfiguration();

        assertEquals(DEFAULT_PREFETCH_FREQ_THRESH, ng.PREFETCH_FREQ_THRESH);
        assertEquals(DEFAULT_PREFETCH_LEN_THRESH, ng.PREFETCH_LEN_THRESH);
        assertEquals(DEFAULT_USE_SQL, ng.USE_SQL);
    }

    @org.junit.jupiter.api.Test
    public void testLoadPropertiesConfigFile() throws IOException, ParseException {
        String filepath = "./src/main/java/shield/tests/config/ExampleExpConfig.json";
        NodeConfiguration ng = new NodeConfiguration(filepath);

        JSONObject prop = readExpConfigFile(filepath);

        assertEquals(Double.parseDouble(prop.get("prefetch_freq_thresh").toString()), ng.PREFETCH_FREQ_THRESH);
        assertEquals(Double.parseDouble(prop.get("prefetch_len_thresh").toString()), ng.PREFETCH_LEN_THRESH);
        assertEquals(Boolean.parseBoolean(prop.get("use_sql").toString()), ng.USE_SQL);
    }

    @org.junit.jupiter.api.Test
    public void testLoadPropertiesEmptyConfigFile() throws IOException, ParseException {
        NodeConfiguration ng = new NodeConfiguration("./src/main/java/shield/tests/config/EmptyExpConfig.json");

        assertEquals(DEFAULT_PREFETCH_FREQ_THRESH, ng.PREFETCH_FREQ_THRESH);
        assertEquals(DEFAULT_PREFETCH_LEN_THRESH, ng.PREFETCH_LEN_THRESH);
        assertEquals(DEFAULT_USE_SQL, ng.USE_SQL);
    }

    private JSONObject readExpConfigFile(String filepath) {
        FileReader reader = null;
        JSONObject prop = null;

        try {
            reader = new FileReader(filepath);

            JSONParser jsonParser = new JSONParser();
            prop = (JSONObject) jsonParser.parse(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return prop;
    }

}