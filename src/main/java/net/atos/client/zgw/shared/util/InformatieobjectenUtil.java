package net.atos.client.zgw.shared.util;

import java.util.Base64;


public class InformatieobjectenUtil {

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
}
