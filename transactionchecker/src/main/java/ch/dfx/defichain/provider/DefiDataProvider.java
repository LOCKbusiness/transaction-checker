package ch.dfx.defichain.provider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.block.DefiBlockData;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.data.masternode.DefiMasternodeData;
import ch.dfx.defichain.data.network.PeerInfoData;
import ch.dfx.defichain.data.pool.DefiPoolPairData;
import ch.dfx.defichain.data.price.DefiFixedIntervalPriceData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.vault.DefiListVaultData;
import ch.dfx.defichain.data.vault.DefiVaultData;
import ch.dfx.defichain.data.wallet.DefiLoadWalletData;

/**
 * 
 */
public interface DefiDataProvider {

  Long getConnectionCount() throws DfxException;

  List<PeerInfoData> getPeerInfo() throws DfxException;

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

  DefiBlockData getBlock(@Nonnull String blockHash) throws DfxException;

  DefiTransactionData getTransaction(@Nonnull String transactionId) throws DfxException;

  DefiTransactionData getTransaction(
      @Nonnull String transactionId,
      @Nonnull String blockHash) throws DfxException;

  DefiTransactionData decodeRawTransaction(@Nonnull String hexString) throws DfxException;

  Boolean isAppliedCustomTransaction(
      @Nonnull String transactionId,
      @Nonnull Long blockCount) throws DfxException;

  DefiCustomData decodeCustomTransaction(@Nonnull String hexString) throws DfxException;

  byte getCustomType(@Nonnull String scriptPubKeyHexString) throws DfxException;

  Map<String, DefiMasternodeData> getMasternode(
      @Nonnull String wallet,
      @Nonnull String transactionId) throws DfxException;

  Map<String, String> getMasternodeBlocks(@Nonnull String masternodeId) throws DfxException;

  DefiPoolPairData getPoolPair(@Nonnull String poolId) throws DfxException;

  DefiFixedIntervalPriceData getFixedIntervalPrice(@Nonnull String fixedIntervalPriceId) throws DfxException;

  Map<String, BigDecimal> getActivePriceMap(@Nonnull Set<String> tokenSet) throws DfxException;

  BigDecimal testPoolSwap(@Nonnull String fromToken, @Nonnull String toToken) throws DfxException;

  List<String> getAccount(@Nonnull String address) throws DfxException;

  List<DefiListVaultData> listVaults(@Nonnull String ownerAddress) throws DfxException;

  DefiVaultData getVault(@Nonnull String vaultId) throws DfxException;
}
