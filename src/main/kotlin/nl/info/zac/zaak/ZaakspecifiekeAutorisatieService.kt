/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import jakarta.inject.Inject
import nl.info.client.pabc.ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
import nl.info.client.zgw.zrc.util.isZaakspecifiekGeautoriseerd
import nl.info.client.zgw.zrc.util.markZaakspecifiekGeautoriseerd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.zaak.exception.ZaakWithoutBehandelaarCannotBeMarkedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReassignedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekeAutorisatieCannotBeLiftedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekeAutorisatieNotAllowedException
import nl.info.zac.app.zaak.exception.ZaaktypeNotZaakspecifiekAutoriseerbaarException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.search.IndexingService
import nl.info.zac.util.AllOpen

@AllOpen
class ZaakspecifiekeAutorisatieService @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zgwApiService: ZgwApiService,
    private val indexingService: IndexingService
) {
    fun isZaakspecifiekGeautoriseerd(zaak: Zaak) = zrcClientService.isZaakspecifiekGeautoriseerd(zaak.uuid)

    fun isZaakspecifiekAutoriseerbaar(zaakType: ZaakType) =
        ztcClientService.findEigenschap(
            zaaktype = zaakType.url,
            eigenschap = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
        ) != null

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

    fun assertBehandelaarNotReassigned(requestedBehandelaarId: String?, currentBehandelaarId: String?) {
        currentBehandelaarId ?: return
        requestedBehandelaarId?.let {
            if (it != currentBehandelaarId) throw ZaakspecifiekGeautoriseerdeZaakCannotBeReassignedException()
        }
    }

    fun assertBehandelaarMayChange(zaak: Zaak, userName: String?) {
        if (!isZaakspecifiekGeautoriseerd(zaak)) return
        val currentBehandelaarId = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            ?.betrokkeneIdentificatie
            ?.identificatie
            ?: return
        if (userName != currentBehandelaarId) {
            throw if (userName.isNullOrEmpty()) {
                ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException()
            } else {
                ZaakspecifiekGeautoriseerdeZaakCannotBeReassignedException()
            }
        }
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
