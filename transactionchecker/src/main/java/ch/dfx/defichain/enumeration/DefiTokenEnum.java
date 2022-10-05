package ch.dfx.defichain.enumeration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 
 */
public enum DefiTokenEnum {
  // Krypto Token ...
  DFI("0", "DFI"),

  ETH("1", "ETH"),
  BTC("2", "BTC"),
  USDT("3", "USDT"),
  ETH_DFI("4", "ETH-DFI", ETH, DFI),
  BTC_DFI("5", "BTC-DFI", BTC, DFI),
  USDT_DFI("6", "USDT-DFI", USDT, DFI),
  DOGE("7", "DOGE"),
  DOGE_DFI("8", "DOGE-DFI", DOGE, DFI),
  LTC("9", "LTC"),
  LTC_DFI("10", "LTC-DFI", LTC, DFI),
  BCH("11", "BCH"),
  BCH_DFI("12", "BCH-DFI", BCH, DFI),
  USDC("13", "USDC"),
  USDC_DFI("14", "USDC-DFI", USDC, DFI),

  // Krypto - Stock Token ...
  DUSD("15", "DUSD"),
  DUSD_DFI("17", "DUSD-DFI", DUSD, DFI),

  // Stock Token ...
  TSLA("16", "TSLA"),
  TSLA_DUSD("18", "TSLA-DUSD", TSLA, DUSD),
  BABA("19", "BABA"),
//  GME/v1("20", "GME/v1"),
  PLTR("21", "PLTR"),
  AAPL("22", "AAPL"),
//  GOOGL/v1("23", "GOOGL/v1"),
  ARKK("24", "ARKK"),
//  GME_DUSD/v1("25", "GME-DUSD/v1", GME, DUSD/v1),
  SPY("26", "SPY"),
  QQQ("27", "QQQ"),
  GLD("28", "GLD"),
  SLV("29", "SLV"),
  PDBC("30", "PDBC"),
  VNQ("31", "VNQ"),
//  GOOGL_DUSD/v1("32", "GOOGL-DUSD/v1", GOOGL, DUSD/v1),
  BABA_DUSD("33", "BABA-DUSD", BABA, DUSD),
  URTH("34", "URTH"),
  PLTR_DUSD("35", "PLTR-DUSD", PLTR, DUSD),
  AAPL_DUSD("36", "AAPL-DUSD", AAPL, DUSD),
  TLT("37", "TLT"),
  SPY_DUSD("38", "SPY-DUSD", SPY, DUSD),
  QQQ_DUSD("39", "QQQ-DUSD", QQQ, DUSD),
  PDBC_DUSD("40", "PDBC-DUSD", PDBC, DUSD),
  VNQ_DUSD("41", "VNQ-DUSD", VNQ, DUSD),
  ARKK_DUSD("42", "ARKK-DUSD", ARKK, DUSD),
  GLD_DUSD("43", "GLD-DUSD", GLD, DUSD),
  URTH_DUSD("44", "URTH-DUSD", URTH, DUSD),
  TLT_DUSD("45", "TLT-DUSD", TLT, DUSD),
  SLV_DUSD("46", "SLV-DUSD", SLV, DUSD),
//  BURN("47", "BURN"),
//  BURN_DFI("48", "BURN-DFI", BURN, DFI),
//  AMZN/v1("49", "AMZN/v1"),
  COIN("50", "COIN"),
  EEM("51", "EEM"),
  NVDA("52", "NVDA"),
  EEM_DUSD("53", "EEM-DUSD", EEM, DUSD),
//  AMZN_DUSD/v1("54", "AMZN-DUSD/v1", AMZN, DUSD/v1),
  NVDA_DUSD("55", "NVDA-DUSD", NVDA, DUSD),
  COIN_DUSD("56", "COIN-DUSD", COIN, DUSD),
  MSFT("57", "MSFT"),
  VOO("58", "VOO"),
  FB("59", "FB"),
  NFLX("60", "NFLX"),
  MSFT_DUSD("61", "MSFT-DUSD", MSFT, DUSD),
  NFLX_DUSD("62", "NFLX-DUSD", NFLX, DUSD),
  VOO_DUSD("63", "VOO-DUSD", VOO, DUSD),
  FB_DUSD("64", "FB-DUSD", FB, DUSD),
  DIS("65", "DIS"),
  MSTR("66", "MSTR"),
  MCHI("67", "MCHI"),
  INTC("68", "INTC"),
  DIS_DUSD("69", "DIS-DUSD", DIS, DUSD),
  MCHI_DUSD("70", "MCHI-DUSD", MCHI, DUSD),
  MSTR_DUSD("71", "MSTR-DUSD", MSTR, DUSD),
  INTC_DUSD("72", "INTC-DUSD", INTC, DUSD),
  PYPL("73", "PYPL"),
  KO("74", "KO"),
  BRK_B("75", "BRK.B"),
  PG("76", "PG"),
  PYPL_DUSD("77", "PYPL-DUSD", PYPL, DUSD),
  BRK_B_DUSD("78", "BRK.B-DUSD", BRK_B, DUSD),
  PG_DUSD("79", "PG-DUSD", PG, DUSD),
  KO_DUSD("80", "KO-DUSD", KO, DUSD),
  SAP("81", "SAP"),
  CS("82", "CS"),
  GSG("83", "GSG"),
  URA("84", "URA"),
  SAP_DUSD("85", "SAP-DUSD", SAP, DUSD),
  GSG_DUSD("86", "GSG-DUSD", GSG, DUSD),
  URA_DUSD("87", "URA-DUSD", URA, DUSD),
  CS_DUSD("88", "CS-DUSD", CS, DUSD),
  AMZN("89", "AMZN"),
  AMZN_DUSD("90", "AMZN-DUSD", AMZN, DUSD),
  PPLT("91", "PPLT"),
  GOVT("92", "GOVT"),
  XOM("93", "XOM"),
  TAN("94", "TAN"),
  PPLT_DUSD("95", "PPLT-DUSD", PPLT, DUSD),
  TAN_DUSD("96", "TAN-DUSD", TAN, DUSD),
  XOM_DUSD("97", "XOM-DUSD", XOM, DUSD),
  GOVT_DUSD("98", "GOVT-DUSD", GOVT, DUSD),
  GOOGL("99", "GOOGL"),
  GOOGL_DUSD("100", "GOOGL-DUSD", GOOGL, DUSD),
  USDT_DUSD("101", "USDT-DUSD", USDT, DUSD),
  USDC_DUSD("102", "USDC-DUSD", USDC, DUSD),
  GME("103", "GME"),
  GME_DUSD("104", "GME-DUSD", GME, DUSD),
  UNKNOWN("999999", "UNKNOWN");

