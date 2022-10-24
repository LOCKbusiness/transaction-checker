package ch.dfx.common.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.PropertyEnum;

/**
 *  
 */
public class ConfigPropertyProvider {
  private static final Logger LOGGER = LogManager.getLogger(ConfigPropertyProvider.class);

  private static ConfigPropertyProvider instance = null;

  private final Map<PropertyEnum, String> propertyMap;

  /**
   * 
   */
  public static void setup(@Nonnull Properties properties) {
    if (null != instance) {
      String errorString = "setup() can only be called once ...";
      LOGGER.error(errorString);
      throw new RuntimeException(errorString);
    }

    instance = new ConfigPropertyProvider();

    for (PropertyEnum property : PropertyEnum.values()) {
      instance.propertyMap.put(property, properties.getProperty(property.name().toLowerCase(), ""));
    }
  }

  /**
   * 
   */
  public static void testSetup(@Nonnull Properties testProperties) {
    if (null == instance) {
      String errorString = "Call setup() before ...";
      LOGGER.error(errorString);
      throw new RuntimeException(errorString);
    }

    for (PropertyEnum property : PropertyEnum.values()) {
      instance.propertyMap.put(property, testProperties.getProperty(property.name().toLowerCase(), ""));
    }
  }

  /**
   * 
   */
  public static ConfigPropertyProvider getInstance() {
    return instance;
  }

  /**
   * 
   */
  public String getProperty(@Nonnull PropertyEnum property) {
    return propertyMap.get(property);
  }

  /**
   * 
   */
  public int getIntValueOrDefault(
      @Nonnull PropertyEnum property,
      int defaultValue) {
    String propertyValue = getProperty(property);

    int value = defaultValue;

    if (StringUtils.isNumeric(propertyValue)) {
      value = Integer.parseInt(propertyValue);
    }

    return value;
  }

  /**
   * 
   */
  private ConfigPropertyProvider() {
    this.propertyMap = new HashMap<>();
  }
}
