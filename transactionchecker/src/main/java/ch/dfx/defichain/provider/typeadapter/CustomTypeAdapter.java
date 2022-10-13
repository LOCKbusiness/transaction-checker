package ch.dfx.defichain.provider.typeadapter;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import ch.dfx.defichain.data.ResultDataA;
import ch.dfx.defichain.data.basic.DefiStringResultData;
import ch.dfx.defichain.data.custom.DefiCustomResultData;

/**
 * 
 */
public class CustomTypeAdapter implements JsonDeserializer<ResultDataA> {

  /**
   * 
   */
  public CustomTypeAdapter() {
  }

  /**
   * 
   */
  @Override
  public ResultDataA deserialize(
      JsonElement json,
      Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    ResultDataA resultData = null;

    if (json.isJsonObject()) {
      JsonObject jsonObject = (JsonObject) json;
      JsonElement jsonResultElement = jsonObject.get("result");

      if (jsonResultElement.isJsonObject()) {
        resultData = context.deserialize(json, DefiCustomResultData.class);
      } else if (jsonResultElement.isJsonPrimitive()) {
        resultData = context.deserialize(json, DefiStringResultData.class);
      }
    }

    if (null == resultData) {
      throw new JsonParseException("cannot parse custom data");
    }

    return resultData;
  }
}
