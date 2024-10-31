/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.app.zaak.converter.RestZaakOverzichtConverter
import net.atos.zac.app.zaak.model.RestZaakOverzicht
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakUUID
import net.atos.zac.mail.MailService
import net.atos.zac.mail.model.Bronnen
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
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.Optional
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
    private val eventingService: EventingService,
    private val flowableTaskService: FlowableTaskService,
    private val mailService: MailService,
    private val signaleringenMailHelper: SignaleringMailHelper,
    private val zrcClientService: ZrcClientService,
    private val restZaakOverzichtConverter: RestZaakOverzichtConverter
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
    fun createSignalering(signalering: Signalering): Signalering {
        ValidationUtil.valideerObject(signalering)
        val created = entityManager.merge(signalering)
        eventingService.send(ScreenEventType.SIGNALERINGEN.updated(created))
        return created
    }

    /**
     * Deletes signaleringen based on the given parameters
     * and sends a screen event for each deletion.
     */
    @Transactional(REQUIRED)
    fun deleteSignaleringen(parameters: SignaleringZoekParameters) {
        val removed: MutableMap<String, Signalering> = HashMap()
        listSignaleringen(parameters)
            .forEach {
                removed[it.target + ';' + it.type.type] = it
                entityManager.remove(it)
            }
        removed.values
            .forEach {
                eventingService.send(ScreenEventType.SIGNALERINGEN.updated(it))
            }
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

    fun listSignaleringen(parameters: SignaleringZoekParameters): List<Signalering> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            Signalering::class.java
        )
        val root = query.from(Signalering::class.java)
        return entityManager.createQuery(
            query.select(root)
                .where(getSignaleringWhere(parameters, builder, root))
                .orderBy(builder.desc(root.get<Any>("tijdstip")))
        )
            .resultList
    }

    fun listSignaleringen(parameters: SignaleringZoekParameters, pageNumber: Int, pageSize: Int): List<Signalering> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            Signalering::class.java
        )
        val root = query.from(Signalering::class.java)
        return entityManager.createQuery(
            query.select(root)
                .where(getSignaleringWhere(parameters, builder, root))
                .orderBy(builder.desc(root.get<Any>("tijdstip")))
        )
            .setFirstResult(pageNumber * pageSize)
            .setMaxResults(pageSize)
            .resultList
    }

    fun latestSignalering(parameters: SignaleringZoekParameters): ZonedDateTime? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            ZonedDateTime::class.java
        )
        val root = query.from(Signalering::class.java)

        query.select(root.get("tijdstip"))
            .where(getSignaleringWhere(parameters, builder, root))
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
        signaleringenMailHelper.getTargetMail(signalering)?.let {
            val from = mailService.gemeenteMailAdres
            val to = formatTo(it)
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
                MailGegevens(from, to, mailTemplate.onderwerp, mailTemplate.body),
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

    fun readInstellingenGroup(type: SignaleringType.Type, target: String?): SignaleringInstellingen {
        signaleringInstance(type).apply {
            setTargetGroup(target)
        }.let {
            return readInstellingen(it)
        }
    }

    fun readInstellingenUser(type: SignaleringType.Type, target: String?): SignaleringInstellingen {
        signaleringInstance(type).apply {
            setTargetUser(target)
        }.let {
            return readInstellingen(it)
        }
    }

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

    @Transactional(REQUIRED)
    fun deleteSignaleringVerzonden(verzonden: SignaleringVerzondenZoekParameters) {
        findSignaleringVerzonden(verzonden).ifPresent { entityManager.remove(it) }
    }

    fun findSignaleringVerzonden(
        parameters: SignaleringVerzondenZoekParameters
    ): Optional<SignaleringVerzonden> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(SignaleringVerzonden::class.java)
        val root = query.from(SignaleringVerzonden::class.java)
        val result = entityManager.createQuery(
            query.select(root).where(getSignaleringVerzondenWhere(parameters, builder, root))
        ).resultList
        return if (result.isEmpty()) { Optional.empty() } else { Optional.of(result[0]) }
    }

    /**
     * Lists zaken signaleringen for the given signaleringsType and sends a screen event with the result.
     * This can be a long-running operation.
     */
    @WithSpan
    fun listZakenSignaleringen(
        user: LoggedInUser,
        @SpanAttribute("signaleringsType") signaleringsType: SignaleringType.Type,
        screenEventResourceId: String
    ) {
        LOG.fine {
            "Started to list zaken signaleringen for type '$signaleringsType' " +
                "with screen event resource ID: '$screenEventResourceId'."
        }

        val zakenSignaleringen = listZakenSignaleringen(user, signaleringsType)

        LOG.fine {
            "Successfully listed ${zakenSignaleringen.size} zaken signaleringen of type '$signaleringsType'."
        }

        // Send an 'updated zaken_verdelen' screen event with the job id so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId.let {
            LOG.fine { "Sending 'ZAKEN_SIGNALERINGEN' screen event with ID '$it'." }
            eventingService.send(ScreenEventType.ZAKEN_SIGNALERINGEN.updated(it, zakenSignaleringen))
        }
    }

    /**
     * Lists zaken signaleringen for the given signaleringsType
     */
    fun listZakenSignaleringenPage(
        user: LoggedInUser,
        signaleringsType: SignaleringType.Type,
        pageNumber: Int,
        pageSize: Int
    ): List<RestZaakOverzicht> {
        LOG.fine {
            "Listing page '$pageNumber' ($pageSize elements) for zaken signaleringen with type '$signaleringsType' ..."
        }

        return SignaleringZoekParameters(user)
            .types(signaleringsType)
            .subjecttype(SignaleringSubject.ZAAK)
            .let { listSignaleringen(it, pageNumber, pageSize) }
            .map { zrcClientService.readZaak(UUID.fromString(it.subject)) }
            .map { restZaakOverzichtConverter.convert(it, user) }
            .toList()
            .also {
                LOG.fine { "Successfully listed page $pageNumber for zaken signaleringen of type '$signaleringsType'." }
            }
    }


    private fun getDocument(documentUUID: String) =
        drcClientService.readEnkelvoudigInformatieobject(UUID.fromString(documentUUID))

    private fun getTask(taakID: String) = flowableTaskService.readTask(taakID)

    private fun getZaak(zaakUUID: String): Zaak = zrcClientService.readZaak(UUID.fromString(zaakUUID))

    private fun listZakenSignaleringen(
        user: LoggedInUser,
        signaleringsType: SignaleringType.Type
    ) = SignaleringZoekParameters(user)
        .types(signaleringsType)
        .subjecttype(SignaleringSubject.ZAAK)
        .let { listSignaleringen(it) }
        .map { zrcClientService.readZaak(UUID.fromString(it.subject)) }
        .map { restZaakOverzichtConverter.convert(it, user) }
        .toList()

    private fun signaleringTypeInstance(signaleringsType: SignaleringType.Type): SignaleringType =
        entityManager.find(SignaleringType::class.java, signaleringsType.toString())
}
