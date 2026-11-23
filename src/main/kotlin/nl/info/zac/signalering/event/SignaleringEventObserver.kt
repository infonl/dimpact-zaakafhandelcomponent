/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.event

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.ObservesAsync
import jakarta.inject.Inject
import jakarta.inject.Named
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
import nl.info.zac.event.AbstractEventObserver
import nl.info.zac.flowable.task.FlowableTaskService
import nl.info.zac.flowable.task.readZaakUUID
import nl.info.zac.identity.IdentityService
import nl.info.zac.log.ZacLogger
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.signalering.log.SignaleringEventHandled
import nl.info.zac.signalering.log.SignaleringEventResult
import nl.info.zac.signalering.log.SignaleringTypeIgnored
import nl.info.zac.signalering.log.UnexpectedBetrokkeneType
import nl.info.zac.signalering.model.Signalering
import nl.info.zac.signalering.model.SignaleringType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.task.api.TaskInfo
import java.net.URI

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
        private val LOG = ZacLogger.getLogger(SignaleringEventObserver::class)
    }

    // signaleringen are sent on behalf of ZAC, and this async thread has no user session
    override fun onFire(@ObservesAsync event: SignaleringEvent<*>) = runAsSystemUser { handle(event) }

    private fun handle(event: SignaleringEvent<*>) {
        val signalering = buildSignalering(event) ?: run {
            LOG.fine { event.toSignaleringEventHandled(SignaleringEventResult.NO_SIGNALERING) }
            return
        }
        if (!signaleringService.isNecessary(signalering, event.actor)) {
            LOG.fine { event.toSignaleringEventHandled(SignaleringEventResult.UNNECESSARY) }
            return
        }

        val subscriptions = signaleringService.readInstellingen(signalering)
        LOG.fine {
            event.toSignaleringEventHandled(
                result = SignaleringEventResult.SUBSCRIPTIONS_APPLIED,
                isDashboard = subscriptions.isDashboard,
                isMail = subscriptions.isMail
            )
        }
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
                val zaak = zrcClientService.readZaak(event.objectId.resource as URI)
                val zaakInformatieObject = zrcClientService.readZaakinformatieobject(
                    (event.objectId.detail as URI).extractUuid()
                )
                getSignaleringVoorBehandelaar(event = event, zaak = zaak, zaakInformatieObject = zaakInformatieObject)
            }
            SignaleringType.Type.ZAAK_OP_NAAM -> {
                val rol = zrcClientService.readRol(event.objectId.resource as URI)
                if (isBehandelaarRol(rol)) getSignaleringVoorBehandelaarRol(event = event, rol = rol) else null
            }
            SignaleringType.Type.TAAK_OP_NAAM -> {
                val task = flowableTaskService.readOpenTask(event.objectId.resource as String)
                getSignaleringVoorBehandelaar(event = fixActor(event = event, task = task), task = task)
            }
            SignaleringType.Type.TAAK_AFGEROND -> {
                val task = flowableTaskService.readTask(event.objectId.resource as String)
                val zaak = zrcClientService.readZaak(readZaakUUID(task))
                getSignaleringVoorBehandelaar(
                    event = event,
                    task = task,
                    username = getRolBehandelaarMedewerker(zaak)?.toUsername()
                )
            }
            SignaleringType.Type.ZAAK_VERLOPEND, SignaleringType.Type.TAAK_VERLOPEN -> {
                // these are not event-driven and should not show up here
                LOG.warning(SignaleringTypeIgnored(signaleringType = event.objectType))
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
                LOG.warning(UnexpectedBetrokkeneType(betrokkeneType = rol.betrokkeneType?.toString()))
                null
            }
        }
    }

    private fun getSignaleringVoorMedewerker(event: SignaleringEvent<*>, zaak: Zaak, rolMedewerker: RolMedewerker) =
        signaleringService.signaleringInstance(event.objectType).apply {
            setSubject(zaak)
            setTarget(
                identityService.readUser(
                    username = rolMedewerker.toUsername(),
                    logContext = "a signalering about zaak '${zaak.identificatie}'"
                )
            )
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
            setTarget(
                identityService.readGroup(
                    groupId = organisatorischeEenheidIdentificatie.identificatie,
                    logContext = "a signalering about zaak '${zaak.identificatie}'"
                )
            )
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

    private fun getSignaleringVoorBehandelaar(
        event: SignaleringEvent<*>,
        task: TaskInfo,
        username: String? = task.assignee
    ) = username?.let {
        signaleringService.signaleringInstance(event.objectType).apply {
            setSubject(task)
            setTarget(identityService.readUser(username = it, logContext = "a signalering about task '${task.id}'"))
        }
    }

    /**
     * The owner of a newly created human task is assumed to be the actor who created it.
     */
    private fun fixActor(event: SignaleringEvent<*>, task: TaskInfo): SignaleringEvent<*> =
        if (event.actor == null) {
            val actor = task.owner?.let(identityService::readUser)
            signaleringEvent(event.objectType, task, actor)
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

private fun RolMedewerker.toUsername() =
    checkNotNull(betrokkeneIdentificatie) { "Rol '$uuid' has no medewerker" }.identificatie

private fun SignaleringEvent<*>.toSignaleringEventHandled(
    result: SignaleringEventResult,
    isDashboard: Boolean = false,
    isMail: Boolean = false
) = SignaleringEventHandled(
    signaleringType = objectType,
    objectId = objectId.toString(),
    result = result,
    isDashboard = isDashboard,
    isMail = isMail
)
