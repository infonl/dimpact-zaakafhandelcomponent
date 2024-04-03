/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import net.atos.zac.event.EventingService
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
import java.util.Arrays
import java.util.Optional
import java.util.function.Function
import java.util.stream.Collectors

@ApplicationScoped
@Transactional
@Suppress("TooManyFunctions")
@NoArgConstructor
@AllOpen
class SignaleringenService @Inject constructor(
    private val eventingService: EventingService,
    private val mailService: MailService,
    private val signaleringenMailHelper: SignaleringenMailHelper,
    private val signaleringenZACHelper: SignaleringenZACHelper,
    private val signaleringenPredicateHelper: SignaleringenPredicateHelper
) {

    companion object {
        @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
        private lateinit var entityManager: EntityManager

        private fun signaleringTypeInstance(signaleringsType: SignaleringType.Type?): SignaleringType {
            return entityManager.find(SignaleringType::class.java, signaleringsType.toString())
        }
    }

    /**
     * Factory method for constructing Signalering instances.
     *
     * @param signaleringsType the type of the signalering to construct
     * @return the constructed instance (subject and target are still null, type and tijdstip have been set)
     */
    fun signaleringInstance(signaleringsType: SignaleringType.Type?): Signalering {
        val instance = Signalering()
        instance.tijdstip = ZonedDateTime.now()
        instance.type = signaleringTypeInstance(signaleringsType)
        return instance
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
    ): SignaleringInstellingen {
        return SignaleringInstellingen(signaleringTypeInstance(signaleringsType), ownerType, ownerId)
    }

    /**
     * Factory method for constructing SignaleringVerzonden instances.
     *
     * @param signalering the signalering that has been sent
     * @return the constructed instance (all members have been set)
     */
    fun signaleringVerzondenInstance(signalering: Signalering?): SignaleringVerzonden =
        SignaleringVerzonden().let {
            it.tijdstip = ZonedDateTime.now()
            it.type = signaleringTypeInstance(signalering!!.type.type)
            it.targettype = signalering.targettype
            it.target = signalering.target
            it.subject = signalering.subject
            it.detail = signalering.detail
            return it
        }

    /**
     * Business logic for deciding if signalling is necessary. Groep-targets will always get signalled but user-targets
     * only when they are not themselves the actor that caused the event (or when the actor is unknown).
     *
     * @param signalering the signalering (should have the target set)
     * @param actor       the actor (a gebruikersnaam) or null if unknown
     * @return true if signalling is necessary
     */
    fun isNecessary(signalering: Signalering, actor: String): Boolean {
        return signalering.targettype != SignaleringTarget.USER || signalering.target != actor
    }

    fun createSignalering(signalering: Signalering): Signalering {
        ValidationUtil.valideerObject(signalering)
        val created = entityManager.merge(signalering)
        eventingService.send(ScreenEventType.SIGNALERINGEN.updated(created))
        return created
    }

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

    fun listSignaleringen(parameters: SignaleringZoekParameters): List<Signalering> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            Signalering::class.java
        )
        val root = query.from(Signalering::class.java)
        return entityManager.createQuery(
            query.select(root)
                .where(signaleringenPredicateHelper.getSignaleringWhere(parameters, builder, root))
                .orderBy(builder.desc(root.get<Any>("tijdstip")))
        )
            .resultList
    }

    fun latestSignalering(parameters: SignaleringZoekParameters): ZonedDateTime? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            ZonedDateTime::class.java
        )
        val root = query.from(Signalering::class.java)

        query.select(root.get("tijdstip"))
            .where(signaleringenPredicateHelper.getSignaleringWhere(parameters, builder, root))
            .orderBy(builder.desc(root.get<Any>("tijdstip")))

        val resultList = entityManager.createQuery(query).resultList

        return if (resultList != null && resultList.isNotEmpty()) {
            resultList[0]
        } else {
            null
        }
    }

    fun sendSignalering(signalering: Signalering) {
        ValidationUtil.valideerObject(signalering)
        val mail = signaleringenMailHelper.getTargetMail(signalering)
        if (mail != null) {
            val from = mailService.gemeenteMailAdres
            val to = signaleringenMailHelper.formatTo(mail)
            val mailTemplate = signaleringenMailHelper.getMailTemplate(signalering)
            val bronnenBuilder = Bronnen.Builder()
            when (signalering.subjecttype!!) {
                SignaleringSubject.ZAAK -> {
                    bronnenBuilder.add(signaleringenZACHelper.getZaak(signalering.subject))
                    if (signalering.type.type === SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD) {
                        bronnenBuilder.add(signaleringenZACHelper.getDocument(signalering.detail))
                    }
                }

                SignaleringSubject.TAAK -> bronnenBuilder.add(signaleringenZACHelper.getTaak(signalering.subject))
                SignaleringSubject.DOCUMENT -> bronnenBuilder.add(
                    signaleringenZACHelper.getDocument(signalering.subject)
                )
            }
            mailService.sendMail(
                MailGegevens(from, to, mailTemplate.onderwerp, mailTemplate.body),
                bronnenBuilder.build()
            )
        }
    }

    fun createUpdateOrDeleteInstellingen(instellingen: SignaleringInstellingen): SignaleringInstellingen? {
        ValidationUtil.valideerObject(instellingen)
        if (instellingen.isEmpty) {
            if (instellingen.id != null) {
                entityManager.remove(entityManager.find(SignaleringInstellingen::class.java, instellingen.id))
            }
            return null
        }
        return entityManager.merge(instellingen)
    }

    fun readInstellingenGroup(type: SignaleringType.Type?, target: String?): SignaleringInstellingen {
        val signalering = signaleringInstance(type)
        signalering.setTargetGroup(target)
        return readInstellingen(signalering)
    }

    fun readInstellingenUser(type: SignaleringType.Type?, target: String?): SignaleringInstellingen {
        val signalering = signaleringInstance(type)
        signalering.setTargetUser(target)
        return readInstellingen(signalering)
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
            query.select(root)
                .where(signaleringenPredicateHelper.getSignaleringInstellingenWhere(parameters, builder, root))
        )
            .resultList
    }

    fun listInstellingenInclusiefMogelijke(
        parameters: SignaleringInstellingenZoekParameters
    ): List<SignaleringInstellingen> {
        val map = listInstellingen(parameters).stream()
            .collect(
                Collectors.toMap(
                    { instellingen: SignaleringInstellingen -> instellingen.type.type },
                    Function.identity()
                )
            )
        Arrays.stream(SignaleringType.Type.entries.toTypedArray())
            .filter { it.isTarget(parameters.ownertype) }
            .filter { !map.containsKey(it) }
            .forEach {
                map[it] = signaleringInstellingenInstance(it, parameters.ownertype, parameters.owner)
            }
        return map.values.stream()
            .sorted(Comparator.comparing { it.type })
            .toList()
    }

    fun count(): Int {
        return SignaleringType.Type.entries.size
    }

    fun createSignaleringVerzonden(signalering: Signalering): SignaleringVerzonden {
        val signaleringVerzonden = signaleringVerzondenInstance(signalering)
        ValidationUtil.valideerObject(signaleringVerzonden)
        return entityManager.merge(signaleringVerzonden)
    }

    fun deleteSignaleringVerzonden(verzonden: SignaleringVerzondenZoekParameters) {
        findSignaleringVerzonden(
            verzonden
        ).ifPresent { entity: SignaleringVerzonden? -> entityManager.remove(entity) }
    }

    fun findSignaleringVerzonden(
        parameters: SignaleringVerzondenZoekParameters
    ): Optional<SignaleringVerzonden> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            SignaleringVerzonden::class.java
        )
        val root = query.from(
            SignaleringVerzonden::class.java
        )
        val result = entityManager.createQuery(
            query.select(root)
                .where(signaleringenPredicateHelper.getSignaleringVerzondenWhere(parameters, builder, root))
        )
            .resultList
        return if (result.isEmpty()) { Optional.empty() } else { Optional.of(result[0]) }
    }
}
