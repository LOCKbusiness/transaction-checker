package ch.dfx.defichain.data;

import java.math.BigDecimal;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.defichain.enumeration.DefiTokenEnum;

/**
 *
 */
public class DefiAmountData {

  private AmountValue amount;
  private final DefiTokenEnum token;

  /**
   *
   */
  public DefiAmountData(
      @Nonnull AmountValue amount,
      @Nonnull DefiTokenEnum token) {
    Objects.requireNonNull(amount, "null amount is not allowed");
    Objects.requireNonNull(token, "null token is not allowed");

    this.amount = amount;
    this.token = token;
  }

  /**
   *
   */
  public DefiAmountData(
      @Nonnull AmountValue amount,
      @Nonnull String token) {
    Objects.requireNonNull(amount, "null amount is not allowed");
    Objects.requireNonNull(token, "null token is not allowed");

    this.amount = amount;

    if (token.matches("^\\d+$")) {
      this.token = DefiTokenEnum.createByKey(token);
    } else {
      this.token = DefiTokenEnum.createBySymbolKey(token);
    }
  }

  /**
   *
   */
  public DefiAmountData(@Nonnull String amountWithToken) {
    Objects.requireNonNull(amountWithToken, "null amountWithToken is not allowed");

    String[] amountWithTokenArray = amountWithToken.split("\\@");
    this.amount = AmountValue.valueOf(amountWithTokenArray[0]);

    String tokenAsString = amountWithTokenArray[1];

    if (tokenAsString.matches("^\\d+$")) {
      this.token = DefiTokenEnum.createByKey(tokenAsString);
    } else {
      this.token = DefiTokenEnum.createBySymbolKey(tokenAsString);
    }
  }

  public DefiAmountData add(@Nonnull BigDecimal amount) {
    this.amount = this.amount.add(amount);
    return this;
  }

  public AmountValue getAmount() {
    return amount;
  }

  public DefiTokenEnum getToken() {
    return token;
  }

  public String getAmountWithKey() {
    return new StringBuilder().append(amount).append("@").append(token.getKey()).toString();
  }

  public String getAmountWithSymbolKey() {
    return new StringBuilder().append(amount).append("@").append(token.getSymbolKey()).toString();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof DefiAmountData) {
      DefiAmountData otherDefiAmountData = (DefiAmountData) other;

      return (Objects.equals(amount, otherDefiAmountData.amount)
          && Objects.equals(token, otherDefiAmountData.token));
    }

    return false;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(amount)
        .append(token)
        .toHashCode();
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
