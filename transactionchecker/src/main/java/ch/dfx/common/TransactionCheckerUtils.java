package ch.dfx.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.defichain.provider.DefiDataProviderImpl;
import ch.dfx.security.EncryptionForSecrets;

/**
 * 
 */
public class TransactionCheckerUtils {
  private static final Logger LOGGER = LogManager.getLogger(TransactionCheckerUtils.class);

  private static final boolean DEBUG_SECRET = false;

  public static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.00000000");

  // ...
  private static final Gson GSON =
      new GsonBuilder()
          .setPrettyPrinting()
          .create();

  // ...
  private static final EnvironmentEnum ENVIRONMENT;

  static {
    String os = System.getProperty("os.name").toLowerCase();

    if (os.contains("win")) {
      ENVIRONMENT = EnvironmentEnum.WINDOWS;
    } else if (os.contains("mac")) {
      ENVIRONMENT = EnvironmentEnum.MACOS;
    } else {
      ENVIRONMENT = EnvironmentEnum.UNKNOWN;
    }
  }

  /**
   * 
   */
  public static EnvironmentEnum getEnvironment() {
    return ENVIRONMENT;
  }

  /**
   * 
   */
  public static boolean isWindows() {
    return EnvironmentEnum.WINDOWS == ENVIRONMENT;
  }

  /**
   * 
   */
  public static boolean isMacOs() {
    return EnvironmentEnum.MACOS == ENVIRONMENT;
  }

  /**
   * 
   */
  public static void initLog4j(@Nonnull String log4j2Xml) {
    Path log4jPath = Paths.get("config", "log4j", log4j2Xml);

    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    context.setConfigLocation(log4jPath.toUri());
  }

  /**
   * 
   */
  public static void loadConfigProperties(
      @Nonnull String network,
      @Nonnull String environment) throws DfxException {
    LOGGER.trace("loadConfigProperties() ...");

    // ...
    File configDirectory = Paths.get("config", "properties", network).toFile();

    File configFile = new File(configDirectory, "config.properties");
    File configSecretEncFile = new File(configDirectory, "config.secret.enc");

    // ...
    File configEnvDirectory = new File(configDirectory, environment);

    File configEnvFile = new File(configEnvDirectory, "config.properties");
    File configEnvSecretEncFile = new File(configEnvDirectory, "config.secret.enc");

    // ...
    Properties properties = new Properties();

    // Environment specific properties ...
    LOGGER.trace("configFileName: '" + configEnvFile.getAbsolutePath() + "'...");

    try (FileInputStream inputStream = new FileInputStream(configEnvFile)) {
      properties.load(inputStream);
    } catch (Exception e) {
      throw new DfxException("loading '" + configEnvFile.getAbsolutePath() + "' ...", e);
    }

    // Global Properties ...
    LOGGER.trace("configFileName: '" + configFile.getAbsolutePath() + "'...");

    try (FileInputStream inputStream = new FileInputStream(configFile)) {
      properties.load(inputStream);
    } catch (Exception e) {
      throw new DfxException("loading '" + configFile.getAbsolutePath() + "' ...", e);
    }

    // Secret Properties ...
    EncryptionForSecrets encryptionForSecrets = new EncryptionForSecrets();
    String secretEncodingPassword = System.getenv("DFX_SEP");

    if (null != secretEncodingPassword) {
      LOGGER.trace("configFileName: '" + configEnvSecretEncFile.getAbsolutePath() + "'...");

      Properties configEnvSecretProperties = encryptionForSecrets.decrypt(configEnvSecretEncFile, secretEncodingPassword);
      properties.putAll(configEnvSecretProperties);

      // Global Secret Properties ...
      LOGGER.trace("configFileName: '" + configSecretEncFile.getAbsolutePath() + "'...");

      Properties configSecretProperties = encryptionForSecrets.decrypt(configSecretEncFile, secretEncodingPassword);
      properties.putAll(configSecretProperties);
    }

    ConfigPropertyProvider.setup(properties);
  }

  /**
   * 
   */
  public static DefiDataProvider createDefiDataProvider() {
    LOGGER.trace("createDefiDataProvider() ...");

    String username = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_RPC_USERNAME);
    String password = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_RPC_PASSWORD);

    if (DEBUG_SECRET) {
      LOGGER.debug("DFI Username:" + username);
      LOGGER.debug("DFI Password:" + password);
    }

    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);

    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, credentials);

    int timeout = 10 * 1000;

    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .setSocketTimeout(timeout)
            .build();

    HttpClient httpClient =
        HttpClientBuilder.create()
            .setDefaultCredentialsProvider(credentialsProvider)
            .setDefaultRequestConfig(requestConfig)
            .build();

    HttpPost httpPost = new HttpPost();

    // ...
    return new DefiDataProviderImpl(httpClient, httpPost);
  }

  /**
   * 
   */
  public static String toNumberFormatString(@Nullable BigDecimal value) {
    return null == value ? NUMBER_FORMAT.format(0) : NUMBER_FORMAT.format(value.doubleValue());
  }

  /**
   * 
   */
  public static String toJson(Object object) {
    String jsonString = "";

    if (null != object) {
      jsonString = GSON.toJson(object);
    }

    return jsonString;
  }

  /**
   * 
   */
  public static <T> T fromJson(@Nonnull File jsonFile, Class<T> clazz) throws DfxException {
    try (JsonReader reader = new JsonReader(new FileReader(jsonFile))) {
      return GSON.fromJson(reader, clazz);
    } catch (Exception e) {
      throw new DfxException("fromJson", e);
    }
  }

  /**
   * 
   */
  public static void rollback(@Nullable Connection connection) throws DfxException {
    try {
      if (null != connection) {
        connection.rollback();
      }
    } catch (Exception e) {
      throw new DfxException("rollback ...", e);
    }
  }

  /**
   * 
   */
  public static String emptyIfNull(@Nullable String value) {
    return StringUtils.defaultIfEmpty(value, "");
  }

  /**
   * 
   */
  public static Integer zeroIfNull(@Nullable Integer value) {
    return null == value ? Integer.valueOf(0) : value;
  }

  /**
   * 
   */
  public static BigDecimal zeroIfNull(@Nullable BigDecimal value) {
    return null == value ? BigDecimal.ZERO : value;
  }
}
