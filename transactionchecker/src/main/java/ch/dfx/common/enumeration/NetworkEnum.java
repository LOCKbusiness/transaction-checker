package ch.dfx.common.enumeration;

/**
 * 
 */
public enum NetworkEnum {
  TESTNET,
  STAGNET,
  MAINNET;

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
