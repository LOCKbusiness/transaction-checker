package ch.dfx.common.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;

/**
 * 
 */
public class TokenProvider {
  private static final Logger LOGGER = LogManager.getLogger(TokenProvider.class);

  private static TokenProvider instance = null;

  private final Map<TokenEnum, Integer> tokenToNumberMap;
  private final Map<Integer, TokenEnum> numberToTokenMap;

  /**
   * 
   */
  public static void setup(@Nonnull NetworkEnum network) throws DfxException {
    Objects.requireNonNull(network, "null 'network' not allowed");

    if (null != instance) {
      String errorString = "setup() can only be called once ...";
      LOGGER.error(errorString);
      throw new RuntimeException(errorString);
    }

    instance = new TokenProvider(network);

    if (NetworkEnum.TESTNET == network) {
      instance.tokenToNumberMap.put(TokenEnum.DFI, 0);
      instance.tokenToNumberMap.put(TokenEnum.DUSD, 11);

      instance.tokenToNumberMap.put(TokenEnum.BTC, 1);
      instance.tokenToNumberMap.put(TokenEnum.ETH, 2);
      instance.tokenToNumberMap.put(TokenEnum.USDT, 5);
      instance.tokenToNumberMap.put(TokenEnum.USDC, 22);
      instance.tokenToNumberMap.put(TokenEnum.EUROC, 25);
      instance.tokenToNumberMap.put(TokenEnum.SPY, -1);
    } else {
      instance.tokenToNumberMap.put(TokenEnum.DFI, 0);
      instance.tokenToNumberMap.put(TokenEnum.DUSD, 15);
      instance.tokenToNumberMap.put(TokenEnum.BTC, 2);
      instance.tokenToNumberMap.put(TokenEnum.ETH, 1);
      instance.tokenToNumberMap.put(TokenEnum.USDT, 3);
      instance.tokenToNumberMap.put(TokenEnum.USDC, 13);
      instance.tokenToNumberMap.put(TokenEnum.SPY, 26);
      instance.tokenToNumberMap.put(TokenEnum.EUROC, 216);
    }

    // ...
    for (TokenEnum token : TokenEnum.values()) {
      instance.numberToTokenMap.put(instance.getNumber(token), token);
    }
  }

  /**
   * 
   */
  public static TokenProvider getInstance() {
    return instance;
  }

  /**
   * 
   */
  public @Nullable TokenEnum createWithNumber(int number) {
    return numberToTokenMap.get(number);
  }

  /**
   * 
   */
  public @Nonnull TokenEnum createWithText(
      @Nullable String text,
      @Nonnull TokenEnum defaultToken) {
    try {
      if (null != text) {
        return TokenEnum.valueOf(text.toUpperCase());
      }
    } catch (Throwable t) {
      // Intentionally left blank ...
    }

    return defaultToken;
  }

  /**
   * 
   */
  public int getNumber(@Nonnull TokenEnum token) throws DfxException {
    Integer number = tokenToNumberMap.get(token);

    if (null == number) {
      throw new DfxException("cannot detect token number");
    }

    return number;
  }

  /**
   * 
   */
  private TokenProvider(@Nonnull NetworkEnum network) {
    this.tokenToNumberMap = new HashMap<>();
    this.numberToTokenMap = new HashMap<>();
  }
}
