/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.util.ValidationUtil
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.client.zgw.ztc.model.generated.ResultaatType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.exception.ZaaktypeInUseException
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCmmnBetrokkeneParameters
import nl.info.zac.admin.model.ZaaktypeCmmnBrpParameters
import nl.info.zac.admin.model.ZaaktypeCmmnCompletionParameters
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration.Companion.CREATIEDATUM
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration.Companion.PRODUCTAANVRAAGTYYPE
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration.Companion.ZAAKTYPE_OMSCHRIJVING
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration.Companion.ZAAKTYPE_UUID
import nl.info.zac.admin.model.ZaaktypeCmmnEmailParameters
import nl.info.zac.admin.model.ZaaktypeCmmnHumantaskParameters
import nl.info.zac.admin.model.ZaaktypeCmmnMailtemplateParameters
import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters
import nl.info.zac.admin.model.ZaaktypeCmmnZaakafzenderParameters
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID
import java.util.logging.Logger
import kotlin.apply

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions")
class ZaaktypeCmmnConfigurationBeheerService @Inject constructor(
    private val entityManager: EntityManager,
    private val ztcClientService: ZtcClientService,
    private val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService,
    private val smartDocumentsTemplatesService: SmartDocumentsTemplatesService,
    private val zaaktypeBpmnConfigurationService: ZaaktypeBpmnConfigurationService
) {
    companion object {
        private val LOG = Logger.getLogger(ZaaktypeCmmnConfigurationBeheerService::class.java.name)
    }

    /**
     * Retrieves the zaaktypeCmmnConfiguration for a given zaaktype UUID.
     * Note that a zaaktype UUID uniquely identifies a _version_ of a zaaktype, and therefore also
     * a specific version of the corresponding zaaktypeCmmnConfiguration.
     *
     * @return the found [ZaaktypeCmmnConfiguration] or a **new instance** if no such record exists yet.
     */
    fun fetchZaaktypeCmmnConfiguration(zaaktypeUUID: UUID): ZaaktypeCmmnConfiguration {
        ztcClientService.resetCacheTimeToNow()
        return readZaaktypeCmmnConfiguration(zaaktypeUUID) ?: ZaaktypeCmmnConfiguration().apply {
            zaakTypeUUID = zaaktypeUUID
        }
    }

    /**
     * Retrieves the zaaktypeCmmnConfiguration for a given zaaktype UUID.
     * Note that a zaaktype UUID uniquely identifies a _version_ of a zaaktype, and therefore also
     * a specific version of the corresponding zaaktypeCmmnConfiguration.
     *
     * @return the found [ZaaktypeCmmnConfiguration] or `null` if no such record exists yet.
     */
    fun readZaaktypeCmmnConfiguration(zaaktypeUUID: UUID): ZaaktypeCmmnConfiguration? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaaktypeCmmnConfiguration::class.java)
        val root = query.from(ZaaktypeCmmnConfiguration::class.java)
        query.select(root).where(builder.equal(root.get<Any>(ZAAKTYPE_UUID), zaaktypeUUID))
        val resultList = entityManager.createQuery(query).setMaxResults(1).resultList
        return resultList.firstOrNull()
    }

    fun listZaaktypeCmmnConfiguration(): List<ZaaktypeCmmnConfiguration> {
        val query = entityManager.criteriaBuilder.createQuery(ZaaktypeCmmnConfiguration::class.java)
        val root = query.from(ZaaktypeCmmnConfiguration::class.java)
        query.select(root).orderBy(entityManager.criteriaBuilder.desc(root.get<Any>("id")))
        return entityManager.createQuery(query).resultList
    }

    fun storeZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration): ZaaktypeCmmnConfiguration {
        ValidationUtil.valideerObject(zaaktypeCmmnConfiguration)

        zaaktypeCmmnConfiguration.zaakTypeUUID?.let { uuid ->
            zaaktypeBpmnConfigurationService
                .findZaaktypeProcessDefinitionByZaaktypeUuid(uuid)
                ?.let {
                    throw ZaaktypeInUseException(
                        "BPMN configuration for zaaktype '${zaaktypeCmmnConfiguration.zaaktypeOmschrijving} already exists"
                    )
                }
        }

        zaaktypeCmmnConfigurationService.clearListCache()
        zaaktypeCmmnConfiguration.apply {
            getHumanTaskParametersCollection().forEach { ValidationUtil.valideerObject(it) }
            getUserEventListenerParametersCollection().forEach { ValidationUtil.valideerObject(it) }
            getMailtemplateKoppelingen().forEach { ValidationUtil.valideerObject(it) }
            creatiedatum = creatiedatum ?: ZonedDateTime.now()
        }

        return if (zaaktypeCmmnConfiguration.id == null) {
            entityManager.persist(zaaktypeCmmnConfiguration)
            zaaktypeCmmnConfiguration
        } else {
            entityManager.merge(zaaktypeCmmnConfiguration)
        }
    }

    /**
     * Finds the active [ZaaktypeCmmnConfiguration] for the specified productaanvraag type.
     * If multiple active zaaktypeCmmnConfiguration are found, this indicates an
     * error in the configuration of zaaktypeCmmnConfiguration.
     * There should be at most only one active zaaktypeCmmnConfiguration for each productaanvraagtype.
     *
     * @return the list of found [ZaaktypeCmmnConfiguration] or an empty list if none are found
     */
    fun findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
        productaanvraagType: String
    ): List<ZaaktypeCmmnConfiguration> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaaktypeCmmnConfiguration::class.java)
        val root = query.from(ZaaktypeCmmnConfiguration::class.java)
        val subquery = query.subquery(Date::class.java)
        val subqueryRoot = subquery.from(ZaaktypeCmmnConfiguration::class.java)
        subquery.select(builder.greatest(subqueryRoot.get(CREATIEDATUM)))
            .where(
                builder.equal(subqueryRoot.get<String>(ZAAKTYPE_OMSCHRIJVING), root.get<String>(ZAAKTYPE_OMSCHRIJVING))
            )
        query.select(root).where(
            builder.and(
                builder.equal(root.get<String>(PRODUCTAANVRAAGTYYPE), productaanvraagType),
                builder.equal(root.get<String>(CREATIEDATUM), subquery)
            )
        )
        return entityManager.createQuery(query).resultList
    }

    fun listZaakbeeindigRedenen(): List<ZaakbeeindigReden> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaakbeeindigReden::class.java)
        val root = query.from(ZaakbeeindigReden::class.java)
        query.orderBy(builder.asc(root.get<Any>("naam")))
        return entityManager.createQuery(query).resultList
    }

    @SuppressWarnings("ReturnCount")
    fun upsertZaaktypeCmmnConfiguration(zaaktypeUri: URI) {
        zaaktypeCmmnConfigurationService.clearListCache()
        ztcClientService.clearZaaktypeCache()

        val zaaktype = ztcClientService.readZaaktype(zaaktypeUri)
        val zaaktypeUuid = zaaktype.url.extractUuid()
        if (zaaktype.concept) {
            LOG.warning { "Zaak type with UUID $zaaktypeUuid is still a concept. Ignoring" }
            return
        }

        val zaaktypeCmmnConfiguration = currentZaaktypeCmmnConfiguration(zaaktypeUuid)
        zaaktypeCmmnConfiguration.apply {
            zaaktypeOmschrijving = zaaktype.omschrijving
            einddatumGeplandWaarschuwing = zaaktypeCmmnConfiguration.einddatumGeplandWaarschuwing.takeIf {
                zaaktype.isServicenormAvailable()
            }
        }

        if (zaaktypeCmmnConfiguration.zaakTypeUUID != null) {
            LOG.warning {
                "ZaaktypeCmmnConfiguration for zaak type with UUID $zaaktypeUuid is already published. " +
                    "Updating parameters data"
            }
            updateZaakbeeindigGegevens(zaaktypeCmmnConfiguration, zaaktype)
            storeZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration)
            return
        } else {
            zaaktypeCmmnConfiguration.zaakTypeUUID = zaaktypeUuid
        }

        val previousZaaktypeCmmnConfiguration = currentZaaktypeCmmnConfiguration(zaaktype.omschrijving)
        mapPreviousZaaktypeCmmnConfigurationData(zaaktypeCmmnConfiguration, zaaktype, previousZaaktypeCmmnConfiguration)
        storeZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration)

        // ZaaktypeCmmnConfiguration and SmartDocumentsTemplates have circular relations. To solve this, we update
        // already existing ZaaktypeCmmnConfiguration with SmartDocuments settings
        previousZaaktypeCmmnConfiguration.zaakTypeUUID?.let { previousZaaktypeCmmnConfigurationUuid ->
            zaaktypeCmmnConfiguration.zaakTypeUUID?.let { newZaaktypeCmmnConfigurationUuid ->
                mapSmartDocuments(previousZaaktypeCmmnConfigurationUuid, newZaaktypeCmmnConfigurationUuid)
                storeZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration)
            }
        }
    }

    private fun mapPreviousZaaktypeCmmnConfigurationData(
        zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        zaaktype: ZaakType,
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) {
        if (previousZaaktypeCmmnConfiguration.zaakTypeUUID == null) {
            LOG.warning {
                "No previous version of ZaaktypeCmmnConfiguration for zaak type with UUID ${zaaktype.url.extractUuid()} " +
                    "found. Skipping data copy"
            }
            return
        }

        zaaktypeCmmnConfiguration.apply {
            caseDefinitionID = previousZaaktypeCmmnConfiguration.caseDefinitionID
            groepID = previousZaaktypeCmmnConfiguration.groepID
            gebruikersnaamMedewerker = previousZaaktypeCmmnConfiguration.gebruikersnaamMedewerker
            einddatumGeplandWaarschuwing = previousZaaktypeCmmnConfiguration.einddatumGeplandWaarschuwing.takeIf {
                zaaktype.isServicenormAvailable()
            }
            uiterlijkeEinddatumAfdoeningWaarschuwing =
                previousZaaktypeCmmnConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing
            intakeMail = previousZaaktypeCmmnConfiguration.intakeMail
            afrondenMail = previousZaaktypeCmmnConfiguration.afrondenMail
            productaanvraagtype = previousZaaktypeCmmnConfiguration.productaanvraagtype
            domein = previousZaaktypeCmmnConfiguration.domein
            smartDocumentsIngeschakeld = previousZaaktypeCmmnConfiguration.smartDocumentsIngeschakeld
            uiterlijkeEinddatumAfdoeningWaarschuwing =
                previousZaaktypeCmmnConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing
            creatiedatum = ZonedDateTime.now()
        }

        mapHumanTaskParameters(previousZaaktypeCmmnConfiguration, zaaktypeCmmnConfiguration)
        mapUserEventListenerParameters(previousZaaktypeCmmnConfiguration, zaaktypeCmmnConfiguration)
        mapZaakbeeindigGegevens(previousZaaktypeCmmnConfiguration, zaaktypeCmmnConfiguration, zaaktype)
        mapMailtemplateKoppelingen(previousZaaktypeCmmnConfiguration, zaaktypeCmmnConfiguration)
        mapZaakAfzenders(previousZaaktypeCmmnConfiguration, zaaktypeCmmnConfiguration)
        mapBetrokkeneKoppelingen(previousZaaktypeCmmnConfiguration, zaaktypeCmmnConfiguration)
        mapBrpDoelbindingen(previousZaaktypeCmmnConfiguration, zaaktypeCmmnConfiguration)
        mapAutomaticEmailConfirmation(previousZaaktypeCmmnConfiguration, zaaktypeCmmnConfiguration)
    }

    private fun currentZaaktypeCmmnConfiguration(zaaktypeUuid: UUID): ZaaktypeCmmnConfiguration {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaaktypeCmmnConfiguration::class.java)
        val root = query.from(ZaaktypeCmmnConfiguration::class.java)
        query.select(root)
            .where(builder.equal(root.get<Any>(ZAAKTYPE_UUID), zaaktypeUuid))
        query.orderBy(builder.desc(root.get<Any>(CREATIEDATUM)))
        val resultList = entityManager.createQuery(query).setMaxResults(1).resultList
        return resultList.firstOrNull() ?: ZaaktypeCmmnConfiguration()
    }

    private fun currentZaaktypeCmmnConfiguration(zaaktypeDescription: String): ZaaktypeCmmnConfiguration {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaaktypeCmmnConfiguration::class.java)
        val root = query.from(ZaaktypeCmmnConfiguration::class.java)
        query.select(root)
            .where(builder.equal(root.get<Any>(ZAAKTYPE_OMSCHRIJVING), zaaktypeDescription))
        query.orderBy(builder.desc(root.get<Any>(CREATIEDATUM)))
        val resultList = entityManager.createQuery(query).setMaxResults(1).resultList
        return resultList.firstOrNull() ?: ZaaktypeCmmnConfiguration()
    }

    /**
     * Kopieren van de HumanTaskParameters van de oude ZaaktypeCmmnConfiguration naar de nieuw ZaaktypeCmmnConfiguration
     *
     * @param previousZaaktypeCmmnConfiguration bron
     * @param newZaaktypeCmmnConfiguration bestemming
     */
    private fun mapHumanTaskParameters(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = previousZaaktypeCmmnConfiguration.getHumanTaskParametersCollection().map {
        ZaaktypeCmmnHumantaskParameters().apply {
            doorlooptijd = it.doorlooptijd
            actief = it.actief
            setFormulierDefinitieID(it.getFormulierDefinitieID())
            planItemDefinitionID = it.planItemDefinitionID
            groepID = it.groepID
            setReferentieTabellen(it.getReferentieTabellen())
        }
    }.toSet().let(newZaaktypeCmmnConfiguration::setHumanTaskParametersCollection)

    /**
     * Kopieren van de UserEventListenerParameters van de oude ZaaktypeCmmnConfiguration naar de nieuw
     * ZaaktypeCmmnConfiguration
     *
     * @param previousZaaktypeCmmnConfiguration bron
     * @param newZaaktypeCmmnConfiguration bestemming
     */
    private fun mapUserEventListenerParameters(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = previousZaaktypeCmmnConfiguration.getUserEventListenerParametersCollection().map {
        ZaaktypeCmmnUsereventlistenerParameters().apply {
            planItemDefinitionID = it.planItemDefinitionID
            toelichting = it.toelichting
        }
    }.toSet().let(newZaaktypeCmmnConfiguration::setUserEventListenerParametersCollection)

    /**
     * Kopieren van de ZaakbeeindigGegevens van de oude ZaaktypeCmmnConfiguration naar de nieuw ZaaktypeCmmnConfiguration
     *
     * @param previousZaaktypeCmmnConfiguration bron
     * @param newZaaktypeCmmnConfiguration bestemming
     * @param newZaaktype                het nieuwe zaaktype om de resultaten van te lezen
     */
    private fun mapZaakbeeindigGegevens(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }
        newZaaktypeCmmnConfiguration.nietOntvankelijkResultaattype =
            previousZaaktypeCmmnConfiguration.nietOntvankelijkResultaattype?.let {
                mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen)
            }
        val zaakbeeindigParametersCollection = previousZaaktypeCmmnConfiguration.getZaakbeeindigParameters()
            .mapNotNull { zaakbeeindigParameter ->
                zaakbeeindigParameter.resultaattype
                    .let { mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen) }
                    ?.let {
                        ZaaktypeCmmnCompletionParameters().apply {
                            zaakbeeindigReden = zaakbeeindigParameter.zaakbeeindigReden
                            resultaattype = it
                        }
                    }
            }.toMutableSet()
        newZaaktypeCmmnConfiguration.setZaakbeeindigParameters(zaakbeeindigParametersCollection)
    }

    /**
     * Pas de ZaakbeeindigGegevens aan op basis van het gegeven zaaktype
     *
     * @param zaaktypeCmmnConfiguration bron
     * @param newZaaktype                het zaaktype om de resultaten van te lezen
     */
    private fun updateZaakbeeindigGegevens(
        zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }

        zaaktypeCmmnConfiguration.nietOntvankelijkResultaattype?.let {
            mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen)
        }

        zaaktypeCmmnConfiguration.getZaakbeeindigParameters().mapNotNull { zaakbeeindigParameter ->
            zaakbeeindigParameter.resultaattype
                ?.let { mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen) }
                ?.let {
                    ZaaktypeCmmnCompletionParameters().apply {
                        zaakbeeindigReden = zaakbeeindigParameter.zaakbeeindigReden
                        resultaattype = it
                    }
                }
        }
    }

    private fun mapMailtemplateKoppelingen(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = previousZaaktypeCmmnConfiguration.getMailtemplateKoppelingen().map {
        ZaaktypeCmmnMailtemplateParameters().apply {
            mailTemplate = it.mailTemplate
            zaaktypeCmmnConfiguration = newZaaktypeCmmnConfiguration
        }
    }.let(newZaaktypeCmmnConfiguration::setMailtemplateKoppelingen)

    private fun mapVorigResultaattypeOpNieuwResultaattype(
        previousResultaattypeUUID: UUID,
        newResultaattypen: List<ResultaatType>,
    ): UUID? =
        ztcClientService.readResultaattype(previousResultaattypeUUID)
            .let { newResultaattypen.firstOrNull { it.omschrijving == it.omschrijving } }
            ?.url
            ?.extractUuid()

    private fun mapSmartDocuments(
        previousZaakafhandelUUID: UUID,
        newZaaktypeCmmnConfigurationUUID: UUID
    ) {
        val templateMappings = smartDocumentsTemplatesService.getTemplatesMapping(previousZaakafhandelUUID)
        smartDocumentsTemplatesService.storeTemplatesMapping(templateMappings, newZaaktypeCmmnConfigurationUUID)
    }

    private fun mapZaakAfzenders(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = previousZaaktypeCmmnConfiguration.getZaakAfzenders().map {
        ZaaktypeCmmnZaakafzenderParameters().apply {
            defaultMail = it.defaultMail
            mail = it.mail
            replyTo = it.replyTo
            zaaktypeCmmnConfiguration = newZaaktypeCmmnConfiguration
        }
    }.let(newZaaktypeCmmnConfiguration::setZaakAfzenders)

    private fun mapBetrokkeneKoppelingen(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = newZaaktypeCmmnConfiguration.apply {
        zaaktypeCmmnBetrokkeneParameters = ZaaktypeCmmnBetrokkeneParameters().apply {
            zaaktypeCmmnConfiguration = newZaaktypeCmmnConfiguration
            brpKoppelen = previousZaaktypeCmmnConfiguration.zaaktypeCmmnBetrokkeneParameters?.brpKoppelen
            kvkKoppelen = previousZaaktypeCmmnConfiguration.zaaktypeCmmnBetrokkeneParameters?.kvkKoppelen
        }
    }

    private fun mapBrpDoelbindingen(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = newZaaktypeCmmnConfiguration.apply {
        zaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
            zaaktypeCmmnConfiguration = newZaaktypeCmmnConfiguration
            zoekWaarde = previousZaaktypeCmmnConfiguration.zaaktypeCmmnBrpParameters?.zoekWaarde
            raadpleegWaarde = previousZaaktypeCmmnConfiguration.zaaktypeCmmnBrpParameters?.raadpleegWaarde
        }
    }

    private fun mapAutomaticEmailConfirmation(
        previousZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
        newZaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
    ) = newZaaktypeCmmnConfiguration.apply {
        zaaktypeCmmnEmailParameters = ZaaktypeCmmnEmailParameters().apply {
            zaaktypeCmmnConfiguration = newZaaktypeCmmnConfiguration
            enabled = previousZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.enabled ?: false
            templateName = previousZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName
            emailSender = previousZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.emailSender
            emailReply = previousZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.emailReply
        }
    }
}
