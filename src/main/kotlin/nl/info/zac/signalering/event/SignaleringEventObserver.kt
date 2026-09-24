/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.event

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.ObservesAsync
import jakarta.inject.Inject
import jakarta.inject.Named
import net.atos.zac.event.AbstractEventObserver
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringType
import nl.info.client.zgw.shared.ZgwApiService.Companion.ROLTYPE_OMSCHRIJVING_BEHANDELAAR
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.RolListParameters
import nl.info.client.zgw.zrc.model.RolMedewerker
import nl.info.client.zgw.zrc.model.RolOrganisatorischeEenheid
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.authentication.runAsSystemUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.task.api.TaskInfo
import java.net.URI
import java.util.logging.Logger

/**
 * Turns [SignaleringEvent]s into signaleringen for the users and groups they concern.
 */
@Named
@ApplicationScoped
@NoArgConstructor
@AllOpen
class SignaleringEventObserver @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
    private val flowableTaskService: FlowableTaskService,
    private val identityService: IdentityService,
    private val signaleringService: SignaleringService
) : AbstractEventObserver<SignaleringEvent<*>>() {
    companion object {
        private val LOG = Logger.getLogger(SignaleringEventObserver::class.java.name)
    }

    /**
     * Signaleringen are sent on behalf of ZAC, not of the user who caused the event: that user is the
     * event's actor, and the async observer thread has no session of its own.
     */
    override fun onFire(@ObservesAsync event: SignaleringEvent<*>) = runAsSystemUser { handle(event) }

    private fun handle(event: SignaleringEvent<*>) {
        LOG.fine { "Signalering event ontvangen: $event" }
        event.delay()

        val signalering = buildSignalering(event) ?: run {
            LOG.fine { "No signal generated for received event: $event" }
            return
        }
        if (!signaleringService.isNecessary(signalering, event.actor)) {
            LOG.fine { "Unnecessary signalering: $signalering for actor ${event.actor}" }
            return
        }

        val subscriptions = signaleringService.readInstellingen(signalering)
        LOG.fine { "Subscription settings: $subscriptions for signalering: $signalering" }
        if (subscriptions.isDashboard) {
            signaleringService.storeSignalering(signalering)
        }
        if (subscriptions.isMail) {
            signaleringService.sendSignalering(signalering)
        }
    }

    private fun buildSignalering(event: SignaleringEvent<*>): Signalering? =
        when (event.objectType) {
            SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD -> {
                val zaak = zrcClientService.readZaak(event.objectId.resource() as URI)
                val zaakInformatieObject = zrcClientService.readZaakinformatieobject(
                    (event.objectId.detail() as URI).extractUuid()
                )
                getSignaleringVoorBehandelaar(event, zaak, zaakInformatieObject)
            }
            SignaleringType.Type.ZAAK_OP_NAAM -> {
                val rol = zrcClientService.readRol(event.objectId.resource() as URI)
                if (OmschrijvingGeneriekEnum.valueOf(rol.omschrijvingGeneriek.uppercase()) ==
                    OmschrijvingGeneriekEnum.BEHANDELAAR
                ) {
                    val zaak = zrcClientService.readZaak(rol.zaak!!)
                    when (rol.betrokkeneType) {
                        BetrokkeneTypeEnum.MEDEWERKER -> getSignaleringVoorRol(event, zaak, rol as RolMedewerker)
                        BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID ->
                            getSignaleringVoorGroup(event, zaak, rol as RolOrganisatorischeEenheid)
                        else -> {
                            LOG.warning { "unexpected BetrokkeneType ${rol.betrokkeneType}" }
                            null
                        }
                    }
                } else {
                    null
                }
            }
            SignaleringType.Type.TAAK_OP_NAAM -> {
                val task = flowableTaskService.readOpenTask(event.objectId.resource() as String)
                getSignaleringVoorBehandelaar(fixActor(event, task), task)
            }
            SignaleringType.Type.ZAAK_VERLOPEND, SignaleringType.Type.TAAK_VERLOPEN -> {
                // These are NOT event-driven and should not show up here
                LOG.warning { "ignored SignaleringType ${event.objectType}" }
                null
            }
            else -> null
        }

    private fun getSignaleringVoorRol(event: SignaleringEvent<*>, zaak: Zaak, rol: Rol<*>): Signalering? =
        signaleringService.signaleringInstance(event.objectType).apply { setSubject(zaak) }.let { addTarget(it, rol) }

    private fun getSignaleringVoorGroup(event: SignaleringEvent<*>, zaak: Zaak, rol: RolOrganisatorischeEenheid) =
        if (getRolBehandelaarMedewerker(zaak) == null) getSignaleringVoorRol(event, zaak, rol) else null

    private fun getSignaleringVoorBehandelaar(
        event: SignaleringEvent<*>,
        zaak: Zaak,
        zaakInformatieObject: ZaakInformatieObject
    ) = getRolBehandelaarMedewerker(zaak)?.let { behandelaar ->
        getSignaleringVoorRol(event, zaak, behandelaar)?.apply {
            setDetailFromZaakInformatieobject(zaakInformatieObject)
        }
    }

    private fun getSignaleringVoorBehandelaar(event: SignaleringEvent<*>, task: TaskInfo) =
        task.assignee?.let { assignee ->
            signaleringService.signaleringInstance(event.objectType).apply {
                setSubject(task)
                setTarget(identityService.readUser(assignee))
            }
        }

    /**
     * The owner of a newly created human task is assumed to be the actor who created it.
     */
    private fun fixActor(event: SignaleringEvent<*>, task: TaskInfo): SignaleringEvent<*> =
        if (event.actor == null) {
            val actor = task.owner?.let(identityService::readUser)
            SignaleringEventUtil.event(event.objectType, task, actor).also {
                if (actor != null) LOG.fine { "Signalering event fixed: $it" }
            }
        } else {
            event
        }

    private fun getRolBehandelaarMedewerker(zaak: Zaak): Rol<*>? =
        zrcClientService.listRollen(
            RolListParameters(
                zaak = zaak.url,
                roltype = ztcClientService.readRoltype(
                    zaaktypeURI = zaak.zaaktype,
                    omschrijvingGeneriekEnum = OmschrijvingGeneriekEnum.BEHANDELAAR,
                    omschrijving = ROLTYPE_OMSCHRIJVING_BEHANDELAAR
                ).url,
                betrokkeneType = BetrokkeneTypeEnum.MEDEWERKER
            )
        ).singleResult

    private fun addTarget(signalering: Signalering, rol: Rol<*>): Signalering? =
        when (rol.betrokkeneType) {
            BetrokkeneTypeEnum.MEDEWERKER -> signalering.apply {
                setTarget(identityService.readUser((rol as RolMedewerker).betrokkeneIdentificatie!!.identificatie))
            }
            BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID -> signalering.apply {
                setTarget(
                    identityService.readGroup((rol as RolOrganisatorischeEenheid).betrokkeneIdentificatie!!.identificatie)
                )
            }
            else -> {
                LOG.warning { "Unknown BetrokkeneType '${rol.betrokkeneType}'" }
                null
            }
        }
}
