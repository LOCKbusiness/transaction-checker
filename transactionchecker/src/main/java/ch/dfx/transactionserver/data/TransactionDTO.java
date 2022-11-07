package ch.dfx.transactionserver.data;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * 
 */
public class TransactionDTO extends DatabaseDTO {
  private final Integer blockNumber;

  private final Integer number;
  private final String transactionId;

  private final List<AddressTransactionOutDTO> addressTransactionOutDTOList;
  private final List<AddressTransactionInDTO> addressTransactionInDTOList;

  /**
   * 
   */
  public TransactionDTO(
      @Nonnull Integer blockNumber,
      @Nonnull Integer number,
      @Nonnull String transactionId) {
    this.blockNumber = blockNumber;
    this.number = number;
    this.transactionId = transactionId;

    this.addressTransactionOutDTOList = new ArrayList<>();
    this.addressTransactionInDTOList = new ArrayList<>();
  }

  public Integer getBlockNumber() {
    return blockNumber;
  }

  public Integer getNumber() {
    return number;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public List<AddressTransactionOutDTO> getAddressTransactionOutDTOList() {
    return addressTransactionOutDTOList;
  }

  /**
   * 
   */
  public void addAddressTransactionOutDTO(@Nonnull AddressTransactionOutDTO addressTransactionOutDTO) {
    this.addressTransactionOutDTOList.add(addressTransactionOutDTO);
  }

  public List<AddressTransactionInDTO> getAddressTransactionInDTOList() {
    return addressTransactionInDTOList;
  }

  /**
   * 
   */
  public void addAddressTransactionInDTO(@Nonnull AddressTransactionInDTO addressTransactionInDTO) {
    this.addressTransactionInDTOList.add(addressTransactionInDTO);
  }
}
