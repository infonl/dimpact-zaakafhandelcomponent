/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieobject
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakUUID
import net.atos.zac.mailtemplates.model.MailGegevens
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringInstellingen
import net.atos.zac.signalering.model.SignaleringInstellingenZoekParameters
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringVerzonden
import net.atos.zac.signalering.model.SignaleringVerzondenZoekParameters
import net.atos.zac.signalering.model.SignaleringZoekParameters
import net.atos.zac.util.ValidationUtil
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.shared.RestPageParameters
import nl.info.zac.app.signalering.converter.toRestSignaleringTaakSummary
import nl.info.zac.app.signalering.model.RestSignaleringTaskSummary
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.model.RestZaakOverzicht
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional(SUPPORTS)
@Suppress("TooManyFunctions", "LongParameterList")
@NoArgConstructor
@AllOpen
class SignaleringService @Inject constructor(
    private val entityManager: EntityManager,
    private val drcClientService: DrcClientService,
    private val restInformatieobjectConverter: RestInformatieobjectConverter,
    private val eventingService: EventingService,
    private val flowableTaskService: FlowableTaskService,
    private val mailService: MailService,
    private val signaleringenMailHelper: SignaleringMailHelper,
    private val zrcClientService: ZrcClientService,
    private val restZaakOverzichtConverter: RestZaakOverzichtConverter,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    companion object {
        private val LOG = Logger.getLogger(SignaleringService::class.java.name)
    }

    /**
     * Factory method for constructing Signalering instances.
     *
     * @param signaleringsType the type of the signalering to construct
     * @return the constructed instance (subject and target are still null, type and tijdstip have been set)
     */
    fun signaleringInstance(signaleringsType: SignaleringType.Type): Signalering =
        Signalering().apply {
            tijdstip = ZonedDateTime.now()
            type = signaleringTypeInstance(signaleringsType)
        }

    /**
     * Factory method for constructing SignaleringInstellingen instances.
     *
     * @param signaleringsType the signalering type of the instellingen to construct
     * @param ownerType        the type of the owner of the instellingen to construct
     * @param ownerId          the id of the owner of the instellingen to construct
     * @return the constructed instance (subject and target are still null, type and tijdstip have been set)
     */
    fun signaleringInstellingenInstance(
        signaleringsType: SignaleringType.Type,
        ownerType: SignaleringTarget?,
        ownerId: String?
    ) = SignaleringInstellingen(signaleringTypeInstance(signaleringsType), ownerType, ownerId)

    /**
     * Factory method for constructing SignaleringVerzonden instances.
     *
     * @param signalering the signalering that has been sent
     * @return the constructed instance (all members have been set)
     */
    fun signaleringVerzondenInstance(signalering: Signalering) =
        SignaleringVerzonden().apply {
            tijdstip = ZonedDateTime.now()
            type = signaleringTypeInstance(signalering.type.type)
            targettype = signalering.targettype
            target = signalering.target
            subject = signalering.subject
            detail = signalering.detail
        }

    /**
     * Business logic for deciding if signalling is necessary. Groep-targets will always get signalled but user-targets
     * only when they are not themselves the actor that caused the event (or when the actor is unknown).
     *
     * @param signalering the signalering (should have the target set)
     * @param actor       the actor (a gebruikersnaam) or null if unknown
     * @return true if signalling is necessary
     */
    fun isNecessary(signalering: Signalering, actor: String?): Boolean =
        signalering.targettype != SignaleringTarget.USER || signalering.target != actor

    @Transactional(REQUIRED)
    fun storeSignalering(signalering: Signalering): Signalering {
        ValidationUtil.valideerObject(signalering)

        val signaleringToStore = findSignalering(signalering)?.apply {
            LOG.info("Replacing $this timestamp $tijdstip with ${signalering.tijdstip}")
            tijdstip = signalering.tijdstip
        } ?: signalering

        return entityManager.merge(signaleringToStore).also {
            eventingService.send(ScreenEventType.SIGNALERINGEN.updated(it))
        }
    }

    @Transactional(REQUIRED)
    fun findSignalering(signalering: Signalering): Signalering? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Signalering::class.java)
        val root = query.from(Signalering::class.java)
        val signaleringZoekParameters = SignaleringZoekParameters(signalering)
        return entityManager.createQuery(
            query.select(root).where(getSignaleringWhere(signaleringZoekParameters, builder, root))
        ).resultList.firstOrNull()
    }

    /**
     * Deletes all signaleringen older than the specified number of days regardless of the type.
     * Does not send any screen events for these deletions.
     *
     * @return the number of deleted signaleringen
     */
    @Transactional(REQUIRED)
    fun deleteOldSignaleringen(deleteOlderThanDays: Long): Int {
        LOG.info("Deleting signaleringen older than $deleteOlderThanDays day(s) from the database.")
        val builder = entityManager.criteriaBuilder
        val query = builder.createCriteriaDelete(Signalering::class.java)
        val root = query.from(Signalering::class.java)
        query.where(
            builder.lessThan(
                root.get("tijdstip"),
                ZonedDateTime.now().minusDays(deleteOlderThanDays)
            )
        )
        return entityManager.createQuery(query).executeUpdate().also {
            LOG.info("Deleted $it signaleringen.")
        }
    }

    /**
     * Deletes signaleringen based on the given parameters and send screen event for these deletions
     * grouped by signalering target and type.
     */
    @Transactional(REQUIRED)
    fun deleteSignaleringen(parameters: SignaleringZoekParameters): Int {
        val signaleringen = listSignaleringen(parameters)
        signaleringen.forEach(entityManager::remove)
        // only send separate screen events when signalering target and/or type differ
        signaleringen.associateBy { it.target + ';' + it.type.type }.values
            .forEach { value ->
                eventingService.send(ScreenEventType.SIGNALERINGEN.updated(value))
            }
        return signaleringen.size
    }

    /**
     * Deletes zaak signaleringen for the given zaak for the logged-in user.
     * Does not send any screen events.
     */
    @Transactional(REQUIRED)
    fun deleteSignaleringenForZaak(zaak: Zaak) =
        deleteSignaleringen(
            loggedInUserInstance.getSignaleringZoekParameters()
                .types(SignaleringType.Type.ZAAK_OP_NAAM, SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD)
                .subject(zaak)
        )

    /**
     * Deletes the first 'signalering verzonden' record found based on the given parameters, if any.
     * Returns the number of records deleted (0 or 1).
     */
    @Transactional(REQUIRED)
    fun deleteSignaleringVerzonden(verzonden: SignaleringVerzondenZoekParameters): Int =
        findSignaleringVerzonden(verzonden)?.run {
            entityManager.remove(this)
            1
        } ?: 0

    fun listSignaleringen(parameters: SignaleringZoekParameters): List<Signalering> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Signalering::class.java)
        val root = query.from(Signalering::class.java)
        return entityManager.createQuery(
            query.select(root)
                .where(getSignaleringWhere(parameters, builder, root))
                .orderBy(builder.desc(root.get<Any>("tijdstip")))
        )
            .resultList
    }

    /**
     * Lists all signaleringen in a page
     *
     * @param signaleringSearchParameters parameters of the signaleringen to list
     * @param pageParameters page parameters
     */
    fun listSignaleringen(
        signaleringSearchParameters: SignaleringZoekParameters,
        pageParameters: RestPageParameters
    ): List<Signalering> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            Signalering::class.java
        )
        val root = query.from(Signalering::class.java)
        return entityManager.createQuery(
            query.select(root)
                .where(getSignaleringWhere(signaleringSearchParameters, builder, root))
                .orderBy(builder.desc(root.get<Any>("tijdstip")))
        )
            .setFirstResult(pageParameters.page * pageParameters.rows)
            .setMaxResults(pageParameters.rows)
            .resultList
    }

    fun latestSignaleringOccurrence(): ZonedDateTime? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(ZonedDateTime::class.java)
        val root = query.from(Signalering::class.java)
        query.select(root.get("tijdstip"))
            .where(getSignaleringWhere(loggedInUserInstance.getSignaleringZoekParameters(), builder, root))
            .orderBy(builder.desc(root.get<Any>("tijdstip")))
        val resultList = entityManager.createQuery(query).resultList
        return if (resultList?.isNotEmpty() == true) {
            resultList[0]
        } else {
            null
        }
    }

    fun sendSignalering(signalering: Signalering) {
        ValidationUtil.valideerObject(signalering)
        signaleringenMailHelper.getTargetMail(signalering)?.let { mail ->
            val mailTemplate = signaleringenMailHelper.getMailTemplate(signalering)
            val bronnenBuilder = Bronnen.Builder()
            when (signalering.subjecttype) {
                SignaleringSubject.ZAAK -> {
                    bronnenBuilder.add(getZaak(signalering.subject))
                    if (signalering.type.type === SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD) {
                        bronnenBuilder.add(getDocument(signalering.detail))
                    }
                }
                SignaleringSubject.TAAK -> {
                    val taskInfo = getTask(signalering.subject)
                    bronnenBuilder.add(taskInfo)
                    // also need to retrieve the zaak for the task and add it to the mail sources
                    // so that the relevant zaak-specific template variables in the task emails
                    // can be resolved
                    // note that this is currently only required for the 'ZAAK_OMSCHRIJVING' template field
                    // because the other template fields can be retrieved from the Flowable CMMN case
                    // using the TaakVariabelenService
                    bronnenBuilder.add(getZaak(readZaakUUID(taskInfo).toString()))
                }
                SignaleringSubject.DOCUMENT -> bronnenBuilder.add(getDocument(signalering.subject))
                else -> {}
            }
            mailService.sendMail(
                MailGegevens(
                    mailService.getGemeenteMailAdres(),
                    formatTo(mail),
                    mailTemplate.onderwerp,
                    mailTemplate.body
                ),
                bronnenBuilder.build()
            )
        }
    }

    @Transactional(REQUIRED)
    fun createUpdateOrDeleteInstellingen(instellingen: SignaleringInstellingen): SignaleringInstellingen? {
        ValidationUtil.valideerObject(instellingen)
        if (instellingen.isEmpty) {
            instellingen.id?.let {
                entityManager.remove(entityManager.find(SignaleringInstellingen::class.java, instellingen.id))
            }
            return null
        }
        return entityManager.merge(instellingen)
    }

    fun readInstellingenGroup(type: SignaleringType.Type, target: String) =
        readInstellingen(signaleringInstance(type).apply { setTargetGroup(target) })

    fun readInstellingenUser(type: SignaleringType.Type, target: String) =
        readInstellingen(signaleringInstance(type).apply { setTargetUser(target) })

    fun readInstellingen(signalering: Signalering): SignaleringInstellingen {
        val instellingen = listInstellingen(
            SignaleringInstellingenZoekParameters(signalering)
        )
        if (instellingen.size == 1) {
            return instellingen[0]
        }
        return SignaleringInstellingen(signalering.type, signalering.targettype, signalering.target)
    }

    fun listInstellingen(parameters: SignaleringInstellingenZoekParameters): List<SignaleringInstellingen> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            SignaleringInstellingen::class.java
        )
        val root = query.from(
            SignaleringInstellingen::class.java
        )
        return entityManager.createQuery(
            query.select(root).where(getSignaleringInstellingenWhere(parameters, builder, root))
        )
            .resultList
    }

    fun listInstellingenInclusiefMogelijke(
        parameters: SignaleringInstellingenZoekParameters
    ): List<SignaleringInstellingen> {
        val map = listInstellingen(parameters).associateBy { it.type.type }.toMutableMap()
        SignaleringType.Type.entries
            .filter { it.isTarget(parameters.ownertype) && !map.containsKey(it) }
            .forEach {
                map[it] = signaleringInstellingenInstance(it, parameters.ownertype, parameters.owner)
            }
        return map.values.sortedBy { it.type }
    }

    fun count(): Int = SignaleringType.Type.entries.size

    @Transactional(REQUIRED)
    fun createSignaleringVerzonden(signalering: Signalering): SignaleringVerzonden {
        val signaleringVerzonden = signaleringVerzondenInstance(signalering)
        ValidationUtil.valideerObject(signaleringVerzonden)
        return entityManager.merge(signaleringVerzonden)
    }

    /**
     * Finds the first SignaleringVerzonden record based on the given parameters.
     * Returns null if no matching record is found.
     */
    fun findSignaleringVerzonden(
        parameters: SignaleringVerzondenZoekParameters
    ): SignaleringVerzonden? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(SignaleringVerzonden::class.java)
        val root = query.from(SignaleringVerzonden::class.java)
        val result = entityManager.createQuery(
            query.select(root).where(getSignaleringVerzondenWhere(parameters, builder, root))
        ).resultList
        return result.firstOrNull()
    }

    /**
     * Lists a page of zaken signaleringen for the given signaleringsType
     */
    fun listZakenSignaleringenPage(
        signaleringsType: SignaleringType.Type,
        pageParameters: RestPageParameters
    ): List<RestZaakOverzicht> =
        loggedInUserInstance.getSignaleringZoekParameters()
            .types(signaleringsType)
            .subjecttype(SignaleringSubject.ZAAK)
            .let {
                LOG.fine {
                    "Listing page ${pageParameters.page} (${pageParameters.rows} elements) for zaken signaleringen " +
                        "of type '$signaleringsType' ..."
                }
                listSignaleringen(it, pageParameters)
            }
            .map { zrcClientService.readZaak(UUID.fromString(it.subject)) }
            .map { restZaakOverzichtConverter.convertForDisplay(it) }
            .also {
                LOG.fine {
                    "Successfully listed page ${pageParameters.page} for zaken signaleringen " +
                        "of type '$signaleringsType'."
                }
            }

    /**
     * Counts the number of zaken signaleringen
     *
     * @param signaleringsType signaleringen type to count
     */
    fun countZakenSignaleringen(signaleringsType: SignaleringType.Type) =
        signaleringenCount(
            loggedInUserInstance.getSignaleringZoekParameters()
                .types(signaleringsType)
                .subjecttype(SignaleringSubject.ZAAK)
        )

    /**
     * Lists a page of taken signaleringen for the given signaleringsType
     */
    fun listTakenSignaleringenPage(
        signaleringsType: SignaleringType.Type
    ): List<RestSignaleringTaskSummary> =
        loggedInUserInstance.getSignaleringZoekParameters()
            .types(signaleringsType)
            .subjecttype(SignaleringSubject.TAAK)
            .let {
                LOG.fine { "Listing taken signaleringen of type '$signaleringsType' ..." }
                listSignaleringen(it)
            }
            .map { flowableTaskService.readTask(it.subject) }
            .map { it.toRestSignaleringTaakSummary() }
            .also {
                LOG.fine { "Successfully listed taken signaleringen of type '$signaleringsType'." }
            }

    /**
     * Lists a page of information objects signaleringen for the given signaleringsType
     */
    fun listInformatieobjectenSignaleringen(
        signaleringsType: SignaleringType.Type
    ): List<RestEnkelvoudigInformatieobject> =
        loggedInUserInstance.getSignaleringZoekParameters()
            .types(signaleringsType)
            .subjecttype(SignaleringSubject.DOCUMENT)
            .let {
                LOG.fine { "Listing information objects signaleringen of type '$signaleringsType' ..." }
                listSignaleringen(it)
            }
            .map { drcClientService.readEnkelvoudigInformatieobject(UUID.fromString(it.subject)) }
            .map(restInformatieobjectConverter::convertToREST)
            .also {
                LOG.fine { "Successfully listed information objects signaleringen of type '$signaleringsType'." }
            }

    private fun getDocument(documentUUID: String) =
        drcClientService.readEnkelvoudigInformatieobject(UUID.fromString(documentUUID))

    private fun Instance<LoggedInUser>.getSignaleringZoekParameters() = SignaleringZoekParameters(get())

    private fun getTask(taakID: String) = flowableTaskService.readTask(taakID)

    private fun getZaak(zaakUUID: String): Zaak = zrcClientService.readZaak(UUID.fromString(zaakUUID))

    private fun signaleringenCount(parameters: SignaleringZoekParameters): Long =
        entityManager.criteriaBuilder.let { builder ->
            builder.createQuery(Long::class.java).let { criteriaQuery ->
                criteriaQuery.from(Signalering::class.java).let { root ->
                    entityManager.createQuery(
                        criteriaQuery
                            .select(builder.count(root))
                            .where(getSignaleringWhere(parameters, builder, root))
                    ).resultList.firstOrNull() ?: 0
                }
            }
        }

    private fun signaleringTypeInstance(signaleringsType: SignaleringType.Type): SignaleringType =
        entityManager.find(SignaleringType::class.java, signaleringsType.toString())
}
