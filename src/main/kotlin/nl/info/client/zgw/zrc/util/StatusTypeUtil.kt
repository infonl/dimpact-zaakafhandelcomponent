/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_HEROPEND
import nl.info.zac.configuratie.ConfiguratieService.Companion.STATUSTYPE_OMSCHRIJVING_INTAKE

/**
 * Returns `true` if the [StatusType] is not `null` and is equals to '[STATUSTYPE_OMSCHRIJVING_HEROPEND]'.
 * Note that in the ZGW ZRC API a zaak status and therefore also the corresponding [StatusType] may be `null`.
 */
fun StatusType?.isHeropend() = this != null && STATUSTYPE_OMSCHRIJVING_HEROPEND == this.getOmschrijving()

/**
 * Returns `true` if the [StatusType] is not `null` and is equals to '[STATUSTYPE_OMSCHRIJVING_INTAKE]'.
 * Note that in the ZGW ZRC API a zaak status and therefore also the corresponding [StatusType] may be `null`.
 */
fun StatusType?.isIntake() = this != null && STATUSTYPE_OMSCHRIJVING_INTAKE == this.getOmschrijving()
