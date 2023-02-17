package ch.dfx.common.config;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.security.EncryptionForSecrets;

/**
 * 
 */
public class ConfigProvider {
  private static final Logger LOGGER = LogManager.getLogger(ConfigProvider.class);

  // ...
  private static final String DFX_SEP = "DFX_SEP";

  private static final String CONFIG_NAME_HOST_ID = "host_id";
  private static final String CONFIG_NAME_FILENAME = "config_filename";

  private static final String PLACEHOLDER_ENVIRONMENT = "[ENVIRONMENT]";
  private static final String PLACEHOLDER_HOST_ID = "[HOST_ID]";

  // ...
  private static ConfigProvider instance = null;

  // ...
  private NetworkEnum network = null;
  private EnvironmentEnum environment = null;
  private String hostId = null;

  private final Map<String, Object> configMap;

  /**
   * 
   */
  public static void setup(
      @Nonnull NetworkEnum network,
      @Nonnull EnvironmentEnum environment,
      @Nonnull String[] args) {
    if (null != instance) {
      String errorString = "Setup can only be called once ...";
      LOGGER.error(errorString);
      throw new RuntimeException(errorString);
    }

    instance = new ConfigProvider();

    instance.doSetup(network, environment, args);
  }

  /**
   * 
   */
  public static ConfigProvider getInstance() {
    return instance;
  }

  /**
   * 
   */
  private void doSetup(
      @Nonnull NetworkEnum network,
      @Nonnull EnvironmentEnum environment,
      @Nonnull String[] args) {
    // ...
    this.network = network;
    this.environment = environment;

    // ...
    String password = null;

    // ...
    Optional<String> optionalPasswordArgument =
        Stream.of(args).filter(a -> a.startsWith("--password=")).findFirst();

    if (optionalPasswordArgument.isPresent()) {
      password = optionalPasswordArgument.get().split("=")[1];
    }

    // ...
    if (null == password) {
      password = System.getProperty(DFX_SEP);
    }

    if (null == password) {
      password = System.getenv(DFX_SEP);
    }

    if (null != password) {
      loadConfig(network, password);
    }
  }

  /**
   * 
   */
  private void loadConfig(
      @Nonnull NetworkEnum network,
      @Nonnull String password) {
    File configFile = Path.of("config", "global", "config.json").toFile();

    try (FileReader reader = new FileReader(configFile)) {
      JsonObject configObject = new Gson().fromJson(reader, JsonObject.class);
      setupConfigMap(configObject, null);

      hostId = getValue(CONFIG_NAME_HOST_ID);

      String configFilename = getValue(network.toString().toLowerCase() + "." + CONFIG_NAME_FILENAME);

      if (null != configFilename) {
        loadNetworkConfig(password, new File(configFilename));
      }
    } catch (Exception e) {
      LOGGER.error("loadConfig", e);
    }
  }

  /**
   * 
   */
  private void loadNetworkConfig(
      @Nonnull String password,
      @Nonnull File configFile) {
    try {
      EncryptionForSecrets encryptionForSecrets = new EncryptionForSecrets();
      String decryptedConfig = encryptionForSecrets.decrypt(configFile, password);

      if (null != decryptedConfig) {
        JsonObject configObject = new Gson().fromJson(decryptedConfig, JsonObject.class);
        setupConfigMap(configObject, null);
      }
    } catch (Exception e) {
      LOGGER.error("loadNetworkConfig", e);
    }
  }

  /**
   * 
   */
  private void setupConfigMap(
      @Nonnull JsonObject jsonObject,
      @Nullable String mapKey) {
    for (Entry<String, JsonElement> jsonConfigDataEntry : jsonObject.entrySet()) {
      String jsonConfigDataEntryKey = jsonConfigDataEntry.getKey();
      JsonElement jsonElement = jsonConfigDataEntry.getValue();

      String workMapKey = (null == mapKey ? jsonConfigDataEntryKey : mapKey + "." + jsonConfigDataEntryKey);

      if (jsonElement.isJsonPrimitive()) {
        if (!jsonConfigDataEntryKey.startsWith("#")) {
          putValueToConfigMap(workMapKey, jsonElement.getAsString());
        }

        workMapKey = mapKey;
      } else if (jsonElement.isJsonArray()) {
        List<String> resultList = new ArrayList<>();

        for (JsonElement jsonArrayElement : jsonElement.getAsJsonArray()) {
          if (jsonArrayElement.isJsonPrimitive()) {
            resultList.add(jsonArrayElement.getAsString());
          }
        }

        putValueToConfigMap(workMapKey, resultList);
        workMapKey = mapKey;
      } else if (jsonElement.isJsonObject()) {
        setupConfigMap((JsonObject) jsonElement, workMapKey);
      }
    }
  }

  /**
   * 
   */
  private void putValueToConfigMap(
      @Nonnull String workMapKey,
      @Nonnull Object valueObject) {
    String overwriteCheckKey =
        new StringBuilder(network.toString().toLowerCase()).append(".").append(workMapKey).toString();
    Object overrideObject = configMap.get(overwriteCheckKey);

    if (null == overrideObject) {
      configMap.put(workMapKey, valueObject);
    } else {
      configMap.put(workMapKey, overrideObject);
    }
  }

  /**
   * 
   */
  public @Nullable String getValue(@Nonnull ConfigEntry config) {
    String absoluteName =
        config.getAbsolutName()
            .replace(PLACEHOLDER_ENVIRONMENT, environment.toString().toLowerCase())
            .replace(PLACEHOLDER_HOST_ID, hostId);

    return getValue(absoluteName);
  }

  /**
   * 
   */
  private @Nullable String getValue(@Nonnull String absoluteName) {
    String value = null;

    Object valueObject = configMap.get(absoluteName);

    if (null != valueObject) {
      value = valueObject.toString();
    }

    return value;
  }

  /**
   * 
   */
  public @Nonnull String getValue(
      @Nonnull ConfigEntry config,
      @Nonnull String defaultValue) {
    String value = defaultValue;

    String configValue = getValue(config);

    if (null != configValue) {
      value = configValue;
    }

    return value;
  }

  /**
   * 
   */
  public boolean getValue(
      @Nonnull ConfigEntry config,
      boolean defaultValue) {
    boolean value = defaultValue;

    String configValue = getValue(config);

    if (null != configValue) {
      if ("0".equals(configValue)) {
        value = false;
      } else if ("1".equals(configValue)) {
        value = true;
      } else {
        value = Boolean.parseBoolean(configValue);
      }
    }

    return value;
  }

  /**
   * 
   */
  public int getValue(
      @Nonnull ConfigEntry config,
      int defaultValue) {
    int value = defaultValue;

    String configValue = getValue(config);

    if (null != configValue) {
      value = Integer.parseInt(configValue);
    }

    return value;
  }

  /**
   * 
   */
  public @Nonnull List<String> getListValue(@Nonnull ConfigEntry config) {
    List<String> resultList = new ArrayList<>();

    Object object = configMap.get(getAbsoluteName(config));

    if (object instanceof List) {
      ((List<?>) object).forEach(v -> resultList.add(v.toString()));
    }

    return resultList;
  }

  /**
   * 
   */
  private String getAbsoluteName(@Nonnull ConfigEntry config) {
    return config.getAbsolutName()
        .replace(PLACEHOLDER_ENVIRONMENT, environment.toString().toLowerCase())
        .replace(PLACEHOLDER_HOST_ID, hostId);
  }

  /**
   * 
   */
  private ConfigProvider() {
    this.configMap = new HashMap<>();
  }
}
