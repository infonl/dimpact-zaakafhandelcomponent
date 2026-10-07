/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import nl.info.zac.flowable.task.readZaakUUID
import nl.info.zac.flowable.task.readZaaktypeOmschrijving
import nl.info.zac.flowable.util.isOpen
import nl.info.client.opa.model.OpaRuleResult
import nl.info.client.opa.model.RuleQuery
import nl.info.client.opa.model.RuleResponse
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.zrc.util.isIntake
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.client.zgw.zrc.util.isVerlengd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import nl.info.zac.enkelvoudiginformatieobject.util.isSigned
import nl.info.zac.policy.exception.OpaRuleNotConfiguredException
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.input.BrpInput
import nl.info.zac.policy.input.DocumentData
import nl.info.zac.policy.input.DocumentInput
import nl.info.zac.policy.input.TaakData
import nl.info.zac.policy.input.TaakInput
import nl.info.zac.policy.input.UserInput
import nl.info.zac.policy.input.ZaakData
import nl.info.zac.policy.input.ZaakInput
import nl.info.zac.policy.output.DocumentRechten
import nl.info.zac.policy.output.NotitieRechten
import nl.info.zac.policy.output.TaakRechten
import nl.info.zac.policy.output.WerklijstRechten
import nl.info.zac.policy.output.ZaakRechten
import nl.info.zac.search.model.DocumentIndicatie
import nl.info.zac.search.model.ZaakAutorisatieGegevens
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.isOpen
import nl.info.zac.search.model.zoekobject.isZaakOpen
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import nl.info.zac.zaak.readZaakAutorisatieGegevens
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.flowable.task.api.TaskInfo
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class PolicyService @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,
    @RestClient private val evaluationClient: OpaEvaluationClient,
    private val ztcClientService: ZtcClientService,
    private val lockService: EnkelvoudigInformatieObjectLockService,
    private val zrcClientService: ZrcClientService,
    private val zaakspecifiekeAutorisatieService: ZaakspecifiekeAutorisatieService
) {
    /**
     * Read 'overige' permissions.
     *
     * @param zaaktypeDescription Optional zaaktype description to include in the input. In the legacy
     * non-PABC IAM architecture it is not used but in the new PABC-based IAM architecture it is,
     * but only for those 'overige rechten' permissions that are zaaktype-specific.
     */
    fun readOverigeRechten(zaaktypeDescription: String? = null) =
        evaluationClient.readOverigeRechten(
            RuleQuery(
                UserInput(
                    loggedInUser = loggedInUserInstance.get(),
                    zaaktype = zaaktypeDescription,
                )
            )
        ).requireResult(OpaEvaluationClient.OVERIGE_RECHTEN_PATH)

    /**
     * @param zaakAutorisatieGegevens the zaakspecifieke autorisatie data of [zaak], for a caller that already read
     * it in this request. When omitted, it is read here.
     */
    // used for the java class BagRestService and will be removed once BagRestService is migrated to kotlin
    @JvmOverloads
    fun readZaakRechten(
        zaak: Zaak,
        loggedInUser: LoggedInUser,
        zaakAutorisatieGegevens: ZaakAutorisatieGegevens? = null
    ): ZaakRechten {
        val zaakType = ztcClientService.readZaaktype(zaak.zaaktype)
        return readZaakRechten(zaak, zaakType, loggedInUser, zaakAutorisatieGegevens)
    }

    /**
     * @param zaakAutorisatieGegevens the zaakspecifieke autorisatie data of [zaak], for a caller that already read
     * it in this request. When omitted, it is read here.
     */
    fun readZaakRechten(
        zaak: Zaak,
        zaaktype: ZaakType,
        loggedInUser: LoggedInUser,
        zaakAutorisatieGegevens: ZaakAutorisatieGegevens? = null
    ): ZaakRechten {
        val resolvedZaakAutorisatieGegevens = zaakAutorisatieGegevens
            ?: zaakspecifiekeAutorisatieService.readZaakAutorisatieGegevens(zrcClientService, zaak)
        val statusType = zaak.status?.let {
            zrcClientService.readStatus(it).statustype
                .let(ztcClientService::readStatustype)
        }
        val zaakData = ZaakData(
            isOpen = zaak.isOpen(),
            zaaktype = zaaktype.getOmschrijving(),
            isOpgeschort = zaak.isOpgeschort(),
            isVerlengd = zaak.isVerlengd(),
            isBesloten = zaaktype.getBesluittypen()?.isNotEmpty() == true,
            isIntake = statusType?.isIntake(),
            isHeropend = statusType?.isHeropend(),
            isBrondatumBepaald = zaak.startdatumBewaartermijn != null,
            isZaakspecifiekGeautoriseerd = resolvedZaakAutorisatieGegevens.isZaakspecifiekGeautoriseerd,
            isLoggedInUserGeautoriseerdeMedewerker = resolvedZaakAutorisatieGegevens.isGeautoriseerdeMedewerker(loggedInUser.id)
        )
        return evaluationClient.readZaakRechten(
            RuleQuery(
                ZaakInput(
                    loggedInUser = loggedInUser,
                    zaakData = zaakData
                )
            )
        ).requireResult(OpaEvaluationClient.ZAAK_RECHTEN_PATH)
    }

    fun readZaakRechtenForZaakZoekObject(zaakZoekObject: ZaakZoekObject): ZaakRechten {
        val zaakData = ZaakData(
            isOpen = zaakZoekObject.isOpen(),
            zaaktype = zaakZoekObject.zaaktypeOmschrijving,
            isOpgeschort = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.OPSCHORTING),
            isVerlengd = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.VERLENGD),
            isHeropend = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.HEROPEND),
            // not taken into account when searching for a zaak
            isIntake = null,
            // not taken into account when searching for a zaak
            isBesloten = null,
            // not taken into account when searching for a zaak
            isBrondatumBepaald = null,
            isZaakspecifiekGeautoriseerd = zaakZoekObject.isZaakspecifiekGeautoriseerd,
            isLoggedInUserGeautoriseerdeMedewerker = isLoggedInUserGeautoriseerdeMedewerker(
                isZaakspecifiekGeautoriseerd = zaakZoekObject.isZaakspecifiekGeautoriseerd,
                zaakGeautoriseerdeMedewerkers = zaakZoekObject.zaakGeautoriseerdeMedewerkers
            )
        )
        return evaluationClient.readZaakRechten(
            RuleQuery(
                ZaakInput(
                    loggedInUser = loggedInUserInstance.get(),
                    zaakData = zaakData
                )
            )
        ).requireResult(OpaEvaluationClient.ZAAK_RECHTEN_PATH)
    }

    fun readDocumentRechten(enkelvoudigInformatieobject: EnkelvoudigInformatieObject, zaak: Zaak?) =
        readDocumentRechten(
            enkelvoudigInformatieobject = enkelvoudigInformatieobject,
            lock = lockService.findLock(enkelvoudigInformatieobject.getUrl().extractUuid()),
            zaak = zaak
        )

    /**
     * @param zaakAutorisatieGegevens the zaakspecifieke autorisatie data of [zaak], for a caller that already read
     * it in this request. When omitted, it is read here. A document without a [zaak] is never zaakspecifiek
     * geautoriseerd.
     */
    fun readDocumentRechten(
        enkelvoudigInformatieobject: EnkelvoudigInformatieObject,
        lock: EnkelvoudigInformatieObjectLock?,
        zaak: Zaak?,
        zaakAutorisatieGegevens: ZaakAutorisatieGegevens? = null
    ): DocumentRechten {
        val resolvedZaakAutorisatieGegevens = zaakAutorisatieGegevens
            ?: zaak?.let { zaakspecifiekeAutorisatieService.readZaakAutorisatieGegevens(zrcClientService, it) }
        val documentData = DocumentData(
            isDefinitief = enkelvoudigInformatieobject.getStatus() == StatusEnum.DEFINITIEF,
            isVergrendeld = enkelvoudigInformatieobject.getLocked(),
            vergrendeldDoor = lock?.userId,
            isOndertekend = enkelvoudigInformatieobject.isSigned(),
            isZaakOpen = zaak?.isOpen() ?: false,
            zaaktype = zaak?.let { ztcClientService.readZaaktype(it.getZaaktype()).getOmschrijving() },
            isZaakspecifiekGeautoriseerd = resolvedZaakAutorisatieGegevens?.isZaakspecifiekGeautoriseerd == true,
            isLoggedInUserGeautoriseerdeMedewerker =
                resolvedZaakAutorisatieGegevens?.isGeautoriseerdeMedewerker(loggedInUserInstance.get().id) == true
        )
        return evaluationClient.readDocumentRechten(
            RuleQuery(
                DocumentInput(
                    loggedInUser = loggedInUserInstance.get(),
                    documentData = documentData
                )
            )
        ).requireResult(OpaEvaluationClient.DOCUMENT_RECHTEN_PATH)
    }

    fun readDocumentRechten(enkelvoudigInformatieobject: DocumentZoekObject): DocumentRechten {
        val documentData = DocumentData(
            isDefinitief = StatusEnum.DEFINITIEF == enkelvoudigInformatieobject.getStatus(),
            isVergrendeld = enkelvoudigInformatieobject.isIndicatie(DocumentIndicatie.VERGRENDELD),
            vergrendeldDoor = enkelvoudigInformatieobject.vergrendeldDoorGebruikersnaam,
            isZaakOpen = enkelvoudigInformatieobject.isZaakOpen(),
            zaaktype = enkelvoudigInformatieobject.zaaktypeOmschrijving,
            isOndertekend = enkelvoudigInformatieobject.ondertekeningDatum != null,
            isZaakspecifiekGeautoriseerd = enkelvoudigInformatieobject.isZaakspecifiekGeautoriseerd,
            isLoggedInUserGeautoriseerdeMedewerker = isLoggedInUserGeautoriseerdeMedewerker(
                isZaakspecifiekGeautoriseerd = enkelvoudigInformatieobject.isZaakspecifiekGeautoriseerd,
                zaakGeautoriseerdeMedewerkers = enkelvoudigInformatieobject.zaakGeautoriseerdeMedewerkers
            )
        )
        return evaluationClient.readDocumentRechten(
            RuleQuery(
                DocumentInput(
                    loggedInUser = loggedInUserInstance.get(),
                    documentData = documentData
                )
            )
        ).requireResult(OpaEvaluationClient.DOCUMENT_RECHTEN_PATH)
    }

    fun readTaakRechten(taskInfo: TaskInfo): TaakRechten {
        val zaaktypeOmschrijving = readZaaktypeOmschrijving(taskInfo)
        return readTaakRechten(taskInfo, zaaktypeOmschrijving)
    }

    /**
     * @param zaakAutorisatieGegevens the zaakspecifieke autorisatie data of the zaak of [taskInfo], for a caller that
     * already read it in this request. When omitted, it is read here.
     */
    fun readTaakRechten(
        taskInfo: TaskInfo,
        zaaktypeOmschrijving: String,
        zaakAutorisatieGegevens: ZaakAutorisatieGegevens? = null
    ): TaakRechten {
        val resolvedZaakAutorisatieGegevens = zaakAutorisatieGegevens
            ?: zaakspecifiekeAutorisatieService.readZaakAutorisatieGegevens(
                zrcClientService = zrcClientService,
                zaakUuid = readZaakUUID(taskInfo)
            )
        val taakData = TaakData(
            isOpen = taskInfo.isOpen(),
            zaaktype = zaaktypeOmschrijving,
            isZaakspecifiekGeautoriseerd = resolvedZaakAutorisatieGegevens.isZaakspecifiekGeautoriseerd,
            isLoggedInUserGeautoriseerdeMedewerker =
                resolvedZaakAutorisatieGegevens.isGeautoriseerdeMedewerker(loggedInUserInstance.get().id)
        )
        return evaluationClient.readTaakRechten(
            RuleQuery(
                TaakInput(
                    loggedInUser = loggedInUserInstance.get(),
                    taakData = taakData
                )
            )
        ).requireResult(OpaEvaluationClient.TAAK_RECHTEN_PATH)
    }

    fun readTaakRechten(taakZoekObject: TaakZoekObject): TaakRechten {
        val taakData = TaakData(
            isOpen = taakZoekObject.isOpen(),
            zaaktype = taakZoekObject.zaaktypeOmschrijving,
            isZaakspecifiekGeautoriseerd = taakZoekObject.isZaakspecifiekGeautoriseerd,
            isLoggedInUserGeautoriseerdeMedewerker = isLoggedInUserGeautoriseerdeMedewerker(
                isZaakspecifiekGeautoriseerd = taakZoekObject.isZaakspecifiekGeautoriseerd,
                zaakGeautoriseerdeMedewerkers = taakZoekObject.zaakGeautoriseerdeMedewerkers
            )
        )
        return evaluationClient.readTaakRechten(
            RuleQuery(
                TaakInput(
                    loggedInUser = loggedInUserInstance.get(),
                    taakData = taakData
                )
            )
        ).requireResult(OpaEvaluationClient.TAAK_RECHTEN_PATH)
    }

    private fun isLoggedInUserGeautoriseerdeMedewerker(
        isZaakspecifiekGeautoriseerd: Boolean,
        zaakGeautoriseerdeMedewerkers: List<String>?
    ) = ZaakAutorisatieGegevens(isZaakspecifiekGeautoriseerd) { zaakGeautoriseerdeMedewerkers.orEmpty() }
        .isGeautoriseerdeMedewerker(loggedInUserInstance.get().id)

    fun readNotitieRechten(): NotitieRechten =
        evaluationClient.readNotitieRechten(
            RuleQuery(
                UserInput(
                    loggedInUser = loggedInUserInstance.get()
                )
            )
        ).requireResult(OpaEvaluationClient.NOTITIE_RECHTEN_PATH)

    fun readWerklijstRechten(): WerklijstRechten =
        evaluationClient.readWerklijstRechten(
            RuleQuery(
                UserInput(
                    loggedInUser = loggedInUserInstance.get()
                )
            )
        ).requireResult(OpaEvaluationClient.WERKLIJST_RECHTEN_PATH)

    fun readBrpRechten(gemeenteCode: String?) =
        evaluationClient.readBrpRechten(
            RuleQuery(
                BrpInput(
                    loggedInUser = loggedInUserInstance.get(),
                    gemeenteCode = gemeenteCode,
                )
            )
        ).requireResult(OpaEvaluationClient.BRP_RECHTEN_PATH)

    fun readLeesrollen(): Set<String> =
        evaluationClient.readLeesrollen().result
            ?: throw OpaRuleNotConfiguredException(OpaEvaluationClient.LEESROLLEN_PATH)

    private fun <T : OpaRuleResult> RuleResponse<T>.requireResult(rulePath: String): T =
        result ?: throw OpaRuleNotConfiguredException(rulePath)
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

/**
 * Assert that the given policy is true.
 * If it is not, log the event with the passed logger and message and finally throw a [PolicyException].
 */
fun assertPolicy(policy: Boolean, logger: Logger, message: String) {
    if (!policy) {
        logger.info(message)
        throw PolicyException()
    }
}
