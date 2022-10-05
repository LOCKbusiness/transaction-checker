package ch.dfx.lockbusiness.stakingbalances.ocean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.lockbusiness.stakingbalances.ocean.data.ListTransactionsData;
import ch.dfx.lockbusiness.stakingbalances.ocean.data.TransactionDetailVinData;
import ch.dfx.lockbusiness.stakingbalances.ocean.data.TransactionDetailVoutData;
import ch.dfx.lockbusiness.stakingbalances.ocean.data.TransactionsData;
import ch.dfx.lockbusiness.stakingbalances.ocean.data.TransactionsDetailData;
import ch.dfx.lockbusiness.stakingbalances.ocean.data.TransactionsPageData;

/**
 * 
 */
public class OceanHandler {
  private static final Logger LOGGER = LogManager.getLogger(OceanHandler.class);

  // ...
  private static final String OCEAN_URI = "https://ocean.defichain.com/v0/mainnet/address/";
  private static final int OCEAN_FETCH_SIZE = 200;

  // ...
  private BufferedWriter writer = null;

  // ...
  private final Gson gson;

  private final HttpClient httpClient;
  private final HttpGet httpGet;

  private String address = null;
  private BigDecimal vinBalance = null;
  private BigDecimal voutBalance = null;

  /**
   * 
   */
  public OceanHandler() {
    this.gson = new GsonBuilder().setPrettyPrinting().create();

    this.httpClient = HttpClientBuilder.create().build();
    this.httpGet = new HttpGet();
  }

  /**
   * 
   */
  public void setup(String address) {
    this.address = address;
    this.vinBalance = BigDecimal.ZERO;
    this.voutBalance = BigDecimal.ZERO;
  }

  public BigDecimal getVinBalance() {
    return vinBalance;
  }

  public BigDecimal getVoutBalance() {
    return voutBalance;
  }

  /**
   * 
   */
  public void openWriter(File outputFile) throws DfxException {
    try {
      if (null == writer) {
        writer = new BufferedWriter(new FileWriter(outputFile));
        writer.append("{\"datalist\": [\n");
      }
    } catch (Exception e) {
      throw new DfxException("openWriter", e);
    }
  }

  /**
   * 
   */
  public void closeWriter() {
    try {
      if (null != writer) {
        writer.append("]}");
        writer.flush();
        writer.close();
        writer = null;
      }
    } catch (Exception e) {
      LOGGER.error("closeWriter", e);
    }
  }

  /**
   * 
   */
  public String webcall(String next) throws DfxException {
    try {
      StringBuilder oceanURIBuilder = new StringBuilder()
          .append(OCEAN_URI)
          .append(address)
          .append("/transactions?size=").append(OCEAN_FETCH_SIZE);

      if (null != next) {
        oceanURIBuilder.append("&next=").append(next);
      }

      String oceanURI = oceanURIBuilder.toString();
      LOGGER.debug("URI: " + oceanURI);

      httpGet.setURI(new URI(oceanURI));

      HttpResponse httpResponse = httpClient.execute(httpGet);
      HttpEntity httpEntity = httpResponse.getEntity();

      String jsonResponse = EntityUtils.toString(httpEntity);

      TransactionsData transactionsData = gson.fromJson(jsonResponse, TransactionsData.class);

      // ...
      if (null != writer) {
        writer.append(transactionsData.toString());

        if (null != transactionsData.getPage()) {
          writer.append(",");
        }

        writer.append("\n");
      }

      return analyzeTransactions(transactionsData);
    } catch (Exception e) {
      throw new DfxException("error", e);
    }
  }

  /**
   * 
   */
  public ListTransactionsData readFromFile(File jsonFile) throws DfxException {
    try {
      ListTransactionsData transactionsDataList = gson.fromJson(new FileReader(jsonFile), ListTransactionsData.class);

      for (TransactionsData transactionsData : transactionsDataList.getDatalist()) {
        analyzeTransactions(transactionsData);
      }

      return transactionsDataList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("readFromFile", e);
    }
  }

  /**
   * 
   */
  private String analyzeTransactions(TransactionsData transactionsData) throws DfxException {
    List<TransactionsDetailData> transactionsDetailDataList = transactionsData.getData();

    for (TransactionsDetailData transactionsDetailData : transactionsDetailDataList) {
      if (0 == transactionsDetailData.getTokenId()) {
        TransactionDetailVinData transactionDetailVinData = transactionsDetailData.getVin();
        TransactionDetailVoutData transactionDetailVoutData = transactionsDetailData.getVout();

        if (null != transactionDetailVinData) {
          vinBalance = vinBalance.add(transactionsDetailData.getValue());
        } else if (null != transactionDetailVoutData) {
          voutBalance = voutBalance.add(transactionsDetailData.getValue());
        } else {
          throw new DfxException("no vin and vout found...");
        }
      } else {
        throw new DfxException("unknown token ...");
      }
    }

    String next = null;

    TransactionsPageData transactionPageData = transactionsData.getPage();

    if (null != transactionPageData) {
      next = transactionPageData.getNext();
    }

    return next;
  }
}
