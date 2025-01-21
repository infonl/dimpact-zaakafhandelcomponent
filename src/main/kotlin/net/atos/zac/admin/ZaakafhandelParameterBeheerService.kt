/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.ResultaatType
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.admin.model.HumanTaskParameters
import net.atos.zac.admin.model.MailtemplateKoppeling
import net.atos.zac.admin.model.UserEventListenerParameters
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.admin.model.ZaakafhandelParameters.CREATIEDATUM
import net.atos.zac.admin.model.ZaakafhandelParameters.PRODUCTAANVRAAGTYYPE
import net.atos.zac.admin.model.ZaakafhandelParameters.ZAAKTYPE_OMSCHRIJVING
import net.atos.zac.admin.model.ZaakafhandelParameters.ZAAKTYPE_UUID
import net.atos.zac.admin.model.ZaakbeeindigParameter
import net.atos.zac.admin.model.ZaakbeeindigReden
import net.atos.zac.smartdocuments.SmartDocumentsTemplatesService
import net.atos.zac.util.ValidationUtil
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions")
class ZaakafhandelParameterBeheerService @Inject constructor(
    private val entityManager: EntityManager,
    private val ztcClientService: ZtcClientService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val smartDocumentsTemplatesService: SmartDocumentsTemplatesService
) {
    companion object {
        private val LOG = Logger.getLogger(ZaakafhandelParameterBeheerService::class.java.name)
    }

    /**
     * Retrieves the zaakafhandelparameters for a given zaaktype UUID.
     * Note that a zaaktype UUID uniquely identifies a _version_ of a zaaktype, and therefore also
     * a specific version of the corresponding zaakafhandelparameters.
     */
    fun readZaakafhandelParameters(zaaktypeUUID: UUID): ZaakafhandelParameters {
        ztcClientService.resetCacheTimeToNow()
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaakafhandelParameters::class.java)
        val root = query.from(ZaakafhandelParameters::class.java)
        query.select(root).where(builder.equal(root.get<Any>(ZaakafhandelParameters.ZAAKTYPE_UUID), zaaktypeUUID))
        val resultList = entityManager.createQuery(query).setMaxResults(1).resultList
        return resultList.firstOrNull() ?: ZaakafhandelParameters().apply {
            zaakTypeUUID = zaaktypeUUID
        }
    }

    fun listZaakafhandelParameters(): List<ZaakafhandelParameters> {
        val query = entityManager.criteriaBuilder.createQuery(ZaakafhandelParameters::class.java)
        val root = query.from(ZaakafhandelParameters::class.java)
        query.select(root).orderBy(entityManager.criteriaBuilder.desc(root.get<Any>("id")))
        return entityManager.createQuery(query).resultList
    }

    fun storeZaakafhandelParameters(zaakafhandelParameters: ZaakafhandelParameters): ZaakafhandelParameters {
        zaakafhandelParameterService.clearListCache()
        ValidationUtil.valideerObject(zaakafhandelParameters)
        zaakafhandelParameters.apply {
            humanTaskParametersCollection.forEach { ValidationUtil.valideerObject(it) }
            userEventListenerParametersCollection.forEach { ValidationUtil.valideerObject(it) }
            mailtemplateKoppelingen.forEach { ValidationUtil.valideerObject(it) }
            zaakAfzenders.forEach { ValidationUtil.valideerObject(it) }
            creatiedatum = zaakafhandelParameters.creatiedatum ?: ZonedDateTime.now()
        }

        return if (zaakafhandelParameters.id == null) {
            entityManager.persist(zaakafhandelParameters)
            zaakafhandelParameters
        } else {
            return entityManager.merge(zaakafhandelParameters)
        }
    }

    /**
     * Finds the active zaakafhandelparameters for the specified productaanvraag type.
     * If multiple active zaakafhandelparameters are found, this indicates an
     * error in the configuration of zaakafhandelparameters.
     * There should be at most only one active zaakafhandelparameters for each productaanvraagtype.
     *
     * @return the list of found zaakafhandelparameters
     */
    fun findActiveZaakafhandelparametersByProductaanvraagtype(
        productaanvraagType: String
    ): List<ZaakafhandelParameters> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaakafhandelParameters::class.java)
        val root = query.from(ZaakafhandelParameters::class.java)
        val subquery = query.subquery(Date::class.java)
        val subqueryRoot = subquery.from(ZaakafhandelParameters::class.java)
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
    fun upsertZaakafhandelParameters(zaaktypeUri: URI) {
        zaakafhandelParameterService.clearListCache()
        zaakafhandelParameterService.clearManagedCache()
        ztcClientService.clearZaaktypeCache()

        val zaaktype = ztcClientService.readZaaktype(zaaktypeUri)
        val zaktypeUuid = zaaktype.url.extractUuid()
        if (zaaktype.concept) {
            LOG.warning { "Zaak type with UUID $zaktypeUuid is still a concept. Ignoring" }
            return
        }

        val zaakafhandelParameters = currentZaakafhandelParameters(zaktypeUuid)

        zaakafhandelParameters.apply {
            zaaktypeOmschrijving = zaaktype.omschrijving
            einddatumGeplandWaarschuwing = zaaktype.servicenorm?.let {
                zaakafhandelParameters.einddatumGeplandWaarschuwing
            }
        }

        if (zaakafhandelParameters.zaakTypeUUID != null) {
            LOG.warning {
                "ZaakafhandelParameters for zaak type with UUID $zaktypeUuid is already published. Updating information"
            }
            updateZaakbeeindigGegevens(zaakafhandelParameters, zaaktype)
            storeZaakafhandelParameters(zaakafhandelParameters)
            return
        }

        val previousZaakafhandelparameters = currentZaakafhandelParameters(zaaktype.omschrijving)

        // Check if this is a "new version" of a `zaaktype`
        if (previousZaakafhandelparameters.zaakTypeUUID == null) {
            LOG.warning {
                "No previous version of ZaakafhandelParameters for zaak type with UUID $zaktypeUuid found. " +
                        "Skipping data copy"
            }
            return
        }

        mapPreviousZaakafhandelparametersData(zaakafhandelParameters, zaaktype, previousZaakafhandelparameters)

        // We need to store the zaakafhandel parameters before mapping smart documents
        // as we are reliant on fetching data which is mapped in the methods above and should be stored
        // to prevent issues with the entityManager
        storeZaakafhandelParameters(zaakafhandelParameters)

        mapSmartDocuments(previousZaakafhandelparameters.zaakTypeUUID, zaakafhandelParameters.zaakTypeUUID)
        storeZaakafhandelParameters(zaakafhandelParameters)
    }

    private fun mapPreviousZaakafhandelparametersData(
        zaakafhandelParameters: ZaakafhandelParameters,
        zaaktype: ZaakType,
        previousZaakafhandelparameters: ZaakafhandelParameters
    ) {
        zaakafhandelParameters.apply {
            zaakTypeUUID = zaaktype.url.extractUuid()
            caseDefinitionID = previousZaakafhandelparameters.caseDefinitionID
            groepID = previousZaakafhandelparameters.groepID
            gebruikersnaamMedewerker = previousZaakafhandelparameters.gebruikersnaamMedewerker
            einddatumGeplandWaarschuwing = zaaktype.servicenorm?.let {
                previousZaakafhandelparameters.einddatumGeplandWaarschuwing
            }
            uiterlijkeEinddatumAfdoeningWaarschuwing =
                previousZaakafhandelparameters.uiterlijkeEinddatumAfdoeningWaarschuwing
            intakeMail = previousZaakafhandelparameters.intakeMail
            afrondenMail = previousZaakafhandelparameters.afrondenMail
            productaanvraagtype = previousZaakafhandelparameters.productaanvraagtype
            domein = previousZaakafhandelparameters.domein
            isSmartDocumentsIngeschakeld = previousZaakafhandelparameters.isSmartDocumentsIngeschakeld
            uiterlijkeEinddatumAfdoeningWaarschuwing =
                previousZaakafhandelparameters.uiterlijkeEinddatumAfdoeningWaarschuwing
            intakeMail = previousZaakafhandelparameters.intakeMail
            afrondenMail = previousZaakafhandelparameters.afrondenMail
            productaanvraagtype = previousZaakafhandelparameters.productaanvraagtype
            domein = previousZaakafhandelparameters.domein
            isSmartDocumentsIngeschakeld = previousZaakafhandelparameters.isSmartDocumentsIngeschakeld
            creatiedatum = ZonedDateTime.now()
        }

        mapHumanTaskParameters(previousZaakafhandelparameters, zaakafhandelParameters)
        mapUserEventListenerParameters(previousZaakafhandelparameters, zaakafhandelParameters)
        mapZaakbeeindigGegevens(previousZaakafhandelparameters, zaakafhandelParameters, zaaktype)
        mapMailtemplateKoppelingen(previousZaakafhandelparameters, zaakafhandelParameters)
    }

    private fun currentZaakafhandelParameters(zaaktypeUuid: UUID): ZaakafhandelParameters {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaakafhandelParameters::class.java)
        val root = query.from(ZaakafhandelParameters::class.java)
        query.select(root)
            .where(builder.equal(root.get<Any>(ZAAKTYPE_UUID), zaaktypeUuid))
        query.orderBy(builder.desc(root.get<Any>(CREATIEDATUM)))
        val resultList = entityManager.createQuery(query).setMaxResults(1).resultList
        return resultList.firstOrNull() ?: ZaakafhandelParameters()
    }

    private fun currentZaakafhandelParameters(zaaktypeDescription: String): ZaakafhandelParameters {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaakafhandelParameters::class.java)
        val root = query.from(ZaakafhandelParameters::class.java)
        query.select(root)
            .where(builder.equal(root.get<Any>(ZAAKTYPE_OMSCHRIJVING), zaaktypeDescription))
        query.orderBy(builder.desc(root.get<Any>(CREATIEDATUM)))
        val resultList = entityManager.createQuery(query).setMaxResults(1).resultList
        return resultList.firstOrNull() ?: ZaakafhandelParameters()
    }

    /**
     * Kopieren van de HumanTaskParameters van de oude ZaakafhandelParameters naar de nieuw ZaakafhandelParameters
     *
     * @param previousZaakafhandelParameters bron
     * @param newZaakafhandelParameters bestemming
     */
    private fun mapHumanTaskParameters(
        previousZaakafhandelParameters: ZaakafhandelParameters,
        newZaakafhandelParameters: ZaakafhandelParameters
    ) = previousZaakafhandelParameters.humanTaskParametersCollection.map {
        HumanTaskParameters().apply {
            doorlooptijd = it.doorlooptijd
            isActief = it.isActief
            formulierDefinitieID = it.formulierDefinitieID
            planItemDefinitionID = it.planItemDefinitionID
            groepID = it.groepID
            referentieTabellen = it.referentieTabellen
            formulierDefinitieID = it.formulierDefinitieID
        }
    }.toSet().let(newZaakafhandelParameters::setHumanTaskParametersCollection)

    /**
     * Kopieren van de UserEventListenerParameters van de oude ZaakafhandelParameters naar de nieuw
     * ZaakafhandelParameters
     *
     * @param previousZaakafhandelParameters bron
     * @param newZaakafhandelParameters bestemming
     */
    private fun mapUserEventListenerParameters(
        previousZaakafhandelParameters: ZaakafhandelParameters,
        newZaakafhandelParameters: ZaakafhandelParameters
    ) = previousZaakafhandelParameters.userEventListenerParametersCollection.map {
        UserEventListenerParameters().apply {
            planItemDefinitionID = it.planItemDefinitionID
            toelichting = it.toelichting
        }
    }.toSet().let(newZaakafhandelParameters::setUserEventListenerParametersCollection)

    /**
     * Kopieren van de ZaakbeeindigGegevens van de oude ZaakafhandelParameters naar de nieuw ZaakafhandelParameters
     *
     * @param previousZaakafhandelParameters bron
     * @param newZaakafhandelParameters bestemming
     * @param newZaaktype                het nieuwe zaaktype om de resultaten van te lezen
     */
    private fun mapZaakbeeindigGegevens(
        previousZaakafhandelParameters: ZaakafhandelParameters,
        newZaakafhandelParameters: ZaakafhandelParameters,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }
        newZaakafhandelParameters.nietOntvankelijkResultaattype =
            previousZaakafhandelParameters.nietOntvankelijkResultaattype?.let {
                mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen)
            }
        val zaakbeeindigParametersCollection = previousZaakafhandelParameters.zaakbeeindigParameters.mapNotNull {
                zaakbeeindigParameter ->
            zaakbeeindigParameter.resultaattype
                ?.let { mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen) }
                ?.let {
                    ZaakbeeindigParameter().apply {
                        zaakbeeindigReden = zaakbeeindigParameter.zaakbeeindigReden
                        resultaattype = it
                    }
                }
        }.toMutableSet()
        newZaakafhandelParameters.setZaakbeeindigParameters(zaakbeeindigParametersCollection)
    }

    /**
     * Pas de ZaakbeeindigGegevens aan op basis van het gegeven zaaktype
     *
     * @param zaakafhandelParameters bron
     * @param newZaaktype                het zaaktype om de resultaten van te lezen
     */
    private fun updateZaakbeeindigGegevens(
        zaakafhandelParameters: ZaakafhandelParameters,
        newZaaktype: ZaakType
    ) {
        val newResultaattypen = newZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }

        zaakafhandelParameters.nietOntvankelijkResultaattype?.let {
            mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen)
        }

        zaakafhandelParameters.zaakbeeindigParameters.mapNotNull { zaakbeeindigParameter ->
            zaakbeeindigParameter.resultaattype
                ?.let { mapVorigResultaattypeOpNieuwResultaattype(it, newResultaattypen) }
                ?.let {
                    ZaakbeeindigParameter().apply {
                        zaakbeeindigReden = zaakbeeindigParameter.zaakbeeindigReden
                        resultaattype = it
                    }
                }
        }
    }

    private fun mapMailtemplateKoppelingen(
        previousZaakafhandelParameters: ZaakafhandelParameters,
        newZaakafhandelParameters: ZaakafhandelParameters
    ) = previousZaakafhandelParameters.mailtemplateKoppelingen.map {
        MailtemplateKoppeling().apply {
            mailTemplate = it.mailTemplate
            zaakafhandelParameters = newZaakafhandelParameters
        }
    }.toSet().let(newZaakafhandelParameters::setMailtemplateKoppelingen)

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
        newZaakafhandelParametersUUID: UUID
    ) {
        val templateMappings = smartDocumentsTemplatesService.getTemplatesMapping(previousZaakafhandelUUID)
        smartDocumentsTemplatesService.storeTemplatesMapping(templateMappings, newZaakafhandelParametersUUID)
    }
}
