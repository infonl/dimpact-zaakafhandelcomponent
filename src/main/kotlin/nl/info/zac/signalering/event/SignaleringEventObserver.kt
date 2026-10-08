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
import nl.info.client.zgw.shared.ZgwApiService
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
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.authentication.runAsSystemUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.task.api.TaskInfo
import java.net.URI
import java.util.logging.Logger

@Named
@ApplicationScoped
@NoArgConstructor
@AllOpen
class SignaleringEventObserver @Inject constructor(
    private val zgwApiService: ZgwApiService,
    private val zrcClientService: ZrcClientService,
    private val flowableTaskService: FlowableTaskService,
    private val identityService: IdentityService,
    private val signaleringService: SignaleringService
) : AbstractEventObserver<SignaleringEvent<*>>() {
    companion object {
        private val LOG = Logger.getLogger(SignaleringEventObserver::class.java.name)
    }

    // signaleringen are sent on behalf of ZAC, and this async thread has no user session
    override fun onFire(@ObservesAsync event: SignaleringEvent<*>) = runAsSystemUser { handle(event) }

    private fun handle(event: SignaleringEvent<*>) {
        LOG.fine { "Signalering event received: $event" }
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
                getSignaleringVoorBehandelaar(event = event, zaak = zaak, zaakInformatieObject = zaakInformatieObject)
            }
            SignaleringType.Type.ZAAK_OP_NAAM -> {
                val rol = zrcClientService.readRol(event.objectId.resource() as URI)
                if (isBehandelaarRol(rol)) getSignaleringVoorBehandelaarRol(event = event, rol = rol) else null
            }
            SignaleringType.Type.TAAK_OP_NAAM -> {
                val task = flowableTaskService.readOpenTask(event.objectId.resource() as String)
                getSignaleringVoorBehandelaar(event = fixActor(event = event, task = task), task = task)
            }
            SignaleringType.Type.ZAAK_VERLOPEND, SignaleringType.Type.TAAK_VERLOPEN -> {
                // these are not event-driven and should not show up here
                LOG.warning { "Ignored SignaleringType ${event.objectType}" }
                null
            }
        }

    private fun getSignaleringVoorBehandelaarRol(event: SignaleringEvent<*>, rol: Rol<*>): Signalering? {
        val zaak = zrcClientService.readZaak(checkNotNull(rol.zaak) { "Rol '${rol.uuid}' has no zaak" })
        return when (rol) {
            is RolMedewerker -> getSignaleringVoorMedewerker(event = event, zaak = zaak, rolMedewerker = rol)
            is RolOrganisatorischeEenheid ->
                getSignaleringVoorGroup(event = event, zaak = zaak, rolOrganisatorischeEenheid = rol)
            else -> {
                LOG.warning { "Unexpected BetrokkeneType ${rol.betrokkeneType}" }
                null
            }
        }
    }

    private fun getSignaleringVoorMedewerker(event: SignaleringEvent<*>, zaak: Zaak, rolMedewerker: RolMedewerker) =
        signaleringService.signaleringInstance(event.objectType).apply {
            setSubject(zaak)
            val medewerkerIdentificatie = checkNotNull(rolMedewerker.betrokkeneIdentificatie) {
                "Rol '${rolMedewerker.uuid}' has no medewerker"
            }
            setTarget(identityService.readUser(medewerkerIdentificatie.identificatie))
        }

    private fun getSignaleringVoorGroup(
        event: SignaleringEvent<*>,
        zaak: Zaak,
        rolOrganisatorischeEenheid: RolOrganisatorischeEenheid
    ) = if (getRolBehandelaarMedewerker(zaak) == null) {
        signaleringService.signaleringInstance(event.objectType).apply {
            setSubject(zaak)
            val organisatorischeEenheidIdentificatie = checkNotNull(rolOrganisatorischeEenheid.betrokkeneIdentificatie) {
                "Rol '${rolOrganisatorischeEenheid.uuid}' has no organisatorische eenheid"
            }
            setTarget(identityService.readGroup(organisatorischeEenheidIdentificatie.identificatie))
        }
    } else {
        null
    }

    private fun getSignaleringVoorBehandelaar(
        event: SignaleringEvent<*>,
        zaak: Zaak,
        zaakInformatieObject: ZaakInformatieObject
    ) = getRolBehandelaarMedewerker(zaak)?.let { behandelaar ->
        getSignaleringVoorMedewerker(event = event, zaak = zaak, rolMedewerker = behandelaar)
            .apply { setDetailFromZaakInformatieobject(zaakInformatieObject) }
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

    private fun isBehandelaarRol(rol: Rol<*>) =
        OmschrijvingGeneriekEnum.valueOf(rol.omschrijvingGeneriek.uppercase()) == OmschrijvingGeneriekEnum.BEHANDELAAR &&
            rol.omschrijving == ROLTYPE_OMSCHRIJVING_BEHANDELAAR

    private fun getRolBehandelaarMedewerker(zaak: Zaak) =
        zrcClientService.listRollen(
            RolListParameters(
                zaak = zaak.url,
                roltype = zgwApiService.readBehandelaarRoltype(zaak.zaaktype).url,
                betrokkeneType = BetrokkeneTypeEnum.MEDEWERKER
            )
        ).singleResult as RolMedewerker?
}
