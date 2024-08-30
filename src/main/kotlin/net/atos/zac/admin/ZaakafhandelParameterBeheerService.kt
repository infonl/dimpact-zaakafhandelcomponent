/*
 * SPDX-FileCopyrightText: 2024 Lifely
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
import net.atos.zac.admin.model.ZaakafhandelParameters.FIND_ACTIVE_ZAAKAFHANDELPARAMETERS_FOR_PRODUCTAANVRAAGTYPE_QUERY
import net.atos.zac.admin.model.ZaakafhandelParameters.PRODUCTAANVRAAGTYPE_DATABASE_NAME
import net.atos.zac.admin.model.ZaakbeeindigParameter
import net.atos.zac.admin.model.ZaakbeeindigReden
import net.atos.zac.util.UriUtil
import net.atos.zac.util.ValidationUtil
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.net.URI
import java.time.ZonedDateTime
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
    private val zaakafhandelParameterService: ZaakafhandelParameterService
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
        val resultList = entityManager.createQuery(query).resultList
        return if (resultList.isNotEmpty()) {
            resultList.first()
        } else {
            ZaakafhandelParameters().apply {
                zaakTypeUUID = zaaktypeUUID
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
        zaakafhandelParameters.humanTaskParametersCollection.forEach { ValidationUtil.valideerObject(it) }
        zaakafhandelParameters.creatiedatum =
            entityManager.find(ZaakafhandelParameters::class.java, zaakafhandelParameters.id).creatiedatum
        return entityManager.merge(zaakafhandelParameters)
    }

    /**
     * Finds the zaaktype UUID for the active zaakafhandelparameters with the specified productaanvraag type.
     * If multiple active zaakafhandelparameters are found, the first result with the most recent creation date is returned.
     *
     * @return the zaaktype UUID; null if no results were found
     */
    fun findActiveZaaktypeUuidByProductaanvraagType(productaanvraagType: String): UUID? {
        val query = entityManager.createNamedQuery(
            FIND_ACTIVE_ZAAKAFHANDELPARAMETERS_FOR_PRODUCTAANVRAAGTYPE_QUERY,
            ZaakafhandelParameters::class.java
        ).setParameter(PRODUCTAANVRAAGTYPE_DATABASE_NAME, productaanvraagType)
        (query.resultList as List<ZaakafhandelParameters>).let {
            if (it.isEmpty()) {
                return null
            }
            if (it.size > 1) {
                LOG.warning(
                    "Multiple active zaakafhandelparameters have been found for productaanvraagtype: '$productaanvraagType'. " +
                        "This indicates that the zaakafhandelparameters are not configured correctly. " +
                        "There should be at most only one active zaakafhandelparameters for each productaanvraagtype. " +
                        "Returning the first result with the most recent creation date, with zaaktypeomschrijving: " +
                        "'${it.first().zaaktypeOmschrijving}' and zaaktype UUID: " +
                        "'${it.first().zaakTypeUUID}'."
                )
            }
            return it.first().zaakTypeUUID
        }
    }

    fun listZaakbeeindigRedenen(): List<ZaakbeeindigReden> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaakbeeindigReden::class.java)
        val root = query.from(ZaakbeeindigReden::class.java)
        query.orderBy(builder.asc(root.get<Any>("naam")))
        return entityManager.createQuery(query).resultList
    }

    /**
     * Zaaktype is aangepast, indien geen concept, dan de zaakafhandelparameters van de vorige versie zoveel mogelijk overnemen
     *
     * @param zaaktypeUri uri van het nieuwe zaaktype
     */
    fun zaaktypeAangepast(zaaktypeUri: URI) {
        zaakafhandelParameterService.clearListCache()
        ztcClientService.clearZaaktypeCache()
        val zaaktype = ztcClientService.readZaaktype(zaaktypeUri)
        if (!zaaktype.concept) {
            val omschrijving = zaaktype.omschrijving
            val vorigeZaakafhandelparameters = readRecentsteZaakafhandelParameters(omschrijving)
            val nieuweZaakafhandelParameters = ZaakafhandelParameters().apply {
                zaakTypeUUID = UriUtil.uuidFromURI(zaaktype.url)
                zaaktypeOmschrijving = zaaktype.omschrijving
                caseDefinitionID = vorigeZaakafhandelparameters.caseDefinitionID
                groepID = vorigeZaakafhandelparameters.groepID
                gebruikersnaamMedewerker = vorigeZaakafhandelparameters.gebruikersnaamMedewerker
                einddatumGeplandWaarschuwing = zaaktype.servicenorm?.let {
                    vorigeZaakafhandelparameters.einddatumGeplandWaarschuwing
                }
                uiterlijkeEinddatumAfdoeningWaarschuwing = vorigeZaakafhandelparameters.uiterlijkeEinddatumAfdoeningWaarschuwing
                intakeMail = vorigeZaakafhandelparameters.intakeMail
                afrondenMail = vorigeZaakafhandelparameters.afrondenMail
                productaanvraagtype = vorigeZaakafhandelparameters.productaanvraagtype
                domein = vorigeZaakafhandelparameters.domein
            }
            mapHumanTaskParameters(vorigeZaakafhandelparameters, nieuweZaakafhandelParameters)
            mapUserEventListenerParameters(vorigeZaakafhandelparameters, nieuweZaakafhandelParameters)
            mapZaakbeeindigGegevens(vorigeZaakafhandelparameters, nieuweZaakafhandelParameters, zaaktype)
            mapMailtemplateKoppelingen(vorigeZaakafhandelparameters, nieuweZaakafhandelParameters)
            createZaakafhandelParameters(nieuweZaakafhandelParameters)
        }
    }

    private fun readRecentsteZaakafhandelParameters(zaaktypeOmschrijving: String): ZaakafhandelParameters {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZaakafhandelParameters::class.java)
        val root = query.from(ZaakafhandelParameters::class.java)
        query.select(root)
            .where(builder.equal(root.get<Any>(ZaakafhandelParameters.ZAAKTYPE_OMSCHRIJVING), zaaktypeOmschrijving))
        query.orderBy(builder.desc(root.get<Any>(ZaakafhandelParameters.CREATIEDATUM)))
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
                .firstOrNull { it.omschrijving == resultaattype.omschrijving }
                ?.let { UriUtil.uuidFromURI(it.url) }
        }
}
