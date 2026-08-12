/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

class ZaakobjectAdresRequest(
    zaak: URI,
    bagobjectURI: URI?,
    adres: ObjectAdres?
) : ZaakobjectMetObjectIdentificatieRequest<ObjectAdres>(zaak, bagobjectURI, ObjectTypeEnum.ADRES, adres)
