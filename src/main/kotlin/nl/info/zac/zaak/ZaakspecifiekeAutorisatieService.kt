/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.pabc.ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.RolMedewerker
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
import nl.info.client.zgw.zrc.util.isZaakspecifiekGeautoriseerd
import nl.info.client.zgw.zrc.util.markZaakspecifiekGeautoriseerd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.zaak.exception.ZaakWithoutBehandelaarCannotBeMarkedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekeAutorisatieCannotBeLiftedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekeAutorisatieNotAllowedException
import nl.info.zac.app.zaak.exception.ZaaktypeNotZaakspecifiekAutoriseerbaarException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.search.IndexingService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.model.ZaakToewijzing

@ApplicationScoped
@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions")
class ZaakspecifiekeAutorisatieService @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zgwApiService: ZgwApiService,
    private val indexingService: IndexingService
) {
    companion object {
        private const val ROLTOELICHTING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER =
            "Zaakspecifiek geautoriseerde medewerker van de zaak"
    }

    fun isZaakspecifiekGeautoriseerd(zaak: Zaak) = zrcClientService.isZaakspecifiekGeautoriseerd(zaak.uuid)

    /**
     * Reads who a zaak is assigned to and who is individually authorised for it.
     *
     * @param rollen pre-fetched rollen for [zaak], to avoid a redundant `listRollen` call when the caller
     * already fetched all rollen for the zaak. When 'null', the rollen are fetched here.
     * @param isZaakspecifiekGeautoriseerd likewise for the marking of [zaak], which a caller that already
     * checked it can pass in. When 'null', it is read here.
     */
    fun readZaakToewijzing(
        zaak: Zaak,
        rollen: List<Rol<*>>? = null,
        isZaakspecifiekGeautoriseerd: Boolean? = null
    ): ZaakToewijzing =
        (rollen ?: zrcClientService.listRollen(zaak)).let { zaakRollen ->
            ZaakToewijzing(
                groep = zgwApiService.findGroepForZaak(zaak, zaakRollen),
                behandelaarRollen = zgwApiService.listBehandelaarMedewerkerRolesForZaak(zaak, zaakRollen),
                zaakspecifiekGeautoriseerdeMedewerkers =
                zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak, zaakRollen),
                isZaakspecifiekGeautoriseerd = isZaakspecifiekGeautoriseerd ?: isZaakspecifiekGeautoriseerd(zaak)
            )
        }

    /**
     * A zaaktype can only be zaakspecifiek geautoriseerd when its catalogus defines both the eigenschap that
     * marks a zaak and the roltype that grants an individual medewerker access to a marked zaak.
     */
    fun isZaakspecifiekAutoriseerbaar(zaakType: ZaakType) =
        ztcClientService.findEigenschap(
            zaaktype = zaakType.url,
            eigenschap = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
        ) != null &&
            zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url) != null

    @Suppress("ThrowsCount")
    fun shouldMarkZaakspecifiekGeautoriseerd(
        zaakType: ZaakType,
        requestedMarking: Boolean?,
        isAlreadyZaakspecifiekGeautoriseerd: Boolean,
        behandelaarId: String?,
        loggedInUser: LoggedInUser
    ): Boolean = when {
        requestedMarking == null -> false
        !requestedMarking -> {
            if (isAlreadyZaakspecifiekGeautoriseerd) throw ZaakspecifiekeAutorisatieCannotBeLiftedException()
            false
        }
        isAlreadyZaakspecifiekGeautoriseerd -> false
        else -> {
            if (!isZaakspecifiekAutoriseerbaar(zaakType)) throw ZaaktypeNotZaakspecifiekAutoriseerbaarException()
            behandelaarId ?: throw ZaakWithoutBehandelaarCannotBeMarkedException()
            if (behandelaarId != loggedInUser.id && !loggedInUser.isZaakspecifiekGeautoriseerdFor(zaakType.omschrijving)) {
                throw ZaakspecifiekeAutorisatieNotAllowedException()
            }
            true
        }
    }

    /**
     * A zaakspecifiek geautoriseerde zaak can be handed over to another behandelaar but never be released:
     * without a behandelaar nobody would be able to pick the zaak up again.
     */
    fun assertBehandelaarMayChange(zaakToewijzing: ZaakToewijzing, requestedBehandelaarId: String?) {
        if (zaakToewijzing.isZaakspecifiekGeautoriseerd &&
            zaakToewijzing.behandelaarRollen.isNotEmpty() &&
            requestedBehandelaarId.isNullOrEmpty()
        ) {
            throw ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException()
        }
    }

    /**
     * Grants [medewerker] individual access to [zaak] by adding a zaakspecifiek geautoriseerde medewerker rol.
     * Does nothing when the medewerker already holds one.
     *
     * @param zaakspecifiekGeautoriseerdeMedewerkers the rollen [zaak] already has, for a caller that read
     * them before. When 'null', they are read here.
     * @return true when a rol was added
     * @throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException when the zaaktype does not define
     * the roltype
     */
    fun grantZaakspecifiekeAutorisatie(
        zaak: Zaak,
        medewerker: MedewerkerIdentificatie,
        reason: String?,
        zaakspecifiekGeautoriseerdeMedewerkers: List<RolMedewerker>? = null
    ): Boolean {
        val medewerkerId = medewerker.identificatie ?: return false
        val roltype = zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak.zaaktype)
            ?: throw ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException(
                "Roltype '${ZgwApiService.ROLTYPE_OMSCHRIJVING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER}' not found " +
                    "for zaaktype '${zaak.zaaktype}' of zaak with UUID '${zaak.uuid}'"
            )
        val isAlreadyGeautoriseerd = (
            zaakspecifiekGeautoriseerdeMedewerkers
                ?: zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak)
            ).any { it.identificatienummer == medewerkerId }
        if (!isAlreadyGeautoriseerd) {
            zrcClientService.createRol(
                RolMedewerker(
                    zaak.url,
                    roltype,
                    ROLTOELICHTING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER,
                    MedewerkerIdentificatie().apply {
                        identificatie = medewerkerId
                        voorletters = medewerker.voorletters
                        voorvoegselAchternaam = medewerker.voorvoegselAchternaam
                        achternaam = medewerker.achternaam
                    }
                ),
                reason
            )
        }
        return !isAlreadyGeautoriseerd
    }

    fun markZaakspecifiekGeautoriseerd(zaak: Zaak) {
        zrcClientService.markZaakspecifiekGeautoriseerd(zaak, ztcClientService)
        indexingService.addOrUpdateZaak(zaak.uuid, inclusiefTaken = false)
        reindexDependents(zaak)
    }

    fun reindexZaakspecifiekeAutorisatieDependents(zaak: Zaak) {
        if (!isZaakspecifiekGeautoriseerd(zaak)) return
        reindexDependents(zaak)
    }

    private fun reindexDependents(zaak: Zaak) {
        indexingService.addOrUpdateTakenForZaak(zaak.uuid)
        indexingService.addOrUpdateInformatieobjectenForZaak(zaak.uuid)
    }

    private fun LoggedInUser.isZaakspecifiekGeautoriseerdFor(zaaktypeOmschrijving: String) =
        ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD in overallRoles ||
            ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD in applicationRolesPerZaaktype[zaaktypeOmschrijving].orEmpty()
}
