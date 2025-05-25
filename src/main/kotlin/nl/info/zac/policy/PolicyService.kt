/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.client.opa.model.RuleQuery
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.util.TaskUtil
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.zrc.util.isIntake
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import nl.info.zac.enkelvoudiginformatieobject.util.isSigned
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.input.DocumentData
import nl.info.zac.policy.input.DocumentInput
import nl.info.zac.policy.input.TaakData
import nl.info.zac.policy.input.TaakInput
import nl.info.zac.policy.input.UserInput
import nl.info.zac.policy.input.ZaakData
import nl.info.zac.policy.input.ZaakInput
import nl.info.zac.policy.output.DocumentRechten
import nl.info.zac.policy.output.OverigeRechten
import nl.info.zac.policy.output.TaakRechten
import nl.info.zac.policy.output.WerklijstRechten
import nl.info.zac.policy.output.ZaakRechten
import nl.info.zac.search.model.DocumentIndicatie
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.flowable.task.api.TaskInfo

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class PolicyService @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,
    @RestClient private val evaluationClient: OpaEvaluationClient,
    private val ztcClientService: ZtcClientService,
    private val lockService: EnkelvoudigInformatieObjectLockService,
    private val zrcClientService: ZrcClientService
) {
    fun readOverigeRechten(): OverigeRechten =
        evaluationClient.readOverigeRechten(
            RuleQuery(UserInput(loggedInUserInstance.get()))
        ).getResult()

    fun readZaakRechten(zaak: Zaak): ZaakRechten {
        val zaakType = ztcClientService.readZaaktype(zaak.zaaktype)
        return readZaakRechten(zaak, zaakType)
    }

    fun readZaakRechten(zaak: Zaak, zaaktype: ZaakType): ZaakRechten {
        val statusType = zaak.status?.let {
            ztcClientService.readStatustype(zrcClientService.readStatus(it).statustype)
        }
        val zaakData = ZaakData().apply {
            this.open = zaak.isOpen
            this.zaaktype = zaaktype.getOmschrijving()
            this.opgeschort = zaak.isOpgeschort
            this.verlengd = zaak.isVerlengd
            this.besloten = zaaktype.getBesluittypen()?.isNotEmpty() == true
            this.intake = statusType.isIntake()
            this.heropend = statusType.isHeropend()
        }
        return evaluationClient.readZaakRechten(
            RuleQuery(
                ZaakInput(loggedInUserInstance.get(), zaakData)
            )
        ).getResult()
    }

    fun readZaakRechtenForZaakZoekObject(zaakZoekObject: ZaakZoekObject): ZaakRechten {
        val zaakData = ZaakData().apply {
            this.open = !zaakZoekObject.isAfgehandeld
            this.zaaktype = zaakZoekObject.zaaktypeOmschrijving
            this.opgeschort = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.OPSCHORTING)
            this.verlengd = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.VERLENGD)
            this.heropend = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.HEROPEND)
        }
        return evaluationClient.readZaakRechten(
            RuleQuery(ZaakInput(loggedInUserInstance.get(), zaakData))
        ).getResult()
    }

    fun readDocumentRechten(enkelvoudigInformatieobject: EnkelvoudigInformatieObject, zaak: Zaak? = null) =
        readDocumentRechten(
            enkelvoudigInformatieobject = enkelvoudigInformatieobject,
            lock = lockService.findLock(enkelvoudigInformatieobject.getUrl().extractUuid()),
            zaak = zaak
        )

    fun readDocumentRechten(
        enkelvoudigInformatieobject: EnkelvoudigInformatieObject,
        lock: EnkelvoudigInformatieObjectLock?,
        zaak: Zaak?
    ): DocumentRechten {
        val documentData = DocumentData().apply {
            this.definitief = enkelvoudigInformatieobject.getStatus() == StatusEnum.DEFINITIEF
            this.vergrendeld = enkelvoudigInformatieobject.getLocked()
            this.vergrendeldDoor = lock?.userId
            this.ondertekend = enkelvoudigInformatieobject.isSigned()
            zaak?.let {
                this.zaakOpen = it.isOpen()
                this.zaaktype = ztcClientService.readZaaktype(it.getZaaktype()).getOmschrijving()
            }
        }
        return evaluationClient.readDocumentRechten(
            RuleQuery(DocumentInput(loggedInUserInstance.get(), documentData))
        ).getResult()
    }

    fun readDocumentRechten(enkelvoudigInformatieobject: DocumentZoekObject): DocumentRechten {
        val documentData = DocumentData().apply {
            this.definitief = StatusEnum.DEFINITIEF == enkelvoudigInformatieobject.getStatus()
            this.vergrendeld = enkelvoudigInformatieobject.isIndicatie(DocumentIndicatie.VERGRENDELD)
            this.vergrendeldDoor = enkelvoudigInformatieobject.vergrendeldDoorGebruikersnaam
            this.zaakOpen = !enkelvoudigInformatieobject.isZaakAfgehandeld
            this.zaaktype = enkelvoudigInformatieobject.zaaktypeOmschrijving
            this.ondertekend = enkelvoudigInformatieobject.ondertekeningDatum != null
        }
        return evaluationClient.readDocumentRechten(
            RuleQuery(DocumentInput(loggedInUserInstance.get(), documentData))
        ).getResult()
    }

    fun readTaakRechten(taskInfo: TaskInfo): TaakRechten {
        val zaaktypeOmschrijving = TaakVariabelenService.readZaaktypeOmschrijving(taskInfo)
        return readTaakRechten(taskInfo, zaaktypeOmschrijving)
    }

    fun readTaakRechten(
        taskInfo: TaskInfo,
        zaaktypeOmschrijving: String?
    ): TaakRechten {
        val taakData = TaakData().apply {
            this.open = TaskUtil.isOpen(taskInfo)
            this.zaaktype = zaaktypeOmschrijving
        }
        return evaluationClient.readTaakRechten(
            RuleQuery(TaakInput(loggedInUserInstance.get(), taakData))
        ).getResult()
    }

    fun readTaakRechten(taakZoekObject: TaakZoekObject): TaakRechten {
        val taakData = TaakData().apply {
            this.zaaktype = taakZoekObject.zaaktypeOmschrijving
        }
        return evaluationClient.readTaakRechten(
            RuleQuery(TaakInput(loggedInUserInstance.get(), taakData))
        ).getResult()
    }

    fun readNotitieRechten() =
        evaluationClient.readNotitieRechten(
            RuleQuery(UserInput(loggedInUserInstance.get()))
        ).getResult()

    fun readWerklijstRechten(): WerklijstRechten =
        evaluationClient.readWerklijstRechten(
            RuleQuery(UserInput(loggedInUserInstance.get()))
        ).getResult()
}

/**
 * Assert that the given policy is true.
 * If it is not, throw a [PolicyException].
 */
fun assertPolicy(policy: Boolean) {
    if (!policy) {
        throw PolicyException()
    }
}
