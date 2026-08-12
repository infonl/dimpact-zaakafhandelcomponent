/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

/**
 * ZaakobjectWoonplaatsRequest
 */
class ZaakobjectWoonplaatsRequest(
    zaak: URI,
    bagobjectUri: URI?,
    woonplaats: ObjectWoonplaats?
) : ZaakobjectMetObjectIdentificatieRequest<ObjectWoonplaats>(zaak, bagobjectUri, ObjectTypeEnum.WOONPLAATS, woonplaats)
