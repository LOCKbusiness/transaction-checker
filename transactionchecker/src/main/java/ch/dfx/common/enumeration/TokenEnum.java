package ch.dfx.common.enumeration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.TokenProvider;

/**
 * DFI: Staking
 * DUSD: Yield Machine
 */
public enum TokenEnum {
  DFI,
  DUSD;

  /**
   * 
   */
  public static @Nullable TokenEnum createWithNumber(int number) {
    return TokenProvider.getInstance().createWithNumber(number);
  }

  /**
   * 
   */
  public static @Nonnull TokenEnum createWithText(
      @Nullable String text,
      @Nonnull TokenEnum defaultToken) {
    return TokenProvider.getInstance().createWithText(text, defaultToken);
  }

  /**
   * 
   */
  public int getNumber() throws DfxException {
    return TokenProvider.getInstance().getNumber(this);
  }
}
