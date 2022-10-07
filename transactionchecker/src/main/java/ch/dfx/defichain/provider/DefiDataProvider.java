package ch.dfx.defichain.provider;

import java.util.List;

import javax.annotation.Nonnull;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.DefiAmountData;
import ch.dfx.defichain.data.DefiListAccountHistoryData;
import ch.dfx.defichain.data.block.DefiBlockData;
import ch.dfx.defichain.data.block.DefiBlockHeaderData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.wallet.DefiLoadWalletData;

/**
 * 
 */
public interface DefiDataProvider {

  DefiLoadWalletData loadWallet(@Nonnull String wallet) throws DfxException;

  String unloadWallet(@Nonnull String wallet) throws DfxException;

  List<String> listWallets() throws DfxException;

  String walletPassphrase(
      @Nonnull String wallet,
      @Nonnull String passphrase,
      int timeInSeconds) throws DfxException;

  String walletLock(@Nonnull String wallet) throws DfxException;

  String signMessage(
      @Nonnull String wallet,
      @Nonnull String address,
      @Nonnull String message) throws DfxException;

  Boolean verifyMessage(
      @Nonnull String address,
      @Nonnull String signature,
      @Nonnull String message) throws DfxException;

  Long getBlockCount() throws DfxException;

  String getBlockHash(@Nonnull Long blockCount) throws DfxException;

  DefiBlockHeaderData getBlockHeader(@Nonnull String blockHash) throws DfxException;

  DefiBlockData getBlock(@Nonnull String blockHash) throws DfxException;

  List<DefiAmountData> getAccount(@Nonnull String address) throws DfxException;

  List<DefiListAccountHistoryData> listAccountHistory(
      @Nonnull String wallet,
      @Nonnull String address,
      @Nonnull Long blockHeight,
      @Nonnull Long limit) throws DfxException;

  DefiTransactionData getTransaction(
      @Nonnull String transactionId,
      @Nonnull String blockHash) throws DfxException;

  DefiTransactionData getTransaction(@Nonnull String transactionId) throws DfxException;

  DefiTransactionData decodeRawTransaction(@Nonnull String hexString) throws DfxException;
}
