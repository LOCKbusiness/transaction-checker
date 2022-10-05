package ch.dfx.defichain.data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * 
 */
public class AmountValue implements Comparable<AmountValue> {
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  private BigDecimal value;

  /**
   * 
   */
  public static final AmountValue ZERO = new AmountValue(0);
  public static final AmountValue ONE = new AmountValue(1);
  public static final AmountValue TWO = new AmountValue(2);
  public static final AmountValue HUNDERED = new AmountValue(100);

  /**
   * 
   */
  public static AmountValue valueOf(@Nonnull String value) {
    return new AmountValue(value);
  }

  /**
   * 
   */
  public static AmountValue valueOf(@Nonnull BigDecimal value) {
    return new AmountValue(value);
  }

  /**
   * 
   */
  public static AmountValue valueOf(double value) {
    return new AmountValue(value);
  }

  /**
   * 
   */
  public static AmountValue valueOf(@Nonnull Double value) {
    return new AmountValue(value);
  }

  /**
   * 
   */
  public static AmountValue valueOf(int value) {
    return new AmountValue(value);
  }

  /**
   * 
   */
  public static AmountValue valueOf(@Nonnull Integer value) {
    return new AmountValue(value);
  }

  /**
   * 
   */
  public static AmountValue valueOf(long value) {
    return new AmountValue(value);
  }

  /**
   * 
   */
  public static AmountValue valueOf(@Nonnull Long value) {
    return new AmountValue(value);
  }

  /**
   * 
   */
  private AmountValue(@Nonnull String value) {
    Objects.requireNonNull(value, "null value is not allowed");
    this.value = new BigDecimal(value).setScale(SCALE, RoundingMode.HALF_UP);
  }

  /**
   * 
   */
  private AmountValue(@Nonnull BigDecimal value) {
    Objects.requireNonNull(value, "null value is not allowed");
    this.value = value.setScale(SCALE, RoundingMode.HALF_UP);
  }

  /**
   * 
   */
  private AmountValue(@Nonnull Double value) {
    Objects.requireNonNull(value, "null value is not allowed");
    this.value = new BigDecimal(value).setScale(SCALE, RoundingMode.HALF_UP);
  }

  /**
   * 
   */
  private AmountValue(@Nonnull Integer value) {
    Objects.requireNonNull(value, "null value is not allowed");
    this.value = new BigDecimal(value).setScale(SCALE, RoundingMode.HALF_UP);
  }

  /**
   * 
   */
  private AmountValue(@Nonnull Long value) {
    Objects.requireNonNull(value, "null value is not allowed");
    this.value = new BigDecimal(value).setScale(SCALE, RoundingMode.HALF_UP);
  }

  /**
   * 
   */
  public BigDecimal getValue() {
    return value;
  }

  /**
   * 
   */
  public AmountValue add(@Nonnull AmountValue augend) {
    Objects.requireNonNull(augend, "null augend is not allowed");
    return add(augend.value);
  }

  /**
   * 
   */
  public AmountValue add(@Nonnull BigDecimal augend) {
    Objects.requireNonNull(augend, "null augend is not allowed");
    return new AmountValue(value.add(augend, MATH_CONTEXT));
  }

  /**
   * 
   */
  public AmountValue subtract(@Nonnull AmountValue subtrahend) {
    Objects.requireNonNull(subtrahend, "null subtrahend is not allowed");
    return subtract(subtrahend.value);
  }

  /**
   * 
   */
  public AmountValue subtract(@Nonnull BigDecimal subtrahend) {
    Objects.requireNonNull(subtrahend, "null subtrahend is not allowed");
    return new AmountValue(value.subtract(subtrahend, MATH_CONTEXT));
  }

  /**
   * 
   */
  public AmountValue divide(@Nonnull AmountValue divisor) {
    Objects.requireNonNull(divisor, "null divisor is not allowed");
    return divide(divisor.value);
  }

  /**
   * 
   */
  public AmountValue divide(@Nonnull BigDecimal divisor) {
    Objects.requireNonNull(divisor, "null divisor is not allowed");
    return new AmountValue(value.divide(divisor, MATH_CONTEXT));
  }

  /**
   * 
   */
  public AmountValue multiply(@Nonnull AmountValue multiplicand) {
    Objects.requireNonNull(multiplicand, "null multiplicand is not allowed");
    return multiply(multiplicand.value);
  }

  /**
   * 
   */
  public AmountValue multiply(@Nonnull BigDecimal multiplicand) {
    Objects.requireNonNull(multiplicand, "null multiplicand is not allowed");
    return new AmountValue(value.multiply(multiplicand, MATH_CONTEXT));
  }

  /**
   * 
   */
  public boolean isGT(@Nonnull AmountValue other) {
    return 0 < compareTo(other);
  }

  /**
   * 
   */
  public boolean isGToE(@Nonnull AmountValue other) {
    return 0 <= compareTo(other);
  }

  /**
   * 
   */
  public boolean isLT(@Nonnull AmountValue other) {
    return 0 > compareTo(other);
  }

  /**
   * 
   */
  public boolean isLToE(@Nonnull AmountValue other) {
    return 0 >= compareTo(other);
  }

  @Override
  public int compareTo(AmountValue other) {
    return this.value.compareTo(other.value);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof AmountValue) {
      AmountValue otherAmountValue = (AmountValue) other;

      return Objects.equals(value, otherAmountValue.value);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value.toPlainString();
  }
}
