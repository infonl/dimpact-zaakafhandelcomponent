# PolicyService.kt

Changes are:
- Caffeine read-through caches are created for each of the policy checks that will be done, they are:
  - overigeRechtenCache: Cache<RechtenCacheKey<String>, OverigeRechten>
  - notitieRechtenCache: Cache<String, NotitieRechten>
  - werklijstRechtenCache: Cache<String, WerklijstRechten>
  - zaakRechtenCache: Cache<RechtenCacheKey<ZaakData>, ZaakRechten>
  - taakRechtenCache: Cache<RechtenCacheKey<TaakData>, TaakRechten>
  - documentRechtenCache: Cache<RechtenCacheKey<DocumentData>, DocumentRechten>
  - brpRechtenCache: Cache<RechtenCacheKey<String>, BrpRechten>
- These each wrap calls to `evaluationClient` read rights
- All these caches use the same size and timeout settings


```kotlin
/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.zac.flowable.task.TaakVariabelenService
import nl.info.zac.flowable.util.isOpen
import nl.info.client.opa.model.RuleQuery
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.zrc.util.isIntake
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.client.zgw.zrc.util.isVerlengd
import nl.info.client.zgw.zrc.util.isZaakspecifiekGeautoriseerd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import nl.info.zac.enkelvoudiginformatieobject.util.isSigned
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.input.BrpInput
import nl.info.zac.policy.input.DocumentData
import nl.info.zac.policy.input.DocumentInput
import nl.info.zac.policy.input.TaakData
import nl.info.zac.policy.input.TaakInput
import nl.info.zac.policy.input.UserInput
import nl.info.zac.policy.input.ZaakData
import nl.info.zac.policy.input.ZaakInput
import nl.info.zac.policy.output.BrpRechten
import nl.info.zac.policy.output.DocumentRechten
import nl.info.zac.policy.output.NotitieRechten
import nl.info.zac.policy.output.OverigeRechten
import nl.info.zac.policy.output.TaakRechten
import nl.info.zac.policy.output.WerklijstRechten
import nl.info.zac.policy.output.ZaakRechten
import nl.info.zac.search.model.DocumentIndicatie
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.TaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.isOpen
import nl.info.zac.search.model.zoekobject.isZaakOpen
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.flowable.task.api.TaskInfo
import java.util.concurrent.TimeUnit
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
    private val zgwApiService: ZgwApiService
) {
    companion object {
        private const val NOT_SET = "##~~--NOT-SET--~~##"
        private const val RECHTEN_CACHE_MAX_SIZE = 500L
        private const val RECHTEN_CACHE_EXPIRATION_MINUTES = 30L

        private fun <K : Any, V : Any> createRechtenCache(): Cache<K, V> =
            Caffeine.newBuilder()
                .maximumSize(RECHTEN_CACHE_MAX_SIZE)
                .expireAfterWrite(RECHTEN_CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .recordStats()
                .build()
    }

    /**
     * Every `readXxxRechten` function below sends OPA an input derived entirely from the logged-in
     * user (via [LoggedInUser], resolved once per HTTP session) and, where applicable, a self-contained
     * data snapshot (e.g. [ZaakData]) built from the arguments passed in. So each function's result is
     * a pure function of (user id, snapshot): caching per user cannot serve one user's rights to
     * another, and an entry keyed on a snapshot that no longer matches the current state simply misses
     * the cache rather than returning a stale result.
     */
    private data class RechtenCacheKey<K>(
        val userId: String,
        val cacheKey: K,
    )

    private val overigeRechtenCache: Cache<RechtenCacheKey<String>, OverigeRechten> = createRechtenCache()
    private val notitieRechtenCache: Cache<String, NotitieRechten> = createRechtenCache()
    private val werklijstRechtenCache: Cache<String, WerklijstRechten> = createRechtenCache()
    private val zaakRechtenCache: Cache<RechtenCacheKey<ZaakData>, ZaakRechten> = createRechtenCache()
    private val taakRechtenCache: Cache<RechtenCacheKey<TaakData>, TaakRechten> = createRechtenCache()
    private val documentRechtenCache: Cache<RechtenCacheKey<DocumentData>, DocumentRechten> = createRechtenCache()
    private val brpRechtenCache: Cache<RechtenCacheKey<String>, BrpRechten> = createRechtenCache()

    private fun <K, T> Cache<RechtenCacheKey<K>, T>.get(userId: String, cacheKey: K, readRechten: () -> T): T =
        get(RechtenCacheKey(userId, cacheKey)) { readRechten() }

    /**
     * Read 'overige' permissions.
     *
     * @param zaaktypeDescription Zaaktype description to include in the input or non-zaaktype-specific if null
     */
    fun readOverigeRechten(zaaktypeDescription: String? = null): OverigeRechten {
        val loggedInUser = loggedInUserInstance.get()
        return overigeRechtenCache.get(loggedInUser.id, zaaktypeDescription ?: NOT_SET) {
            evaluationClient.readOverigeRechten(
                RuleQuery(
                    UserInput(
                        loggedInUser = loggedInUser,
                        zaaktype = zaaktypeDescription,
                    )
                )
            ).result
        }
    }

    fun readZaakRechten(zaak: Zaak, loggedInUser: LoggedInUser): ZaakRechten {
        val zaakType = ztcClientService.readZaaktype(zaak.zaaktype)
        return readZaakRechten(zaak, zaakType, loggedInUser)
    }

    fun readZaakRechten(zaak: Zaak, zaaktype: ZaakType, loggedInUser: LoggedInUser): ZaakRechten {
        val statusType = zaak.status?.let {
            zrcClientService.readStatus(it).statustype
                .let(ztcClientService::readStatustype)
        }
        val zaakspecifiekGeautoriseerd = zrcClientService.isZaakspecifiekGeautoriseerd(zaak.uuid)
        val zaakData = ZaakData(
            open = zaak.isOpen(),
            zaaktype = zaaktype.getOmschrijving(),
            opgeschort = zaak.isOpgeschort(),
            verlengd = zaak.isVerlengd(),
            besloten = zaaktype.getBesluittypen()?.isNotEmpty() == true,
            intake = statusType?.isIntake(),
            heropend = statusType?.isHeropend(),
            brondatumBepaald = zaak.startdatumBewaartermijn != null,
            zaakspecifiekGeautoriseerd = zaakspecifiekGeautoriseerd,
            loggedInUserIsGeautoriseerdeMedewerker = zaakspecifiekGeautoriseerd &&
                zaak.isGeautoriseerdeMedewerkerOf(loggedInUser.id)
        )
        return zaakRechtenCache.get(loggedInUser.id, zaakData) {
            evaluationClient.readZaakRechten(
                RuleQuery(
                    ZaakInput(
                        loggedInUser = loggedInUser,
                        zaakData = zaakData
                    )
                )
            ).result
        }
    }

    fun readZaakRechtenForZaakZoekObject(zaakZoekObject: ZaakZoekObject): ZaakRechten {
        val loggedInUser = loggedInUserInstance.get()
        val zaakData = ZaakData(
            open = zaakZoekObject.isOpen(),
            zaaktype = zaakZoekObject.zaaktypeOmschrijving,
            opgeschort = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.OPSCHORTING),
            verlengd = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.VERLENGD),
            heropend = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.HEROPEND),
            // not taken into account when searching for a zaak
            intake = null,
            // not taken into account when searching for a zaak
            besloten = null,
            // not taken into account when searching for a zaak
            brondatumBepaald = null,
            zaakspecifiekGeautoriseerd = zaakZoekObject.isZaakspecifiekGeautoriseerd,
            loggedInUserIsGeautoriseerdeMedewerker = zaakZoekObject.isZaakspecifiekGeautoriseerd &&
                loggedInUser.id in zaakZoekObject.zaakGeautoriseerdeMedewerkers.orEmpty()
        )
        return zaakRechtenCache.get(loggedInUser.id, zaakData) {
            evaluationClient.readZaakRechten(
                RuleQuery(
                    ZaakInput(
                        loggedInUser = loggedInUser,
                        zaakData = zaakData
                    )
                )
            ).result
        }
    }

    fun readDocumentRechten(enkelvoudigInformatieobject: EnkelvoudigInformatieObject, zaak: Zaak?) =
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
        val loggedInUser = loggedInUserInstance.get()
        val zaakspecifiekGeautoriseerd = zaak?.let { zrcClientService.isZaakspecifiekGeautoriseerd(it.uuid) } == true
        val documentData = DocumentData(
            definitief = enkelvoudigInformatieobject.getStatus() == StatusEnum.DEFINITIEF,
            vergrendeld = enkelvoudigInformatieobject.getLocked(),
            vergrendeldDoor = lock?.userId,
            ondertekend = enkelvoudigInformatieobject.isSigned(),
            zaakOpen = zaak?.isOpen() ?: false,
            zaaktype = zaak?.let { ztcClientService.readZaaktype(it.getZaaktype()).getOmschrijving() },
            zaakspecifiekGeautoriseerd = zaakspecifiekGeautoriseerd,
            loggedInUserIsGeautoriseerdeMedewerker = zaakspecifiekGeautoriseerd &&
                zaak.isGeautoriseerdeMedewerkerOf(loggedInUser.id)
        )
        return documentRechtenCache.get(loggedInUser.id, documentData) {
            evaluationClient.readDocumentRechten(
                RuleQuery(
                    DocumentInput(
                        loggedInUser = loggedInUser,
                        documentData = documentData
                    )
                )
            ).result
        }
    }

    fun readDocumentRechten(enkelvoudigInformatieobject: DocumentZoekObject): DocumentRechten {
        val loggedInUser = loggedInUserInstance.get()
        val documentData = DocumentData(
            definitief = StatusEnum.DEFINITIEF == enkelvoudigInformatieobject.getStatus(),
            vergrendeld = enkelvoudigInformatieobject.isIndicatie(DocumentIndicatie.VERGRENDELD),
            vergrendeldDoor = enkelvoudigInformatieobject.vergrendeldDoorGebruikersnaam,
            zaakOpen = enkelvoudigInformatieobject.isZaakOpen(),
            zaaktype = enkelvoudigInformatieobject.zaaktypeOmschrijving,
            ondertekend = enkelvoudigInformatieobject.ondertekeningDatum != null,
            zaakspecifiekGeautoriseerd = enkelvoudigInformatieobject.isZaakspecifiekGeautoriseerd,
            loggedInUserIsGeautoriseerdeMedewerker = enkelvoudigInformatieobject.isZaakspecifiekGeautoriseerd &&
                loggedInUser.id in enkelvoudigInformatieobject.zaakGeautoriseerdeMedewerkers.orEmpty()
        )
        return documentRechtenCache.get(loggedInUser.id, documentData) {
            evaluationClient.readDocumentRechten(
                RuleQuery(
                    DocumentInput(
                        loggedInUser = loggedInUser,
                        documentData = documentData
                    )
                )
            ).result
        }
    }

    fun readTaakRechten(taskInfo: TaskInfo): TaakRechten {
        val zaaktypeOmschrijving = TaakVariabelenService.readZaaktypeOmschrijving(taskInfo)
        return readTaakRechten(taskInfo, zaaktypeOmschrijving)
    }

    fun readTaakRechten(
        taskInfo: TaskInfo,
        zaaktypeOmschrijving: String
    ): TaakRechten {
        val loggedInUser = loggedInUserInstance.get()
        val zaakUUID = TaakVariabelenService.readZaakUUID(taskInfo)
        val zaakspecifiekGeautoriseerd = zrcClientService.isZaakspecifiekGeautoriseerd(zaakUUID)
        val taakData = TaakData(
            open = taskInfo.isOpen(),
            zaaktype = zaaktypeOmschrijving,
            zaakspecifiekGeautoriseerd = zaakspecifiekGeautoriseerd,
            loggedInUserIsGeautoriseerdeMedewerker = zaakspecifiekGeautoriseerd &&
                zrcClientService.readZaak(zaakUUID).isGeautoriseerdeMedewerkerOf(loggedInUser.id)
        )
        return taakRechtenCache.get(loggedInUser.id, taakData) {
            evaluationClient.readTaakRechten(
                RuleQuery(
                    TaakInput(
                        loggedInUser = loggedInUser,
                        taakData = taakData
                    )
                )
            ).result
        }
    }

    fun readTaakRechten(taakZoekObject: TaakZoekObject): TaakRechten {
        val loggedInUser = loggedInUserInstance.get()
        val taakData = TaakData(
            open = taakZoekObject.isOpen(),
            zaaktype = taakZoekObject.zaaktypeOmschrijving,
            zaakspecifiekGeautoriseerd = taakZoekObject.isZaakspecifiekGeautoriseerd,
            loggedInUserIsGeautoriseerdeMedewerker = taakZoekObject.isZaakspecifiekGeautoriseerd &&
                loggedInUser.id in taakZoekObject.zaakGeautoriseerdeMedewerkers.orEmpty()
        )
        return taakRechtenCache.get(loggedInUser.id, taakData) {
            evaluationClient.readTaakRechten(
                RuleQuery(
                    TaakInput(
                        loggedInUser = loggedInUser,
                        taakData = taakData
                    )
                )
            ).result
        }
    }

    private fun Zaak.isGeautoriseerdeMedewerkerOf(userId: String) =
        zgwApiService.findBehandelaarMedewerkerRoleForZaak(this)?.betrokkeneIdentificatie?.identificatie == userId

    fun readNotitieRechten(): NotitieRechten {
        val loggedInUser = loggedInUserInstance.get()
        return notitieRechtenCache.get(loggedInUser.id) {
            evaluationClient.readNotitieRechten(
                RuleQuery(
                    UserInput(
                        loggedInUser = loggedInUser
                    )
                )
            ).result
        }
    }

    fun readWerklijstRechten(): WerklijstRechten {
        val loggedInUser = loggedInUserInstance.get()
        return werklijstRechtenCache.get(loggedInUser.id) {
            evaluationClient.readWerklijstRechten(
                RuleQuery(
                    UserInput(
                        loggedInUser = loggedInUser
                    )
                )
            ).result
        }
    }

    fun readBrpRechten(gemeenteCode: String?): BrpRechten {
        val loggedInUser = loggedInUserInstance.get()
        return brpRechtenCache.get(loggedInUser.id, gemeenteCode ?: NOT_SET) {
            evaluationClient.readBrpRechten(
                RuleQuery(
                    BrpInput(
                        loggedInUser = loggedInUser,
                        gemeenteCode = gemeenteCode,
                    )
                )
            ).result
        }
    }
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
```
