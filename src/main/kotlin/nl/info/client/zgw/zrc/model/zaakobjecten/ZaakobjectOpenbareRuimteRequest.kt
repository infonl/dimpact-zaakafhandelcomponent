/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

/**
 * ZaakobjectOpenbareRuimteRequest
 */
class ZaakobjectOpenbareRuimteRequest(
    zaak: URI,
    bagobjectURI: URI?,
    objectOpenbareRuimte: ObjectOpenbareRuimte?
) : ZaakobjectMetObjectIdentificatieRequest<ObjectOpenbareRuimte>(zaak, bagobjectURI, ObjectTypeEnum.OPENBARE_RUIMTE, objectOpenbareRuimte)
