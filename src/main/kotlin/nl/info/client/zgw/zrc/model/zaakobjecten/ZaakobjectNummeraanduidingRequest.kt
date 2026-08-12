/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

class ZaakobjectNummeraanduidingRequest(
    zaak: URI,
    bagObjectUri: URI?,
    nummeraanduiding: ObjectNummeraanduiding?
) : ZaakobjectMetObjectIdentificatieRequest<ObjectOverige<ObjectNummeraanduiding>>(
    zaak,
    bagObjectUri,
    ObjectTypeEnum.OVERIGE,
    ObjectOverige(nummeraanduiding)
) {
    init {
        objectTypeOverige = ZaakobjectNummeraanduiding.OBJECT_TYPE_OVERIGE_NUMMERAANDUIDING
    }
}
