/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.model

import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User

/**
 * The groep and behandelaar a zaak is about to be assigned to, with the groep membership of the behandelaar
 * already validated. Created by [nl.info.zac.zaak.ZaakService.readZaakAssignment], so that a caller can
 * reject an invalid assignment before it writes anything and then apply it without validating it again.
 *
 * @property group the groep to assign the zaak to, or null to leave the groep of the zaak as it is
 * @property user the behandelaar to assign the zaak to, or null to remove the behandelaar
 */
data class ZaakAssignment internal constructor(val group: Group?, val user: User?)
