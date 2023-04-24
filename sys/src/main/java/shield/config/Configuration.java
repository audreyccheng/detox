package shield.config;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public abstract class Configuration {

  /**
   * Guard flag: determines whether this block has been initialized No work should be done until
   * executed
   */
  protected boolean isInitialised = false;
  protected String configFileName = null;
  protected static Configuration config = null;


  /**
   * Loads the constant values from JSON file
   */
  public abstract void loadProperties(String fileName)
      throws IOException, ParseException;

  /**
   * This is a test method which initializes constants to default values without the need to pass in
   * a configuration file
   *
   * @return true if initialization successful
   */
  public abstract void loadProperties();

  public boolean isInitialised() {
    return isInitialised == true;
  }

  public String getPropString(JSONObject prop, String key, String def) {
    String data = (String) prop.get(key);
    if (data != null) {
      data.trim();
      return data;
    } else {
      return def;
    }
  }

  public Integer getPropInt(JSONObject prop, String key, int def) {
    String data = (String) prop.get(key);
    if (data != null) {
      data.trim();
      return Integer.parseInt(data);
    } else {
      return def;
    }
  }

  public long getPropLong(JSONObject prop, String key, long def) {
    String data = (String) prop.get(key);
    if (data != null) {
      data.trim();
      return Long.parseLong(data);
    } else {
      return def;
    }
  }

  public double getPropDouble(JSONObject prop, String key, double def) {
    String data = (String) prop.get(key);
    if (data != null) {
      data.trim();
      return Double.parseDouble(data);
    } else {
      return def;
    }
  }

  public Boolean getPropBool(JSONObject prop, String key, boolean def) {
    String data = (String) prop.get(key);
    if (data != null) {
      data.trim();
      return Boolean.parseBoolean(data);
    } else {
      return def;
    }
  }


}
