package net.atos.client.zgw.drc

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.SoortEnum
import java.util.Base64

private const val BASE64_PADDING_CHARACTER = "="

/**
 * Converts a byte array to a base64 string as required by the ZGW DRC API.
 *
 * @return the bas64 converted byte array as string
 */
fun ByteArray.toBase64String(): String {
    return Base64.getEncoder().encodeToString(this)
}

/**
 * Returns the length of the original (base64 decoded) string
 */
fun String.decodedBase64StringLength(): Int {
    return substringBeforeLast(BASE64_PADDING_CHARACTER).length * 3 / 4
}

/**
 * Check if EnkelvoudigInformatieObject is signed
 */
fun EnkelvoudigInformatieObject.isSigned(): Boolean {
    return ondertekening != null
            && ondertekening.datum != null
            && ondertekening.soort != null
            && ondertekening.soort != SoortEnum.EMPTY
}
