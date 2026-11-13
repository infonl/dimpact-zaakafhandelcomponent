/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.app.zaak.model.RestZaaktypeAanmaakInstellingen
import nl.info.zac.app.zaak.model.RestZaaktypeKeuze
import nl.info.zac.app.zaak.model.toRestZaaktypeAanmaakInstellingen
import nl.info.zac.app.zaak.model.toRestZaaktypeKeuze
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.flowable.ZaakProcessService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@Path("zaaktypen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
class ZaaktypeRestService @Inject constructor(
    private val configurationService: ConfigurationService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val zaakProcessService: ZaakProcessService,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val ztcClientService: ZtcClientService
) {
    /**
     * Lists the zaaktypes of the default catalogus. With [startbaar] `true` only the zaaktypes that the user can create
     * a zaak of, with `false` only the others.
     */
    @GET
    fun listZaaktypen(@QueryParam("startbaar") startbaar: Boolean?): List<RestZaaktypeKeuze> {
        val loggedInUser = lazy { loggedInUserInstance.get() }
        val startrollen = lazy { policyService.readStartrollen() }
        return ztcClientService.listZaaktypen(configurationService.readDefaultCatalogusURI())
            .asSequence()
            .filter { startbaar == null || it.isOfferedForZaakCreation(loggedInUser.value, startrollen) == startbaar }
            .map { it.toRestZaaktypeKeuze() }
            .toList()
    }

    /**
     * Reads the settings that the Zaak aanmaken form applies once the user chose the zaaktype.
     *
     * @throws nl.info.zac.policy.exception.PolicyException when the user may not start a zaak of the zaaktype
     * @throws NotFoundException when the zaaktype does not exist, or [listZaaktypen] with `startbaar=true` does not
     * offer it
     */
    @GET
    @Path("{zaaktypeUuid}")
    fun readZaaktype(
        @PathParam("zaaktypeUuid") zaaktypeUuid: UUID
    ): RestZaaktypeAanmaakInstellingen {
        val zaaktype = ztcClientService.listZaaktypen(configurationService.readDefaultCatalogusURI())
            .find { it.url.extractUuid() == zaaktypeUuid }
            ?: throw NotFoundException("Zaaktype '$zaaktypeUuid' is not offered for zaak creation")
        val loggedInUser = loggedInUserInstance.get()
        val startrollen = lazy { policyService.readStartrollen() }
        assertPolicy(loggedInUser.mayStartZaakOfZaaktype(zaaktype.omschrijving, startrollen.value))
        if (!zaaktype.isOfferedForZaakCreation(loggedInUser, startrollen)) {
            throw NotFoundException("Zaaktype '$zaaktypeUuid' is not offered for zaak creation")
        }
        return zaaktype.toRestZaaktypeAanmaakInstellingen(
            zaaktypeConfigurationService.readConfiguration(zaaktypeUuid)
        )
    }

    // Mirrors OPA's `starten_zaak` rule, which evaluates the roles for the zaaktype together with the overall roles,
    // so that listing the zaaktypen takes one OPA call instead of one per zaaktype.
    private fun LoggedInUser.mayStartZaakOfZaaktype(zaaktypeOmschrijving: String, startrollen: Set<String>) =
        (applicationRolesPerZaaktype[zaaktypeOmschrijving].orEmpty() + overallRoles).any(startrollen::contains)

    /**
     * Decides which zaaktypes a user can create a zaak of, both for the list of zaaktypes and for reading the settings
     * of one of them. The checks run from cheap to expensive.
     */
    private fun ZaakType.isOfferedForZaakCreation(loggedInUser: LoggedInUser, startrollen: Lazy<Set<String>>) =
        !concept &&
            isNuGeldig() &&
            loggedInUser.mayStartZaakOfZaaktype(omschrijving, startrollen.value) &&
            isValidForZaakCreation()

    /**
     * The engine check of the zaaktype can call Open Zaak several times, so it only runs when the ZAC configuration
     * of the zaaktype is valid.
     */
    private fun ZaakType.isValidForZaakCreation() =
        zaaktypeConfigurationService.findConfiguration(url.extractUuid())?.let {
            it.isValidForZaakCreation() && zaakProcessService.isZaaktypeReady(it, url)
        } ?: false
}
