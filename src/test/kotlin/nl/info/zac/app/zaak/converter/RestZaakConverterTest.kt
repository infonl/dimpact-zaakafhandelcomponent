/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ONTVANGSTBEVESTIGING_VERSTUURD
import nl.info.client.klant.KlantClientService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.model.createNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createOpschorting
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.ZaakEigenschap
import nl.info.client.zgw.zrc.util.isEerderOpgeschort
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.client.zgw.zrc.util.isVerlengd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.test.org.flowable.engine.repository.createProcessDefinition
import nl.info.zac.app.identity.converter.RestGroupConverter
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.klant.model.contactdetails.ContactDetails
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.createBetrokkeneIdentificatie
import nl.info.zac.app.zaak.model.createRESTGerelateerdeZaak
import nl.info.zac.app.zaak.model.createRestBesluit
import nl.info.zac.app.zaak.model.createRestGroup
import nl.info.zac.app.zaak.model.createRestUser
import nl.info.zac.app.zaak.model.createRestZaaktype
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuration.ConfigurationService.Companion.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE
import nl.info.zac.configuration.ConfigurationService.Companion.STATUSTYPE_OMSCHRIJVING_AFGEROND
import nl.info.zac.configuration.ConfigurationService.Companion.STATUSTYPE_OMSCHRIJVING_HEROPEND
import nl.info.zac.configuration.ConfigurationService.Companion.STATUSTYPE_OMSCHRIJVING_INTAKE
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.identification.IdentificationService
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.model.ZaakIndicatie.ONTVANGSTBEVESTIGING_NIET_VERSTUURD
import java.net.URI
import java.util.EnumSet
import java.util.UUID

private data class TestCase(
    val description: String,
    val zaakdata: Map<String, Any>,
    val expectedHeeftOntvangstbevestigingVerstuurd: Boolean,
    val expectedIndicatiePresent: Boolean
)

class RestZaakConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val brcClientService = mockk<BrcClientService>()
    val zgwApiService = mockk<ZgwApiService>()
    val restZaakResultaatConverter = mockk<RestZaakResultaatConverter>()
    val restGroupConverter = mockk<RestGroupConverter>()
    val restGerelateerdeZaakConverter = mockk<RestGerelateerdeZaakConverter>()
    val restUserConverter = mockk<RestUserConverter>()
    val restBesluitConverter = mockk<RestBesluitConverter>()
    val restZaaktypeConverter = mockk<RestZaaktypeConverter>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val bpmnService = mockk<BpmnService>()
    val identificationService = mockk<IdentificationService>()
    val klantClientService = mockk<KlantClientService>()
    val restZaakConverter = RestZaakConverter(
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService,
        brcClientService = brcClientService,
        zgwApiService = zgwApiService,
        restZaakResultaatConverter = restZaakResultaatConverter,
        restGroupConverter = restGroupConverter,
        restGerelateerdeZaakConverter = restGerelateerdeZaakConverter,
        restUserConverter = restUserConverter,
        restBesluitConverter = restBesluitConverter,
        restZaaktypeConverter = restZaaktypeConverter,
        zaakVariabelenService = zaakVariabelenService,
        bpmnService = bpmnService,
        identificationService = identificationService,
        klantClientService = klantClientService,
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("A CMMN zaak with a natuurlijk persoon as initiator, a group, a behandelaar and a besluit") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid()
        val restGroup = createRestGroup()
        val besluit = createBesluit()
        val restBesluit = createRestBesluit()
        val rolMedewerker = createRolMedewerker()
        val restUser = createRestUser()
        val bsn = "fakeBsn"
        val temporaryPersonId = UUID.randomUUID()
        val rolNatuurlijkPersoon = createRolNatuurlijkPersoon(
            natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(
                bsn = bsn
            )
        )
        val betrokkeneIdentificatie = createBetrokkeneIdentificatie(
            temporaryPersonId = temporaryPersonId
        )
        val restZaakType = createRestZaaktype()
        val zaakRechten = createZaakRechten()
        val zaakdata = mapOf("fakeKey" to "fakeValue")
        val loggedInUser = createLoggedInUser()

        val roles = listOf(rolOrganisatorischeEenheid, rolMedewerker, rolNatuurlijkPersoon)

        every { zrcClientService.listRollen(zaak) } returns roles
        with(zgwApiService) {
            every { findGroepForZaak(zaak, roles) } returns rolOrganisatorischeEenheid
            every { findBehandelaarMedewerkerRoleForZaak(zaak, roles) } returns rolMedewerker
            every { findInitiatorRoleForZaak(zaak, roles) } returns rolNatuurlijkPersoon
        }
        every { zaakVariabelenService.readZaakdata(zaak.uuid) } returns zaakdata
        every { restGroupConverter.convertGroupId(rolOrganisatorischeEenheid.identificatienummer!!) } returns restGroup
        every { brcClientService.listBesluiten(zaak) } returns listOf(besluit)
        every { restBesluitConverter.convertToRestBesluit(besluit) } returns restBesluit
        every { restUserConverter.convertUserId(rolMedewerker.identificatienummer!!) } returns restUser
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns null
        every {
            identificationService.createBetrokkeneIdentificatieForInitiatorRole(rolNatuurlijkPersoon)
        } returns betrokkeneIdentificatie
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("converting a zaak to a rest zaak") {
            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)

            then("the zaak should be converted correctly") {
                with(restZaak) {
                    uuid shouldBe zaak.uuid
                    identificatie shouldBe zaak.identificatie
                    with(initiatorIdentificatie!!) {
                        this.type shouldBe IdentificatieType.BSN
                        this.temporaryPersonId shouldBe temporaryPersonId
                    }
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    this.zaaktype shouldBe zaaktype
                    isVerlengd shouldBe zaak.isVerlengd()
                    isOpgeschort shouldBe zaak.isOpgeschort()
                    eerdereOpschorting shouldBe zaak.isEerderOpgeschort()
                    indicaties shouldContainExactly EnumSet.of(ONTVANGSTBEVESTIGING_NIET_VERSTUURD)
                    zaakSpecificContactDetails shouldBe null
                }
            }

            then("only a single role list lookup and a single zaak variables lookup are performed") {
                verify(exactly = 1) { zrcClientService.listRollen(zaak) }
                verify(exactly = 1) { zaakVariabelenService.readZaakdata(zaak.uuid) }
            }

            then("the same pre-fetched roles list is reused for all role lookups") {
                verify(exactly = 1) { zgwApiService.findGroepForZaak(zaak = zaak, roles = refEq(roles)) }
                verify(exactly = 1) { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak = zaak, roles =refEq(roles)) }
                verify(exactly = 1) { zgwApiService.findInitiatorRoleForZaak(zaak = zaak, roles = refEq(roles)) }
            }
        }
    }

    given("A zaak that was closed and later reopened") {
        val status = createZaakStatus()
        status.statustoelichting = "fake status"
        val statusType = createStatusType()
            .apply {
                omschrijving = STATUSTYPE_OMSCHRIJVING_HEROPEND
            }
        val zaak = createZaak().apply {
            archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
        }
        val zaakType = createZaakType()
        val rolOrganistorischeEenheid = createRolOrganisatorischeEenheid()
        val restGroup = createRestGroup()
        val besluit = createBesluit()
        val restBesluit = createRestBesluit()
        val rolMedewerker = createRolMedewerker()
        val restUser = createRestUser()
        val rol = createRolNatuurlijkPersoon()
        val betrokkeneIdentificatie = createBetrokkeneIdentificatie()
        val restZaakType = createRestZaaktype()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()
        val zaakdata = mapOf("fakeKey" to "fakeValue")

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        with(zgwApiService) {
            every { findGroepForZaak(zaak, any()) } returns rolOrganistorischeEenheid
            every { findBehandelaarMedewerkerRoleForZaak(zaak, any()) } returns rolMedewerker
            every { findInitiatorRoleForZaak(zaak, any()) } returns rol
        }
        with(zaakVariabelenService) {
            every { readZaakdata(zaak.uuid) } returns zaakdata
        }
        every { restGroupConverter.convertGroupId(rolOrganistorischeEenheid.identificatienummer!!) } returns restGroup
        every { brcClientService.listBesluiten(zaak) } returns listOf(besluit)
        every { restBesluitConverter.convertToRestBesluit(besluit) } returns restBesluit
        every { restUserConverter.convertUserId(rolMedewerker.identificatienummer!!) } returns restUser
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns null
        every { identificationService.createBetrokkeneIdentificatieForInitiatorRole(rol) } returns betrokkeneIdentificatie
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("converting a zaak to a rest zaak") {
            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser, status, statusType)

            then("the zaak should be converted correctly") {
                with(restZaak) {
                    uuid shouldBe zaak.uuid
                    identificatie shouldBe zaak.identificatie
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    this.zaaktype shouldBe zaaktype
                    isVerlengd shouldBe zaak.isVerlengd()
                    isOpgeschort shouldBe zaak.isOpgeschort()
                    eerdereOpschorting shouldBe zaak.isEerderOpgeschort()
                    indicaties shouldNotContain EnumSet.of(ONTVANGSTBEVESTIGING_NIET_VERSTUURD)
                    zaakSpecificContactDetails shouldBe null
                }
            }
        }
    }

    given("A zaak that was previously suspended") {
        val status = createZaakStatus()
        status.statustoelichting = "fake status"
        val statusType = createStatusType()
            .apply {
                omschrijving = STATUSTYPE_OMSCHRIJVING_AFGEROND
            }
        val opschorting = createOpschorting(eerdereOpschorting = true)
        val zaak = createZaak().apply {
            archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
            this.opschorting = opschorting
        }
        val zaakType = createZaakType()
        val rolOrganistorischeEenheid = createRolOrganisatorischeEenheid()
        val restGroup = createRestGroup()
        val besluit = createBesluit()
        val restBesluit = createRestBesluit()
        val rolMedewerker = createRolMedewerker()
        val restUser = createRestUser()
        val rol = createRolNatuurlijkPersoon()
        val betrokkeneIdentificatie = createBetrokkeneIdentificatie()
        val restZaakType = createRestZaaktype()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()
        val zaakdata = mapOf("fakeKey" to "fakeValue")

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        with(zgwApiService) {
            every { findGroepForZaak(zaak, any()) } returns rolOrganistorischeEenheid
            every { findBehandelaarMedewerkerRoleForZaak(zaak, any()) } returns rolMedewerker
            every { findInitiatorRoleForZaak(zaak, any()) } returns rol
        }
        with(zaakVariabelenService) {
            every { readZaakdata(zaak.uuid) } returns zaakdata
        }
        every { restGroupConverter.convertGroupId(rolOrganistorischeEenheid.identificatienummer!!) } returns restGroup
        every { brcClientService.listBesluiten(zaak) } returns listOf(besluit)
        every { restBesluitConverter.convertToRestBesluit(besluit) } returns restBesluit
        every { restUserConverter.convertUserId(rolMedewerker.identificatienummer!!) } returns restUser
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns null
        every { identificationService.createBetrokkeneIdentificatieForInitiatorRole(rol) } returns betrokkeneIdentificatie
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("converting a zaak to a rest zaak") {
            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser, status, statusType)

            then("the zaak should be converted correctly") {
                with(restZaak) {
                    uuid shouldBe zaak.uuid
                    identificatie shouldBe zaak.identificatie
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    this.zaaktype shouldBe zaaktype
                    isVerlengd shouldBe zaak.isVerlengd()
                    isOpgeschort shouldBe zaak.isOpgeschort()
                    eerdereOpschorting shouldBe true
                    indicaties shouldNotContain EnumSet.of(ONTVANGSTBEVESTIGING_NIET_VERSTUURD)
                    zaakSpecificContactDetails shouldBe null
                }
            }
        }
    }

    given("A BPMN process-driven zaak with a process definition") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val processDefinition = createProcessDefinition(
            key = "fakeProcessKey",
            name = "fakeProcessName",
            version = 3
        )
        val restZaakType = createRestZaaktype()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()
        val zaakdata = mapOf("fakeKey" to "fakeValue")

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        with(zgwApiService) {
            every { findGroepForZaak(zaak, any()) } returns null
            every { findBehandelaarMedewerkerRoleForZaak(zaak, any()) } returns null
            every { findInitiatorRoleForZaak(zaak, any()) } returns null
        }
        every { zaakVariabelenService.readZaakdata(zaak.uuid) } returns zaakdata
        every { brcClientService.listBesluiten(zaak) } returns emptyList()
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns processDefinition
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("converting the zaak to a rest zaak") {
            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)

            then("bpmnProcessDefinition is populated with the process definition key, name, and version") {
                with(restZaak.bpmnProcessDefinition!!) {
                    processDefinitionKey shouldBe "fakeProcessKey"
                    processDefinitionName shouldBe "fakeProcessName"
                    processDefinitionVersion shouldBe 3
                }
            }
        }
    }

    given("A BPMN process-driven zaak where the confirmation of receipt has not been sent") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val processDefinition = createProcessDefinition(
            key = "fakeProcessKey",
            name = "fakeProcessName",
            version = 1
        )
        val restZaakType = createRestZaaktype()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        with(zgwApiService) {
            every { findGroepForZaak(zaak, any()) } returns null
            every { findBehandelaarMedewerkerRoleForZaak(zaak, any()) } returns null
            every { findInitiatorRoleForZaak(zaak, any()) } returns null
        }
        every { zaakVariabelenService.readZaakdata(zaak.uuid) } returns emptyMap()
        every { brcClientService.listBesluiten(zaak) } returns emptyList()
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns processDefinition
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("converting the zaak to a rest zaak") {
            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)

            then("ONTVANGSTBEVESTIGING_NIET_VERSTUURD is not in indicaties because the zaak is process-driven") {
                restZaak.indicaties shouldNotContain ONTVANGSTBEVESTIGING_NIET_VERSTUURD
            }
        }
    }

    given("A non-BPMN zaak without a process definition") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val restZaakType = createRestZaaktype()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()
        val zaakdata = mapOf("fakeKey" to "fakeValue")

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        with(zgwApiService) {
            every { findGroepForZaak(zaak, any()) } returns null
            every { findBehandelaarMedewerkerRoleForZaak(zaak, any()) } returns null
            every { findInitiatorRoleForZaak(zaak, any()) } returns null
        }
        every { zaakVariabelenService.readZaakdata(zaak.uuid) } returns zaakdata
        every { brcClientService.listBesluiten(zaak) } returns emptyList()
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns null
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("converting the zaak to a rest zaak") {
            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)

            then("bpmnProcessDefinition is null") {
                restZaak.bpmnProcessDefinition.shouldBeNull()
            }
        }
    }

    given("A zaak with ontvangstbevestiging status") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val rolNatuurlijkPersoon = createRolNatuurlijkPersoon()
        val betrokkeneIdentificatie = createBetrokkeneIdentificatie()
        val restZaakType = createRestZaaktype()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        with(zgwApiService) {
            every { findGroepForZaak(zaak, any()) } returns null
            every { findBehandelaarMedewerkerRoleForZaak(zaak, any()) } returns null
            every { findInitiatorRoleForZaak(zaak, any()) } returns rolNatuurlijkPersoon
        }
        every { brcClientService.listBesluiten(zaak) } returns emptyList()
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns null
        every {
            identificationService.createBetrokkeneIdentificatieForInitiatorRole(rolNatuurlijkPersoon)
        } returns betrokkeneIdentificatie
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("converting a zaak to a rest zaak") {
            val testCases = listOf(
                TestCase(
                    description = "not sent (false)",
                    zaakdata = mapOf(VAR_ONTVANGSTBEVESTIGING_VERSTUURD to false),
                    expectedHeeftOntvangstbevestigingVerstuurd = false,
                    expectedIndicatiePresent = true
                ),
                TestCase(
                    description = "sent (true)",
                    zaakdata = mapOf(VAR_ONTVANGSTBEVESTIGING_VERSTUURD to true),
                    expectedHeeftOntvangstbevestigingVerstuurd = true,
                    expectedIndicatiePresent = false
                ),
                TestCase(
                    description = "unknown (absent)",
                    zaakdata = emptyMap(),
                    expectedHeeftOntvangstbevestigingVerstuurd = false,
                    expectedIndicatiePresent = true
                )
            )

            testCases.forEach { testCase ->
                every { zaakVariabelenService.readZaakdata(zaak.uuid) } returns testCase.zaakdata

                val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)

                then(
                    """
                    when ontvangstbevestiging is ${testCase.description},
                    heeftOntvangstbevestigingVerstuurd should be ${testCase.expectedHeeftOntvangstbevestigingVerstuurd}
                    """.trimIndent()
                ) {
                    restZaak.heeftOntvangstbevestigingVerstuurd shouldBe
                        testCase.expectedHeeftOntvangstbevestigingVerstuurd
                }

                then(
                    """
                    when ontvangstbevestiging is ${testCase.description},
                    ONTVANGSTBEVESTIGING_NIET_VERSTUURD indication should
                    ${if (testCase.expectedIndicatiePresent) "be present" else "not be present"}
                    """.trimIndent()
                ) {
                    if (testCase.expectedIndicatiePresent) {
                        restZaak.indicaties shouldContainExactly EnumSet.of(ONTVANGSTBEVESTIGING_NIET_VERSTUURD)
                    } else {
                        restZaak.indicaties shouldNotContain ONTVANGSTBEVESTIGING_NIET_VERSTUURD
                    }
                }
            }
        }
    }

    given("A zaak with an intake-related status type") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val status = createZaakStatus().apply { statustoelichting = "fake status" }
        val restZaakType = createRestZaaktype()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()
        val zaakdata = mapOf("fakeKey" to "fakeValue")

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        with(zgwApiService) {
            every { findGroepForZaak(zaak, any()) } returns null
            every { findBehandelaarMedewerkerRoleForZaak(zaak, any()) } returns null
            every { findInitiatorRoleForZaak(zaak, any()) } returns null
        }
        every { zaakVariabelenService.readZaakdata(zaak.uuid) } returns zaakdata
        every { brcClientService.listBesluiten(zaak) } returns emptyList()
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns null
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("converting a zaak with the 'Intake' status") {
            val statusType = createStatusType().apply { omschrijving = STATUSTYPE_OMSCHRIJVING_INTAKE }
            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser, status, statusType)

            then("isInIntakeFase should be true") {
                restZaak.isInIntakeFase shouldBe true
            }
        }

        `when`("converting a zaak with the 'Wacht op aanvullende informatie' status") {
            val statusType = createStatusType().apply {
                omschrijving = STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE
            }
            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser, status, statusType)

            then("isInIntakeFase should be true") {
                restZaak.isInIntakeFase shouldBe true
            }
        }
    }

    given("A zaak with gerelateerdeZaken") {
        val gerelateerdeZaakUuid = UUID.randomUUID()
        val gerelateerdeZaakItem = GerelateerdeZaak().apply {
            url = URI("https://example.com/zaak/$gerelateerdeZaakUuid")
        }
        val zaak = createZaak().apply {
            addGerelateerdeZakenItem(gerelateerdeZaakItem)
        }
        val zaakType = createZaakType()
        val loggedInUser = createLoggedInUser()
        val zaakRechten = createZaakRechten()
        val restZaakType = createRestZaaktype()
        val zaakdata = mapOf("fakeKey" to "fakeValue")
        val restGerelateerdeZaak = createRESTGerelateerdeZaak().apply {
            relatieType = RelatieType.GERELATEERD
            identificatie = "fakeIdentificatie"
        }

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        with(zgwApiService) {
            every { findGroepForZaak(zaak, any()) } returns null
            every { findBehandelaarMedewerkerRoleForZaak(zaak, any()) } returns null
            every { findInitiatorRoleForZaak(zaak, any()) } returns null
        }
        every { zaakVariabelenService.readZaakdata(zaak.uuid) } returns zaakdata
        every { brcClientService.listBesluiten(zaak) } returns emptyList()
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns null
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
        every {
            restGerelateerdeZaakConverter.convert(zaak, zaakRechten, gerelateerdeZaakItem, loggedInUser)
        } returns restGerelateerdeZaak

        `when`("converting the zaak to a rest zaak") {
            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)

            then("gerelateerdeZaken includes the converted gerelateerde zaak") {
                restZaak.gerelateerdeZaken!! shouldHaveSize 1
                restZaak.gerelateerdeZaken!![0] shouldBe restGerelateerdeZaak
            }
        }
    }

    given("A zaak with zaak-specific contact details") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val rolNatuurlijkPersoon = createRolNatuurlijkPersoon()
        val betrokkeneIdentificatie = createBetrokkeneIdentificatie()
        val restZaakType = createRestZaaktype()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()
        val zaakdata = mapOf("fakeKey" to "fakeValue")
        val contactDetails = ContactDetails(telephoneNumber = "0612345678", emailAddress = "test@example.com")

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        with(zgwApiService) {
            every { findGroepForZaak(zaak, any()) } returns null
            every { findBehandelaarMedewerkerRoleForZaak(zaak, any()) } returns null
            every { findInitiatorRoleForZaak(zaak, any()) } returns rolNatuurlijkPersoon
        }
        every { zaakVariabelenService.readZaakdata(zaak.uuid) } returns zaakdata
        every { brcClientService.listBesluiten(zaak) } returns emptyList()
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns null
        every {
            identificationService.createBetrokkeneIdentificatieForInitiatorRole(rolNatuurlijkPersoon)
        } returns betrokkeneIdentificatie
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns contactDetails
        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

        `when`("converting a zaak to a rest zaak") {
            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)

            then("the zaakSpecificContactDetails should be set on the rest zaak") {
                restZaak.zaakSpecificContactDetails shouldBe contactDetails
            }
        }
    }

    given("A zaak and its zaakeigenschappen") {
        val zaak = createZaak()
        val zaakType = createZaakType()
        val restZaakType = createRestZaaktype()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()
        val zaakdata = mapOf("fakeKey" to "fakeValue")

        every { zrcClientService.listRollen(zaak) } returns emptyList()
        with(zgwApiService) {
            every { findGroepForZaak(zaak, any()) } returns null
            every { findBehandelaarMedewerkerRoleForZaak(zaak, any()) } returns null
            every { findInitiatorRoleForZaak(zaak, any()) } returns null
        }
        every { zaakVariabelenService.readZaakdata(zaak.uuid) } returns zaakdata
        every { brcClientService.listBesluiten(zaak) } returns emptyList()
        every { restZaaktypeConverter.convert(zaakType) } returns restZaakType
        every { bpmnService.findProcessDefinitionByZaak(zaak.uuid) } returns null
        every { klantClientService.findZaakSpecificContactDetails(zaak.uuid) } returns null

        `when`("the zaakeigenschappen include ZAAK_GEAUTORISEERD with value 'true'") {
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
                ZaakEigenschap(null, UUID.randomUUID(), "ZAAK_GEAUTORISEERD").apply { waarde = "true" }
            )

            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)

            then("isZaakspecifiekGeautoriseerd should be true") {
                restZaak.isZaakspecifiekGeautoriseerd shouldBe true
            }
        }

        `when`("the zaakeigenschappen do not include ZAAK_GEAUTORISEERD") {
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)

            then("isZaakspecifiekGeautoriseerd should be false") {
                restZaak.isZaakspecifiekGeautoriseerd shouldBe false
            }
        }

        `when`("the zaakeigenschappen include ZAAK_GEAUTORISEERD with a value other than 'true'") {
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
                ZaakEigenschap(null, UUID.randomUUID(), "ZAAK_GEAUTORISEERD").apply { waarde = "false" }
            )

            val restZaak = restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)

            then("isZaakspecifiekGeautoriseerd should be false") {
                restZaak.isZaakspecifiekGeautoriseerd shouldBe false
            }
        }
    }
})
