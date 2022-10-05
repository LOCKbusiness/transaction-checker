package ch.dfx.defichain.provider;

import java.util.List;

import javax.annotation.Nonnull;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.DefiAmountData;
import ch.dfx.defichain.data.DefiListAccountHistoryData;
import ch.dfx.defichain.data.block.DefiBlockData;
import ch.dfx.defichain.data.block.DefiBlockHeaderData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;

/**
 * 
 */
public interface DefiDataProvider {

  Long getBlockCount() throws DfxException;

  String getBlockHash(@Nonnull Long blockCount) throws DfxException;

  DefiBlockHeaderData getBlockHeader(@Nonnull String blockHash) throws DfxException;

  DefiBlockData getBlock(@Nonnull String blockHash) throws DfxException;

  List<DefiAmountData> getAccount(@Nonnull String address) throws DfxException;

  public List<DefiListAccountHistoryData> listAccountHistory(
      @Nonnull String wallet,
      @Nonnull String address,
      @Nonnull Long blockHeight,
      @Nonnull Long limit) throws DfxException;

  public DefiTransactionData getTransaction(
      @Nonnull String transactionId,
      @Nonnull String blockHash) throws DfxException;

  public DefiTransactionData getTransaction(@Nonnull String transactionId) throws DfxException;

  public DefiTransactionData decodeRawTransaction(@Nonnull String hexString) throws DfxException;
}
