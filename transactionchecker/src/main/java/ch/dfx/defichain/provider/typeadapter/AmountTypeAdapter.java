package ch.dfx.defichain.provider.typeadapter;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import ch.dfx.defichain.data.DefiAmountData;

/**
 * 
 */
public class AmountTypeAdapter implements JsonDeserializer<DefiAmountData> {

  /**
   * 
   */
  public AmountTypeAdapter() {
  }

  /**
   * 
   */
  @Override
  public DefiAmountData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    return new DefiAmountData(json.getAsString());
  }
}
