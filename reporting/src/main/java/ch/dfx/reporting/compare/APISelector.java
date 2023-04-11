package ch.dfx.reporting.compare;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.config.ReportingConfigEnum;
import ch.dfx.reporting.compare.data.APIStakingBalanceDTOList;
import ch.dfx.reporting.compare.data.APITransactionHistoryDTOList;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class APISelector {
  private static final Logger LOGGER = LogManager.getLogger(APISelector.class);

  // ...
  private static final int HTTP_CLIENT_TIMEOUT = 10 * 1000;

  private static File API_CUSTOMER_BALANCE_FILE = new File("data", "api-customer-balance.json");

  // ...
  private final DatabaseBalanceHelper databaseStakingBalanceHelper;
  private final DatabaseBalanceHelper databaseYieldmachineBalanceHelper;

  private final Gson gson;

  // ...
  private Multimap<String, StakingDTO> stakingCustomerToStakingDTOMap = null;
  private Multimap<String, StakingDTO> yieldmachineCustomerToStakingDTOMap = null;
  private Map<String, APIStakingBalanceDTOList> customerToAPIStakingBalanceDTOListMap = null;

  /**
   * 
   */
  public APISelector(
      @Nonnull DatabaseBalanceHelper databaseStakingBalanceHelper,
      @Nonnull DatabaseBalanceHelper databaseYieldmachineBalanceHelper) {

    this.databaseStakingBalanceHelper = databaseStakingBalanceHelper;
    this.databaseYieldmachineBalanceHelper = databaseYieldmachineBalanceHelper;

    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  public Multimap<String, StakingDTO> getStakingCustomerToStakingDTOMap() {
    return stakingCustomerToStakingDTOMap;
  }

  public Multimap<String, StakingDTO> getYieldmachineCustomerToStakingDTOMap() {
    return yieldmachineCustomerToStakingDTOMap;
  }

  public Map<String, APIStakingBalanceDTOList> getCustomerToAPIStakingBalanceDTOListMap() {
    return customerToAPIStakingBalanceDTOListMap;
  }

  /**
   * 
   */
  public void selectAllCustomerBalances() throws DfxException {
    LOGGER.debug("selectAllCustomerBalances()");

    long startTime = System.currentTimeMillis();

    try {
      stakingCustomerToStakingDTOMap = createCustomerToStakingDTOMap(databaseStakingBalanceHelper);
      yieldmachineCustomerToStakingDTOMap = createCustomerToStakingDTOMap(databaseYieldmachineBalanceHelper);

//      doImport();

      Set<String> customerSet = new HashSet<>();
      customerSet.addAll(stakingCustomerToStakingDTOMap.keySet());
      customerSet.addAll(yieldmachineCustomerToStakingDTOMap.keySet());

      // ...
      customerToAPIStakingBalanceDTOListMap = new HashMap<>();

      int totalSize = customerSet.size();
      int counter = 0;

      for (String customerAddress : customerSet) {
        APIStakingBalanceDTOList apiStakingBalanceDTOList = createAPIStakingBalanceDTOList(customerAddress);
        customerToAPIStakingBalanceDTOListMap.put(customerAddress, apiStakingBalanceDTOList);

        counter++;

        LOGGER.debug(counter + " of " + totalSize + " checked");
      }

      // ...
      // doExport();
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("selectAllCustomerBalances", e);
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void doExport() throws DfxException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(API_CUSTOMER_BALANCE_FILE))) {
      writer.append(customerToAPIStakingBalanceDTOListMap.toString());
      writer.append("\n");
    } catch (Exception e) {
      throw new DfxException("doExport", e);
    }
  }

  /**
   * 
   */
  private void doImport() throws DfxException {
    try {
      customerToAPIStakingBalanceDTOListMap = new HashMap<>();

      Map<?, ?> importDataMap = gson.fromJson(new FileReader(API_CUSTOMER_BALANCE_FILE), Map.class);

      for (Entry<?, ?> importDataMapEntry : importDataMap.entrySet()) {
        String customerAddress = (String) importDataMapEntry.getKey();
        List<?> assetList = (List<?>) importDataMapEntry.getValue();

        APIStakingBalanceDTOList apiStakingBalanceDTOList =
            gson.fromJson(assetList.toString(), APIStakingBalanceDTOList.class);

        customerToAPIStakingBalanceDTOListMap.put(customerAddress, apiStakingBalanceDTOList);
      }
    } catch (Exception e) {
      throw new DfxException("doImport", e);
    }
  }

  /**
   * 
   */
  private Multimap<String, StakingDTO> createCustomerToStakingDTOMap(@Nonnull DatabaseBalanceHelper databaseBalanceHelper) throws DfxException {
    LOGGER.trace("createCustomerToStakingDTOMap()");

    List<StakingDTO> stakingDTOList = new ArrayList<>();

    for (TokenEnum token : TokenEnum.values()) {
      stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(token));
    }

    Multimap<String, StakingDTO> customerAddressToStakingDTOMap = ArrayListMultimap.create();

    for (StakingDTO stakingDTO : stakingDTOList) {
      customerAddressToStakingDTOMap.put(stakingDTO.getCustomerAddress(), stakingDTO);
    }

    return customerAddressToStakingDTOMap;
  }

  /**
   * 
   */
  private APIStakingBalanceDTOList createAPIStakingBalanceDTOList(@Nonnull String customerAddress) throws DfxException {
    LOGGER.trace("createAPIStakingBalanceDTOList()");

    String url = ConfigProvider.getInstance().getValue(ReportingConfigEnum.LOCK_API_URL) + "/staking/balance";
    url = url + "?userAddress=" + customerAddress;
    url = url + "&type=json";
    LOGGER.trace("URL: " + url);

    try {
      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectTimeout(HTTP_CLIENT_TIMEOUT)
              .setConnectionRequestTimeout(HTTP_CLIENT_TIMEOUT)
              .setSocketTimeout(HTTP_CLIENT_TIMEOUT).build();

      HttpClient httpClient =
          HttpClientBuilder.create()
              .setDefaultRequestConfig(requestConfig)
              .build();

      HttpGet httpGet = new HttpGet(url);
      HttpResponse httpResponse = httpClient.execute(httpGet);

      int statusCode = httpResponse.getStatusLine().getStatusCode();

      APIStakingBalanceDTOList apiStakingBalanceDTOList;

      if (HttpStatus.SC_NOT_FOUND != statusCode) {
        String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
        apiStakingBalanceDTOList = gson.fromJson(jsonResponse, APIStakingBalanceDTOList.class);

        // ...
        Collection<StakingDTO> stakingStakingDTOCollection = stakingCustomerToStakingDTOMap.get(customerAddress);
        updateStakingDTO(databaseStakingBalanceHelper, stakingStakingDTOCollection);

        Collection<StakingDTO> yieldmachineStakingDTOCollection = yieldmachineCustomerToStakingDTOMap.get(customerAddress);
        updateStakingDTO(databaseYieldmachineBalanceHelper, yieldmachineStakingDTOCollection);
      } else {
        apiStakingBalanceDTOList = new APIStakingBalanceDTOList();
      }

      return apiStakingBalanceDTOList;
    } catch (Exception e) {
      throw new DfxException("createAPIStakingBalanceDTOList: " + url, e);
    }
  }

  /**
   * 
   */
  private void updateStakingDTO(
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper,
      @Nonnull Collection<StakingDTO> stakingDTOCollection) throws DfxException {
    LOGGER.trace("updateStakingDTO()");

    for (StakingDTO stakingDTO : stakingDTOCollection) {
      TokenEnum token = TokenEnum.createWithNumber(stakingDTO.getTokenNumber());

      if (null != token) {
        int customerAddressNumber = stakingDTO.getCustomerAddressNumber();

        List<StakingDTO> currentStakingDTOList =
            databaseBalanceHelper.getStakingDTOListByCustomerAddressNumber(token, customerAddressNumber);

        BigDecimal currentVin = BigDecimal.ZERO;
        BigDecimal currentVout = BigDecimal.ZERO;

        for (StakingDTO currentStakingDTO : currentStakingDTOList) {
          currentVin = currentVin.add(currentStakingDTO.getVin());
          currentVout = currentVout.add(currentStakingDTO.getVout());
        }

        stakingDTO.setVin(currentVin);
        stakingDTO.setVout(currentVout);
      }
    }
  }

  /**
   * 
   */
  public APITransactionHistoryDTOList getDepositTransactionHistoryDTOList(@Nonnull String depositAddress) throws DfxException {
    LOGGER.trace("getDepositTransactionHistoryDTOList()");

    String url = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_URL) + "/analytics/history/Compact";
    url = url + "?depositAddress=" + depositAddress;
    url = url + "&type=json";

    return getTransactionHistoryDTOList(url);
  }

  /**
   * 
   */
  public APITransactionHistoryDTOList getCustomerTransactionHistoryDTOList(@Nonnull String customerAddress) throws DfxException {
    LOGGER.trace("getCustomerTransactionHistoryDTOList()");

    String url = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.LOCK_API_URL) + "/analytics/history/Compact";
    url = url + "?userAddress=" + customerAddress;
    url = url + "&type=json";

    return getTransactionHistoryDTOList(url);
  }

  /**
   * 
   */
  private APITransactionHistoryDTOList getTransactionHistoryDTOList(@Nonnull String url) throws DfxException {
    LOGGER.trace("getTransactionHistoryDTOList()");

    try {
      LOGGER.debug("URL: " + url);

      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectTimeout(HTTP_CLIENT_TIMEOUT)
              .setConnectionRequestTimeout(HTTP_CLIENT_TIMEOUT)
              .setSocketTimeout(HTTP_CLIENT_TIMEOUT).build();

      HttpClient httpClient =
          HttpClientBuilder.create()
              .setDefaultRequestConfig(requestConfig)
              .build();

      HttpGet httpGet = new HttpGet(url);
      HttpResponse httpResponse = httpClient.execute(httpGet);

      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());

      APITransactionHistoryDTOList historyDTOList = gson.fromJson(jsonResponse, APITransactionHistoryDTOList.class);

      // ...
      historyDTOList.sort((d1, d2) -> d2.getDate().compareTo(d1.getDate()));

      return historyDTOList;
    } catch (Exception e) {
      throw new DfxException("getTransactionHistoryDTOList", e);
    }
  }
}
