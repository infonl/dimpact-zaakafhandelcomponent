/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.TakenService
import net.atos.zac.mail.MailService
import net.atos.zac.mail.model.Bronnen
import net.atos.zac.mailtemplates.MailTemplateService
import net.atos.zac.mailtemplates.model.Mail
import net.atos.zac.mailtemplates.model.MailGegevens
import net.atos.zac.mailtemplates.model.MailTemplate
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringDetail
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
import nl.lifely.zac.util.NoArgConstructor
import org.flowable.task.api.TaskInfo
import java.time.ZonedDateTime
import java.util.Arrays
import java.util.Optional
import java.util.UUID
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors

@ApplicationScoped
@Transactional
@Suppress("TooManyFunctions", "LongParameterList")
@NoArgConstructor
open class SignaleringenService @Inject constructor(
    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private val entityManager: EntityManager,
    private val eventingService: EventingService,
    private val mailService: MailService,
    private val mailTemplateService: MailTemplateService,
    private val signaleringenMailHelper: SignaleringenMailHelper,
    private val zrcClientService: ZRCClientService,
    private val takenService: TakenService,
    private val drcClientService: DRCClientService
) {

    private fun signaleringTypeInstance(signaleringsType: SignaleringType.Type?): SignaleringType {
        return entityManager.find(SignaleringType::class.java, signaleringsType.toString())
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
    fun signaleringVerzondenInstance(signalering: Signalering?): SignaleringVerzonden {
        val instance = SignaleringVerzonden()
        instance.tijdstip = ZonedDateTime.now()
        instance.type = signaleringTypeInstance(signalering!!.type.type)
        instance.targettype = signalering.targettype
        instance.target = signalering.target
        instance.subject = signalering.subject
        instance.detail = signalering.detail
        return instance
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
            .forEach(
                Consumer<Signalering> { signalering: Signalering ->
                    removed[signalering.target + ';' + signalering.type.type] = signalering
                    entityManager.remove(signalering)
                }
            )
        removed.values
            .forEach(
                Consumer { signalering: Signalering? ->
                    eventingService.send(
                        ScreenEventType.SIGNALERINGEN.updated(
                            signalering
                        )
                    )
                }
            )
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

        return if (resultList != null && !resultList.isEmpty()) {
            resultList[0]
        } else {
            null
        }
    }

    private fun getSignaleringWhere(
        parameters: SignaleringZoekParameters,
        builder: CriteriaBuilder,
        root: Root<Signalering>
    ): Predicate {
        val where: MutableList<Predicate> = ArrayList()
        where.add(builder.equal(root.get<Any>("targettype"), parameters.targettype))
        if (parameters.target != null) {
            where.add(builder.equal(root.get<Any>("target"), parameters.target))
        }
        if (!parameters.types.isEmpty()) {
            where.add(
                root.get<Any>("type").get<Any>("id")
                    .`in`(
                        parameters.types.stream().map { obj: SignaleringType.Type -> obj.toString() }
                            .collect(Collectors.toList())
                    )
            )
        }
        if (parameters.subjecttype != null) {
            where.add(builder.equal(root.get<Any>("type").get<Any>("subjecttype"), parameters.subjecttype))
            if (parameters.subject != null) {
                where.add(builder.equal(root.get<Any>("subject"), parameters.subject))
            }
        }
        @Suppress("SpreadOperator")
        return builder.and(*where.toTypedArray<Predicate>())
    }

    fun sendSignalering(signalering: Signalering?) {
        ValidationUtil.valideerObject(signalering)
        val mail = signaleringenMailHelper.getTargetMail(signalering)
        if (mail != null) {
            val from = mailService.gemeenteMailAdres
            val to = signaleringenMailHelper.formatTo(mail)
            val mailTemplate = getMailtemplate(signalering)
            val bronnenBuilder = Bronnen.Builder()
            when (signalering!!.subjecttype!!) {
                SignaleringSubject.ZAAK -> {
                    bronnenBuilder.add(getZaak(signalering.subject))
                    if (signalering.type.type === SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD) {
                        bronnenBuilder.add(getDocument(signalering.detail))
                    }
                }

                SignaleringSubject.TAAK -> bronnenBuilder.add(getTaak(signalering.subject))
                SignaleringSubject.DOCUMENT -> bronnenBuilder.add(getDocument(signalering.subject))
            }
            mailService.sendMail(
                MailGegevens(from, to, mailTemplate.onderwerp, mailTemplate.body),
                bronnenBuilder.build()
            )
        }
    }

    private fun getZaak(zaakUUID: String): Zaak {
        return zrcClientService.readZaak(UUID.fromString(zaakUUID))
    }

    private fun getTaak(taakID: String): TaskInfo {
        return takenService.readTask(taakID)
    }

    private fun getDocument(documentUUID: String): EnkelvoudigInformatieObject {
        return drcClientService.readEnkelvoudigInformatieobject(UUID.fromString(documentUUID))
    }

    private fun getMailtemplate(signalering: Signalering?): MailTemplate {
        return mailTemplateService.readMailtemplate(
            when (signalering!!.type.type!!) {
                SignaleringType.Type.TAAK_OP_NAAM -> Mail.SIGNALERING_TAAK_OP_NAAM
                SignaleringType.Type.TAAK_VERLOPEN -> Mail.SIGNALERING_TAAK_VERLOPEN
                SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD -> Mail.SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD
                SignaleringType.Type.ZAAK_OP_NAAM -> Mail.SIGNALERING_ZAAK_OP_NAAM
                SignaleringType.Type.ZAAK_VERLOPEND -> when (SignaleringDetail.valueOf(signalering.detail)) {
                    SignaleringDetail.STREEFDATUM -> Mail.SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM
                    SignaleringDetail.FATALE_DATUM -> Mail.SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM
                }
            }
        )
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
                .where(getSignaleringInstellingenWhere(parameters, builder, root))
        )
            .resultList
    }

    private fun getSignaleringInstellingenWhere(
        parameters: SignaleringInstellingenZoekParameters,
        builder: CriteriaBuilder,
        root: Root<SignaleringInstellingen>
    ): Predicate {
        val where: MutableList<Predicate> = ArrayList()
        if (parameters.owner != null) {
            when (parameters.ownertype!!) {
                SignaleringTarget.GROUP -> {
                    where.add(builder.equal(root.get<Any>("groep"), parameters.owner))
                }

                SignaleringTarget.USER -> {
                    where.add(builder.equal(root.get<Any>("medewerker"), parameters.owner))
                }
            }
        }
        if (parameters.type != null) {
            where.add(builder.equal(root.get<Any>("type").get<Any>("id"), parameters.type.toString()))
        }
        if (parameters.dashboard) {
            where.add(builder.isTrue(root.get("dashboard")))
        }
        if (parameters.mail) {
            where.add(builder.isTrue(root.get("mail")))
        }
        @Suppress("SpreadOperator")
        return builder.and(*where.toTypedArray<Predicate>())
    }

    fun listInstellingenInclusiefMogelijke(
        parameters: SignaleringInstellingenZoekParameters
    ): List<SignaleringInstellingen> {
        val map = listInstellingen(parameters).stream()
            .collect(
                Collectors.toMap(
                    Function { instellingen: SignaleringInstellingen -> instellingen.type.type },
                    Function.identity()
                )
            )
        Arrays.stream(SignaleringType.Type.entries.toTypedArray())
            .filter { type: SignaleringType.Type -> type.isTarget(parameters.ownertype) }
            .filter { type: SignaleringType.Type -> !map.containsKey(type) }
            .forEach { type: SignaleringType.Type ->
                map[type] = signaleringInstellingenInstance(
                    type, parameters.ownertype,
                    parameters.owner
                )
            }
        return map.values.stream()
            .sorted(Comparator.comparing { obj: SignaleringInstellingen -> obj.type })
            .toList()
    }

    fun count(): Int {
        return SignaleringType.Type.entries.size
    }

    fun createSignaleringVerzonden(signalering: Signalering?): SignaleringVerzonden {
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
                .where(getSignaleringVerzondenWhere(parameters, builder, root))
        )
            .resultList
        return if (result.isEmpty()) { Optional.empty() } else { Optional.of(result[0]) }
    }

    private fun getSignaleringVerzondenWhere(
        parameters: SignaleringVerzondenZoekParameters,
        builder: CriteriaBuilder,
        root: Root<SignaleringVerzonden>
    ): Predicate {
        val where: MutableList<Predicate> = ArrayList()
        where.add(builder.equal(root.get<Any>("targettype"), parameters.targettype))
        if (parameters.target != null) {
            where.add(builder.equal(root.get<Any>("target"), parameters.target))
        }
        if (!parameters.types.isEmpty()) {
            where.add(
                root.get<Any>("type").get<Any>("id")
                    .`in`(
                        parameters.types.stream().map { obj: SignaleringType.Type -> obj.toString() }
                            .collect(Collectors.toList())
                    )
            )
        }
        if (parameters.subjecttype != null) {
            where.add(builder.equal(root.get<Any>("type").get<Any>("subjecttype"), parameters.subjecttype))
            if (parameters.subject != null) {
                where.add(builder.equal(root.get<Any>("subject"), parameters.subject))
            }
        }
        if (parameters.detail != null) {
            where.add(builder.equal(root.get<Any>("detail"), parameters.detail.toString()))
        }
        @Suppress("SpreadOperator")
        return builder.and(*where.toTypedArray<Predicate>())
    }
}
