package ch.dfx;

import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.TokenProvider;
import ch.dfx.defichain.data.pool.DefiPoolPairData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.defichain.provider.DefiDataProviderImpl;

/**
 * 
 */
public class TransactionCheckerUtils {
  private static final Logger LOGGER = LogManager.getLogger(TransactionCheckerUtils.class);

  // ...
  private static final int HTTP_CLIENT_TIMEOUT = 30 * 1000;

  public static final SimpleDateFormat LOGFILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

  public static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.00000000");
  public static final DecimalFormat GERMAN_DECIMAL_FORMAT = new DecimalFormat("#,##0.00000000");

  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

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
  public static NetworkEnum getNetwork(boolean isMainnet, boolean isStagnet, boolean isTestnet) {
    NetworkEnum network;

    if (isMainnet) {
      network = NetworkEnum.MAINNET;
    } else if (isStagnet) {
      network = NetworkEnum.STAGNET;
    } else {
      network = NetworkEnum.TESTNET;
    }

    return network;
  }

  /**
   * 
   */
  public static String getLog4jFilename(
      @Nonnull String name,
      @Nonnull NetworkEnum network) {
    return new StringBuilder()
        .append(name)
        .append("-").append(network)
        .append("-").append(getEnvironment())
        .append("-").append(TransactionCheckerUtils.LOGFILE_DATE_FORMAT.format(new Date()))
        .toString();
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
  public static void setupGlobalProvider(
      @Nonnull NetworkEnum network,
      @Nonnull EnvironmentEnum environment,
      @Nonnull String[] args) throws DfxException {
    LOGGER.trace("setupGlobalProvider()");

    setupTokenProvider(network);
    setupConfigProvider(network, environment, args);
  }

  /**
   * 
   */
  private static void setupTokenProvider(@Nonnull NetworkEnum network) throws DfxException {
    LOGGER.trace("setupTokenProvider()");

    TokenProvider.setup(network);
  }

  /**
   * 
   */
  private static void setupConfigProvider(
      @Nonnull NetworkEnum network,
      @Nonnull EnvironmentEnum environment,
      @Nonnull String[] args) throws DfxException {
    LOGGER.trace("setupConfigProvider()");

    ConfigProvider.setup(network, environment, args);
  }

  /**
   * 
   */
  public static String getProcessLockFilename(
      @Nonnull String name,
      @Nonnull NetworkEnum network) {
    return new StringBuilder()
        .append(name)
        .append(".").append(network)
        .append(".").append(getEnvironment())
        .append(".lock")
        .toString();
  }

  /**
   * 
   */
  public static Timestamp getCurrentTimeInUTC() {
    return Timestamp.valueOf(LocalDateTime.ofEpochSecond(System.currentTimeMillis() / 1000, 0, ZoneOffset.UTC));
  }

  /**
   * 
   */
  public static DefiDataProvider createDefiDataProvider() {
    LOGGER.trace("createDefiDataProvider()");

    String username = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.DFI_RPC_USERNAME);
    String password = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.DFI_RPC_PASSWORD);

    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);

    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, credentials);

    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(HTTP_CLIENT_TIMEOUT)
            .setConnectionRequestTimeout(HTTP_CLIENT_TIMEOUT)
            .setSocketTimeout(HTTP_CLIENT_TIMEOUT)
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
  public static Map<String, BigDecimal> getTokenToAmountMap(@Nonnull List<String> amountWithTokenList) {
    Map<String, BigDecimal> tokenToAmountMap = new HashMap<>();

    for (String amountWithTokenEntry : amountWithTokenList) {
      String[] amountWithTokenSplitArray = amountWithTokenEntry.split("\\@");
      tokenToAmountMap.put(amountWithTokenSplitArray[1], new BigDecimal(amountWithTokenSplitArray[0]));
    }

    return tokenToAmountMap;
  }

  /**
   * 
   */
  public static Pair<BigDecimal, BigDecimal> getPoolTokenAmountPair(
      @Nonnull DefiPoolPairData poolPairData,
      @Nonnull BigDecimal ourPoolAmount) {

    BigDecimal poolTokenAReserve = poolPairData.getReserveA();
    BigDecimal poolTokenBReserve = poolPairData.getReserveB();
    BigDecimal poolTotalLiquidity = poolPairData.getTotalLiquidity();

    BigDecimal ourPoolTokenAAmount =
        poolTokenAReserve.divide(poolTotalLiquidity, MATH_CONTEXT).multiply(ourPoolAmount, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);
    BigDecimal ourPoolTokenBAmount =
        poolTokenBReserve.divide(poolTotalLiquidity, MATH_CONTEXT).multiply(ourPoolAmount, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    return Pair.of(ourPoolTokenAAmount, ourPoolTokenBAmount);
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
  public static String toGermanDecimalFormatString(@Nullable BigDecimal value) {
    return null == value ? GERMAN_DECIMAL_FORMAT.format(0) : GERMAN_DECIMAL_FORMAT.format(value.doubleValue());
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
