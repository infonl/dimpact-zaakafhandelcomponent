/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.formulieren

import jakarta.inject.Inject
import jakarta.json.Json
import jakarta.json.JsonArray
import jakarta.json.JsonObject
import jakarta.json.JsonString
import jakarta.json.JsonValue
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.app.formulieren.model.FormulierData
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.app.task.model.RestTask
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.shared.helper.SuspensionZaakHelper
import org.flowable.task.api.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

