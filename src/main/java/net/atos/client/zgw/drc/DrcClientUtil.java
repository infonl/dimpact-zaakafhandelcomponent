package net.atos.client.zgw.drc;

import java.util.Base64;

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.drc.model.generated.SoortEnum;

public class DrcClientUtil {

    /**
     * Utility function to convert a byte array to a base64 string as
     * required by the ZGW DRC API.
     *
     * @param byteArray the byte array
     * @return the bas64 converted byte array as string
     */
    public static String convertByteArrayToBase64String(byte[] byteArray) {
        return Base64.getEncoder().encodeToString(byteArray);
    }

    public static boolean isOndertekend(EnkelvoudigInformatieObject enkelvoudigInformatieObject) {
        return enkelvoudigInformatieObject.getOndertekening() != null &&
               enkelvoudigInformatieObject.getOndertekening().getDatum() != null &&
               enkelvoudigInformatieObject.getOndertekening().getSoort() != null &&
               // this extra check is because the API can return an empty ondertekening soort
               // when no signature is present (even if this is not permitted according to the
               // original OpenAPI spec)
               !enkelvoudigInformatieObject.getOndertekening().getSoort().equals(
                       SoortEnum.EMPTY
               );
    }
}
