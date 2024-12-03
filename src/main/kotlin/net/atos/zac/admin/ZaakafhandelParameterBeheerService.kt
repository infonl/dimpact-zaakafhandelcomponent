/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
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
import net.atos.zac.admin.model.ZaakbeeindigParameter
import net.atos.zac.admin.model.ZaakbeeindigReden
import net.atos.zac.util.ValidationUtil
import net.atos.zac.util.extractUuid
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.net.URI
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions")
class ZaakafhandelParameterBeheerService @Inject constructor(
    private val entityManager: EntityManager,
    private val ztcClientService: ZtcClientService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService
) {
    /**
     * Retrieves the most recent zaakafhandelparameters for a given zaaktype UUID or creates a new one
     * if none exists yet.
     * Note that a zaaktype UUID uniquely identifies a _version_ of a zaaktype, and therefore also
     * a specific version of the corresponding zaakafhandelparameters.
     */
    fun readZaakafhandelParameters(zaaktypeUUID: UUID): ZaakafhandelParameters {
        ztcClientService.resetCacheTimeToNow()
        return listZaakafhandelParametersForZaaktypeUuid(zaaktypeUUID).let { resultList ->
            if (resultList.isNotEmpty()) {
                // by definition, we only ever can have at most one zaakafhandelparameters for a zaaktype UUID
                resultList.first()
            } else {
                ZaakafhandelParameters().apply {
                    zaakTypeUUID = zaaktypeUUID
                }
            }
        }
    }

    fun listZaakafhandelParameters(): List<ZaakafhandelParameters> {
        val query = entityManager.criteriaBuilder.createQuery(ZaakafhandelParameters::class.java)
        val root = query.from(ZaakafhandelParameters::class.java)
        query.select(root).orderBy(entityManager.criteriaBuilder.desc(root.get<Any>("id")))
        return entityManager.createQuery(query).resultList
    }

    fun createZaakafhandelParameters(zaakafhandelParameters: ZaakafhandelParameters): ZaakafhandelParameters {
        zaakafhandelParameterService.clearListCache()
        ValidationUtil.valideerObject(zaakafhandelParameters)
        zaakafhandelParameters.apply {
            humanTaskParametersCollection.forEach { ValidationUtil.valideerObject(it) }
            userEventListenerParametersCollection.forEach { ValidationUtil.valideerObject(it) }
            mailtemplateKoppelingen.forEach { ValidationUtil.valideerObject(it) }
            creatiedatum = ZonedDateTime.now()
        }
        entityManager.persist(zaakafhandelParameters)
        return zaakafhandelParameters
    }

    fun updateZaakafhandelParameters(zaakafhandelParameters: ZaakafhandelParameters): ZaakafhandelParameters {
        ValidationUtil.valideerObject(zaakafhandelParameters)
        zaakafhandelParameters.humanTaskParametersCollection.forEach(ValidationUtil::valideerObject)
        zaakafhandelParameters.creatiedatum =
            entityManager.find(ZaakafhandelParameters::class.java, zaakafhandelParameters.id).creatiedatum
        return entityManager.merge(zaakafhandelParameters)
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

    /**
     * Zaaktype has been changed. If it is not a concept, and we do not already have zaakafhandelparameters for this
     * exact zaaktype (identified by the uuid) then this indicates that a 'new version' of the zaaktype
     * was created. Therefore, we create new zaakafhandelparameters with values which are copied from the previous
     * zaakafhandelparameters 'version' for this zaaktype.
     * Note that when a zaaktype is republished in the ZGW APIs technically a new zaaktype is created with its own
     * unique zaaktype UUID. A new zaaktype relates to the previous 'version' of this zaaktype only by zaaktype description.
     *
     * @param zaaktypeUri uri of the new zaaktype
     */
    fun createNewZaakafhandelParametersOnZaakTypeChange(zaaktypeUri: URI) {
        // if we already have a zaakafhandelparameters for this zaaktype UUID, so do not attempt to create a new one
        if (listZaakafhandelParametersForZaaktypeUuid(zaaktypeUri.extractUuid()).isNotEmpty()) return

        ztcClientService.readZaaktype(zaaktypeUri).takeIf { !it.concept }?.let { zaaktype ->
            zaakafhandelParameterService.clearListCache()
            ztcClientService.clearZaaktypeCache()
            val currentZaakafhandelParameters = readMostRecentZaakafhandelParametersForZaaktypeDescription(
                zaaktype.omschrijving
            )
            val newZaakafhandelParameters = ZaakafhandelParameters().apply {
                zaakTypeUUID = zaaktype.url.extractUuid()
                zaaktypeOmschrijving = zaaktype.omschrijving
                caseDefinitionID = currentZaakafhandelParameters.caseDefinitionID
                groepID = currentZaakafhandelParameters.groepID
                gebruikersnaamMedewerker = currentZaakafhandelParameters.gebruikersnaamMedewerker
                einddatumGeplandWaarschuwing = zaaktype.servicenorm?.let { currentZaakafhandelParameters.einddatumGeplandWaarschuwing }
                uiterlijkeEinddatumAfdoeningWaarschuwing = currentZaakafhandelParameters.uiterlijkeEinddatumAfdoeningWaarschuwing
                intakeMail = currentZaakafhandelParameters.intakeMail
                afrondenMail = currentZaakafhandelParameters.afrondenMail
                productaanvraagtype = currentZaakafhandelParameters.productaanvraagtype
                domein = currentZaakafhandelParameters.domein
            }
            mapHumanTaskParameters(currentZaakafhandelParameters, newZaakafhandelParameters)
            mapUserEventListenerParameters(currentZaakafhandelParameters, newZaakafhandelParameters)
            mapZaakbeeindigGegevens(currentZaakafhandelParameters, newZaakafhandelParameters, zaaktype)
            mapMailtemplateKoppelingen(currentZaakafhandelParameters, newZaakafhandelParameters)
            createZaakafhandelParameters(newZaakafhandelParameters)
        }
    }

    private fun readMostRecentZaakafhandelParametersForZaaktypeDescription(zaaktypeDescription: String): ZaakafhandelParameters {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaakafhandelParameters::class.java)
        val root = query.from(ZaakafhandelParameters::class.java)
        query.select(root)
            .where(builder.equal(root.get<Any>(ZAAKTYPE_OMSCHRIJVING), zaaktypeDescription))
        query.orderBy(builder.desc(root.get<Any>(CREATIEDATUM)))
        val resultList = entityManager.createQuery(query).setMaxResults(1).resultList
        return if (resultList.isNotEmpty()) {
            resultList.first()
        } else {
            ZaakafhandelParameters()
        }
    }

    /**
     * Kopieren van de HumanTaskParameters van de oude ZaakafhandelParameters naar de nieuw ZaakafhandelParameters
     *
     * @param vorigeZaakafhandelparameters bron
     * @param nieuweZaakafhandelParameters bestemming
     */
    private fun mapHumanTaskParameters(
        vorigeZaakafhandelparameters: ZaakafhandelParameters,
        nieuweZaakafhandelParameters: ZaakafhandelParameters
    ) = vorigeZaakafhandelparameters.humanTaskParametersCollection.map {
        HumanTaskParameters().apply {
            doorlooptijd = it.doorlooptijd
            isActief = it.isActief
            formulierDefinitieID = it.formulierDefinitieID
            planItemDefinitionID = it.planItemDefinitionID
            groepID = it.groepID
            referentieTabellen = it.referentieTabellen
            formulierDefinitieID = it.formulierDefinitieID
        }
    }.toSet().let(nieuweZaakafhandelParameters::setHumanTaskParametersCollection)

    /**
     * Kopieren van de UserEventListenerParameters van de oude ZaakafhandelParameters naar de nieuw ZaakafhandelParameters
     *
     * @param vorigeZaakafhandelparameters bron
     * @param nieuweZaakafhandelParameters bestemming
     */
    private fun mapUserEventListenerParameters(
        vorigeZaakafhandelparameters: ZaakafhandelParameters,
        nieuweZaakafhandelParameters: ZaakafhandelParameters
    ) = vorigeZaakafhandelparameters.userEventListenerParametersCollection.map {
        UserEventListenerParameters().apply {
            planItemDefinitionID = it.planItemDefinitionID
            toelichting = it.toelichting
        }
    }.toSet().let(nieuweZaakafhandelParameters::setUserEventListenerParametersCollection)

    /**
     * Kopieren van de ZaakbeeindigGegevens van de oude ZaakafhandelParameters naar de nieuw ZaakafhandelParameters
     *
     * @param vorigeZaakafhandelparameters bron
     * @param nieuweZaakafhandelParameters bestemming
     * @param nieuwZaaktype                het nieuwe zaaktype om de resultaten van te lezen
     */
    private fun mapZaakbeeindigGegevens(
        vorigeZaakafhandelparameters: ZaakafhandelParameters,
        nieuweZaakafhandelParameters: ZaakafhandelParameters,
        nieuwZaaktype: ZaakType
    ) {
        val nieuweResultaattypen = nieuwZaaktype.resultaattypen.map { ztcClientService.readResultaattype(it) }
        nieuweZaakafhandelParameters.nietOntvankelijkResultaattype =
            vorigeZaakafhandelparameters.nietOntvankelijkResultaattype?.let {
                mapVorigResultaattypeOpNieuwResultaattype(nieuweResultaattypen, it)
            }
        val zaakbeeindigParametersCollection = vorigeZaakafhandelparameters.zaakbeeindigParameters.mapNotNull {
                zaakbeeindigParameter ->
            zaakbeeindigParameter.resultaattype
                ?.let { mapVorigResultaattypeOpNieuwResultaattype(nieuweResultaattypen, it) }
                ?.let {
                    ZaakbeeindigParameter().apply {
                        zaakbeeindigReden = zaakbeeindigParameter.zaakbeeindigReden
                        resultaattype = it
                    }
                }
        }.toMutableSet()
        nieuweZaakafhandelParameters.setZaakbeeindigParameters(zaakbeeindigParametersCollection)
    }

    private fun mapMailtemplateKoppelingen(
        vorigeZaakafhandelparameters: ZaakafhandelParameters,
        nieuweZaakafhandelParameters: ZaakafhandelParameters
    ) = vorigeZaakafhandelparameters.mailtemplateKoppelingen.map {
        MailtemplateKoppeling().apply {
            mailTemplate = it.mailTemplate
            zaakafhandelParameters = nieuweZaakafhandelParameters
        }
    }.toSet().let(nieuweZaakafhandelParameters::setMailtemplateKoppelingen)

    private fun mapVorigResultaattypeOpNieuwResultaattype(
        nieuweResultaattypen: List<ResultaatType>,
        vorigResultaattypeUUID: UUID
    ): UUID? =
        ztcClientService.readResultaattype(vorigResultaattypeUUID).let { resultaattype ->
            nieuweResultaattypen
                .firstOrNull { it.omschrijving == resultaattype.omschrijving }?.url?.extractUuid()
        }

    private fun listZaakafhandelParametersForZaaktypeUuid(zaaktypeUUID: UUID): List<ZaakafhandelParameters> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaakafhandelParameters::class.java)
        val root = query.from(ZaakafhandelParameters::class.java)
        query.select(root).where(builder.equal(root.get<Any>(ZaakafhandelParameters.ZAAKTYPE_UUID), zaaktypeUUID))
        return entityManager.createQuery(query).resultList
    }
}