  // ...
  private static final Map<String, DefiTokenEnum> keyToTokenCache;

  static {
    keyToTokenCache = new HashMap<>();

    for (DefiTokenEnum defiToken : DefiTokenEnum.values()) {
      keyToTokenCache.put(defiToken.getKey(), defiToken);
    }
  }

  /**
   * 
   */
  public static @Nonnull DefiTokenEnum createByKey(@Nonnull String key) {
    Objects.requireNonNull(key, "Key is null ...");

    DefiTokenEnum token = keyToTokenCache.get(key);

    if (null == token) {
      token = UNKNOWN;
      // throw new DfxRuntimeException("Unknown Key '" + key + "' ...");
    }

    return token;
  }

  // ...
  private static final Map<String, DefiTokenEnum> symbolKeyToTokenCache;

  static {
    symbolKeyToTokenCache = new HashMap<>();

    for (DefiTokenEnum defiToken : DefiTokenEnum.values()) {
      symbolKeyToTokenCache.put(defiToken.getSymbolKey(), defiToken);
    }
  }

  /**
   * 
   */
  public static @Nonnull DefiTokenEnum createBySymbolKey(@Nonnull String symbolKey) {
    Objects.requireNonNull(symbolKey, "Symbol Key is null ...");

    DefiTokenEnum token = symbolKeyToTokenCache.get(symbolKey);

    if (null == token) {
      token = UNKNOWN;
      // throw new DfxRuntimeException("Unknown Symbol Key '" + symbolKey + "' ...");
    }

    return token;
  }

  // ...
  private static final Map<DefiTokenEnum, DefiTokenEnum> dfiPoolCache;

  static {
    dfiPoolCache = new HashMap<>();

    for (DefiTokenEnum token : DefiTokenEnum.values()) {
      if (DefiTokenEnum.DFI == token.getPoolToken2()) {
        dfiPoolCache.put(token.getPoolToken1(), token);
      }
    }
  }

  /**
   * 
   */
  public static @Nullable DefiTokenEnum getDFIPoolToken(@Nonnull DefiTokenEnum token) {
    return dfiPoolCache.get(token);
  }

  // ...
  private static final Map<DefiTokenEnum, DefiTokenEnum> dusdPoolCache;

  static {
    dusdPoolCache = new HashMap<>();

    for (DefiTokenEnum token : DefiTokenEnum.values()) {
      if (DefiTokenEnum.DUSD == token.getPoolToken2()) {
        dusdPoolCache.put(token.getPoolToken1(), token);
      }
    }
  }

  /**
   * 
   */
  public static @Nullable DefiTokenEnum getDUSDPoolToken(@Nonnull DefiTokenEnum token) {
    return dusdPoolCache.get(token);
  }

  // ...
  private final String key;
  private final String symbolKey;
  private final DefiTokenEnum poolToken1;
  private final DefiTokenEnum poolToken2;

  /**
   * 
   */
  DefiTokenEnum(
      @Nonnull String key,
      @Nonnull String symbolKey) {
    this.key = key;
    this.symbolKey = symbolKey;
    this.poolToken1 = null;
    this.poolToken2 = null;
  }

  /**
   * 
   */
  DefiTokenEnum(
      @Nonnull String key,
      @Nonnull String symbolKey,
      @Nonnull DefiTokenEnum poolToken1,
      @Nonnull DefiTokenEnum poolToken2) {
    this.key = key;
    this.symbolKey = symbolKey;
    this.poolToken1 = poolToken1;
    this.poolToken2 = poolToken2;
  }

  /**
   * 
   */
  public String getKey() {
    return key;
  }

  /**
   * 
   */
  public byte getKeyAsByte() {
    return Byte.valueOf(key).byteValue();
  }

  /**
   * 
   */
  public int getKeyAsInt() {
    return Integer.valueOf(key).intValue();
  }

  /**
   * 
   */
  public String getSymbolKey() {
    return symbolKey;
  }

  /**
   * 
   */
  public @Nullable DefiTokenEnum getPoolToken1() {
    return poolToken1;
  }

  /**
   * 
   */
  public @Nullable DefiTokenEnum getPoolToken2() {
    return poolToken2;
  }

  /**
   * 
   */
  public String getDisplayName() {
    return this == DefiTokenEnum.DFI ? symbolKey : "d" + symbolKey;
  }

  /**
   * 
   */
  public boolean isKryptoPoolToken() {
    return null != dfiPoolCache.get(this);
  }

  /**
   * 
   */
  public boolean isStockPoolToken() {
    return null != dusdPoolCache.get(this);
  }

  /**
   * 
   */
  public boolean isPoolToken() {
    return isKryptoPoolToken() || isStockPoolToken();
  }

  /**
   * 
   */
  public boolean isLPS() {
    return null != poolToken1;
  }
}
