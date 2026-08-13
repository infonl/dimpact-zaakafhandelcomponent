/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

class ZaakobjectProductaanvraagRequest(
    zaak: URI,
    productaanvraag: URI?
) : ZaakobjectRequest(zaak, productaanvraag, ObjectTypeEnum.OVERIGE) {
    init {
        objectTypeOverige = ZaakobjectProductaanvraag.OBJECT_TYPE_OVERIGE_PRODUCTAANVRAAG
    }
}
