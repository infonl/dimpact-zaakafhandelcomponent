/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldNotBeBefore
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createAutomaticEmailConfirmation
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createHumanTaskParameters
import nl.info.zac.admin.model.createHumanTaskReferentieTabel
import nl.info.zac.admin.model.createMailTemplate
import nl.info.zac.admin.model.createMailtemplateKoppelingen
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.admin.model.createReferenceTableValue
import nl.info.zac.admin.model.createZaakAfzender
import nl.info.zac.admin.model.createZaaktypeBrpParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.createZaaktypeConfigurationsUnderTest
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.net.URI
import java.util.UUID

class ZaaktypeConfigurationVersioningTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeConfigurationVersioning = ZaaktypeConfigurationVersioning(ztcClientService)

    afterEach {
        checkUnnecessaryStub()
    }

    fun resultaattypeUri(uuid: UUID) = URI("https://example.com/resultaattypen/$uuid")

    fun zaakbeeindigReden(id: Long, naam: String) = ZaakbeeindigReden().apply {
        this.id = id
        this.naam = naam
    }

    fun completionParameter(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        zaakbeeindigReden: ZaakbeeindigReden,
        resultaattypeOmschrijving: String
    ) = ZaaktypeCompletionParameters().apply {
        this.zaaktypeConfiguration = zaaktypeConfiguration
        this.zaakbeeindigReden = zaakbeeindigReden
        this.resultaattypeOmschrijving = resultaattypeOmschrijving
    }

    fun stubResultaattype(uuid: UUID, omschrijving: String) {
        every { ztcClientService.readResultaattype(resultaattypeUri(uuid)) } returns
            createResultaatType(url = resultaattypeUri(uuid), omschrijving = omschrijving)
    }

    createZaaktypeConfigurationsUnderTest().forEach { (configurationType, createZaaktypeConfiguration) ->
        context("creating the next version of a $configurationType zaaktype configuration") {
            given("a previous configuration whose resultaattypen are not the first ones of the new zaaktype") {
                val newVerlengdUuid = UUID.randomUUID()
                val newToegekendUuid = UUID.randomUUID()
                val newNietOntvankelijkUuid = UUID.randomUUID()
                val newAfgebrokenUuid = UUID.randomUUID()
                val newZaaktype = createZaakType(
                    resultTypes = listOf(newVerlengdUuid, newToegekendUuid, newNietOntvankelijkUuid, newAfgebrokenUuid)
                        .map(::resultaattypeUri)
                )
                stubResultaattype(newVerlengdUuid, "Verlengd")
                stubResultaattype(newToegekendUuid, "Toegekend")
                stubResultaattype(newNietOntvankelijkUuid, "Niet ontvankelijk")
                stubResultaattype(newAfgebrokenUuid, "Afgebroken")
                val previousZaaktypeConfiguration = createZaaktypeConfiguration("Niet ontvankelijk").apply {
                    setZaakbeeindigParameters(
                        listOf(
                            completionParameter(
                                this,
                                zaakbeeindigReden(id = 1L, naam = "Zaak is afgebroken"),
                                "Afgebroken"
                            ),
                            completionParameter(
                                this,
                                zaakbeeindigReden(id = 2L, naam = "Zaak is toegekend"),
                                "Toegekend"
                            )
                        )
                    )
                }

                `when`("the next version is created") {
                    val nextZaaktypeConfiguration =
                        zaaktypeConfigurationVersioning.createNextVersion(previousZaaktypeConfiguration, newZaaktype)

                    then("every zaakbeeindig parameter keeps its omschrijving and belongs to the next version") {
                        val zaakbeeindigParameters = nextZaaktypeConfiguration.getZaakbeeindigParameters()
                        zaakbeeindigParameters shouldHaveSize 2
                        with(zaakbeeindigParameters.first { it.zaakbeeindigReden.id == 1L }) {
                            resultaattypeOmschrijving shouldBe "Afgebroken"
                            zaaktypeConfiguration shouldBeSameInstanceAs nextZaaktypeConfiguration
                        }
                        zaakbeeindigParameters.first { it.zaakbeeindigReden.id == 2L }
                            .resultaattypeOmschrijving shouldBe "Toegekend"
                    }

                    and("the niet-ontvankelijk resultaattype keeps its omschrijving as well") {
                        nextZaaktypeConfiguration.nietOntvankelijkResultaattypeOmschrijving shouldBe "Niet ontvankelijk"
                    }
                }
            }

            given("a previous configuration with a resultaattype that no longer exists in the new zaaktype") {
                val newToegekendUuid = UUID.randomUUID()
                val newZaaktype = createZaakType(resultTypes = listOf(resultaattypeUri(newToegekendUuid)))
                stubResultaattype(newToegekendUuid, "Toegekend")
                val previousZaaktypeConfiguration = createZaaktypeConfiguration("Niet ontvankelijk").apply {
                    setZaakbeeindigParameters(
                        listOf(
                            completionParameter(
                                this,
                                zaakbeeindigReden(id = 1L, naam = "Zaak is verwijderd"),
                                "Verwijderd"
                            )
                        )
                    )
                }

                `when`("the next version is created") {
                    val nextZaaktypeConfiguration =
                        zaaktypeConfigurationVersioning.createNextVersion(previousZaaktypeConfiguration, newZaaktype)

                    then("the unmatched references are dropped") {
                        nextZaaktypeConfiguration.zaaktypeCompletionParameters.shouldNotBeNull().shouldBeEmpty()
                        nextZaaktypeConfiguration.nietOntvankelijkResultaattypeOmschrijving.shouldBeNull()
                    }
                }
            }

            given("a previous configuration with the settings that both engines have in common") {
                val newZaaktypeUuid = UUID.randomUUID()
                val newZaaktype = createZaakType(
                    uri = URI("https://example.com/zaaktypes/$newZaaktypeUuid"),
                    omschrijving = "fakeNewZaaktypeOmschrijving",
                    resultTypes = emptyList()
                )
                val previousZaaktypeConfiguration = createZaaktypeConfiguration(null).apply {
                    groepID = "fakeGroupId"
                    defaultBehandelaarId = "fakeDefaultBehandelaarId"
                    productaanvraagtype = "fakeProductaanvraagtype"
                    isSmartDocumentsEnabled = true
                    zaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(brpKoppelen = false)
                    zaaktypeBrpParameters = createZaaktypeBrpParameters(raadpleegWaarde = "fakeRaadpleegWaarde")
                }

                `when`("the next version is created") {
                    val nextZaaktypeConfiguration =
                        zaaktypeConfigurationVersioning.createNextVersion(previousZaaktypeConfiguration, newZaaktype)

                    then("it is a new, unsaved configuration of the new zaaktype version") {
                        with(nextZaaktypeConfiguration) {
                            this shouldNotBeSameInstanceAs previousZaaktypeConfiguration
                            id.shouldBeNull()
                            zaaktypeUuid shouldBe newZaaktypeUuid
                            zaaktypeOmschrijving shouldBe "fakeNewZaaktypeOmschrijving"
                            creatiedatum!! shouldNotBeBefore previousZaaktypeConfiguration.creatiedatum!!
                        }
                    }

                    and("the group, behandelaar, productaanvraagtype and SmartDocuments settings are copied") {
                        nextZaaktypeConfiguration.groepID shouldBe "fakeGroupId"
                        nextZaaktypeConfiguration.defaultBehandelaarId shouldBe "fakeDefaultBehandelaarId"
                        nextZaaktypeConfiguration.productaanvraagtype shouldBe "fakeProductaanvraagtype"
                        nextZaaktypeConfiguration.isSmartDocumentsEnabled shouldBe true
                    }

                    and("the betrokkene koppelingen and BRP doelbindingen are copied") {
                        with(nextZaaktypeConfiguration.getBetrokkeneParameters()) {
                            isBrpKoppelenEnabled shouldBe false
                            isKvkKoppelenEnabled shouldBe true
                            zaaktypeConfiguration shouldBeSameInstanceAs nextZaaktypeConfiguration
                        }
                        with(nextZaaktypeConfiguration.getBrpParameters()) {
                            raadpleegWaarde shouldBe "fakeRaadpleegWaarde"
                            zaaktypeConfiguration shouldBeSameInstanceAs nextZaaktypeConfiguration
                        }
                    }
                }
            }

            given("a previous configuration with deadline warning windows, mail settings and a confirmation email") {
                val newZaaktype = createZaakType(resultTypes = emptyList(), servicenorm = "P30D")
                val zaakAlgemeenMailTemplate = createMailTemplate()
                val previousZaaktypeConfiguration = createZaaktypeConfiguration(null).apply {
                    einddatumGeplandWaarschuwing = 3
                    uiterlijkeEinddatumAfdoeningWaarschuwing = 2
                    setMailtemplateKoppelingen(emptyList())
                    setMailtemplateKoppelingen(
                        setOf(createMailtemplateKoppelingen(zaaktypeConfiguration = this, mailTemplate = zaakAlgemeenMailTemplate))
                    )
                    setZaakAfzenders(
                        setOf(
                            createZaakAfzender(
                                zaaktypeConfiguration = this,
                                defaultMail = true,
                                mail = "afzender@example.com",
                                replyTo = "antwoord@example.com"
                            )
                        )
                    )
                    zaaktypeEmailParameters = createAutomaticEmailConfirmation(zaaktypeConfiguration = this)
                }

                `when`("the next version is created for a new zaaktype with a servicenorm") {
                    val nextZaaktypeConfiguration =
                        zaaktypeConfigurationVersioning.createNextVersion(previousZaaktypeConfiguration, newZaaktype)

                    then("both deadline warning windows are copied") {
                        nextZaaktypeConfiguration.einddatumGeplandWaarschuwing shouldBe 3
                        nextZaaktypeConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe 2
                    }

                    and("the mailtemplate koppelingen and zaakafzenders are copied") {
                        with(nextZaaktypeConfiguration.getMailtemplateKoppelingen().single()) {
                            mailTemplate shouldBeSameInstanceAs zaakAlgemeenMailTemplate
                            zaaktypeConfiguration shouldBeSameInstanceAs nextZaaktypeConfiguration
                        }
                        with(nextZaaktypeConfiguration.getZaakAfzenders().single()) {
                            isDefaultMail shouldBe true
                            mail shouldBe "afzender@example.com"
                            replyTo shouldBe "antwoord@example.com"
                            zaaktypeConfiguration shouldBeSameInstanceAs nextZaaktypeConfiguration
                        }
                    }

                    and("the confirmation email is copied") {
                        with(nextZaaktypeConfiguration.zaaktypeEmailParameters.shouldNotBeNull()) {
                            isEnabled shouldBe true
                            templateName shouldBe "fakeTemplateName"
                            emailSender shouldBe "sender@example.com"
                            emailReply shouldBe "reply@example.com"
                            zaaktypeConfiguration shouldBeSameInstanceAs nextZaaktypeConfiguration
                        }
                    }
                }
            }

            given("a previous configuration with deadline warning windows and without a confirmation email") {
                val newZaaktype = createZaakType(resultTypes = emptyList(), servicenorm = null)
                val previousZaaktypeConfiguration = createZaaktypeConfiguration(null).apply {
                    einddatumGeplandWaarschuwing = 3
                    uiterlijkeEinddatumAfdoeningWaarschuwing = 2
                    zaaktypeEmailParameters = null
                }

                `when`("the next version is created for a new zaaktype without a servicenorm") {
                    val nextZaaktypeConfiguration =
                        zaaktypeConfigurationVersioning.createNextVersion(previousZaaktypeConfiguration, newZaaktype)

                    then("the einddatum gepland warning window is cleared, because the zaak has no einddatum gepland") {
                        nextZaaktypeConfiguration.einddatumGeplandWaarschuwing.shouldBeNull()
                    }

                    and("the uiterlijke einddatum afdoening warning window is copied") {
                        nextZaaktypeConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe 2
                    }

                    and("no confirmation email is created") {
                        nextZaaktypeConfiguration.zaaktypeEmailParameters.shouldBeNull()
                    }
                }
            }

            given("a previous configuration bound to $configurationType") {
                val newZaaktype = createZaakType(resultTypes = emptyList())
                val previousZaaktypeConfiguration = createZaaktypeConfiguration(null).apply {
                    bindTo(configurationType, "fakePreviousDefinitionKey")
                }

                `when`("the next version is created") {
                    val nextZaaktypeConfiguration =
                        zaaktypeConfigurationVersioning.createNextVersion(previousZaaktypeConfiguration, newZaaktype)

                    then("it is bound to the same engine and definition") {
                        with(nextZaaktypeConfiguration.processBinding.shouldNotBeNull()) {
                            processEngine shouldBe configurationType
                            definitionKey shouldBe "fakePreviousDefinitionKey"
                            zaaktypeConfiguration shouldBeSameInstanceAs nextZaaktypeConfiguration
                        }
                        nextZaaktypeConfiguration.processBinding shouldNotBeSameInstanceAs
                            previousZaaktypeConfiguration.processBinding
                    }

                    and("only a CMMN configuration gets a CMMN extension") {
                        (nextZaaktypeConfiguration.cmmnExtension != null) shouldBe (configurationType == ProcessEngine.CMMN)
                    }
                }
            }
        }
    }

    context("creating the next version of a CMMN zaaktype configuration") {
        given("a previous configuration whose human task is coupled to the ADVIES reference table") {
            val newZaaktype = createZaakType(resultTypes = emptyList())
            val adviesReferenceTable = createReferenceTable(
                code = "ADVIES",
                name = "Advies",
                isSystemReferenceTable = true,
                values = mutableListOf(
                    createReferenceTableValue(id = 1L, name = "Positief"),
                    createReferenceTableValue(id = 2L, name = "Negatief")
                )
            )
            val previousZaaktypeConfiguration = createZaaktypeCmmnConfiguration(nietOntvankelijkResultaattypeOmschrijving = null).apply {
                getOrCreateCmmnExtension().apply {
                    intakeMail = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_AAN.name
                    afrondenMail = ZaakafhandelparametersStatusMailOption.NIET_BESCHIKBAAR.name
                    setHumanTaskParametersCollection(
                        setOf(
                            createHumanTaskParameters(
                                zaaktypeCmmnExtension = this,
                                formulierDefinitieID = "ADVIES",
                                planItemDefinitionID = "ADVIES",
                                referenceTables = listOf(
                                    createHumanTaskReferentieTabel(referenceTable = adviesReferenceTable, field = "ADVIES")
                                )
                            )
                        )
                    )
                    setUserEventListenerParametersCollection(
                        setOf(
                            ZaaktypeCmmnUsereventlistenerParameters().apply {
                                planItemDefinitionID = "fakeUserEventListenerId"
                                toelichting = "fakeToelichting"
                            }
                        )
                    )
                }
            }
            val previousHumanTaskParameters =
                previousZaaktypeConfiguration.cmmnExtension.shouldNotBeNull().getHumanTaskParametersCollection().single()
            val previousReferentieTabel = previousHumanTaskParameters.getReferentieTabellen().single()

            `when`("the next version is created") {
                val nextZaaktypeConfiguration =
                    zaaktypeConfigurationVersioning.createNextVersion(previousZaaktypeConfiguration, newZaaktype)
                val nextCmmnExtension = nextZaaktypeConfiguration.cmmnExtension.shouldNotBeNull()

                then("the intake and afronden mail options and the user event listeners are copied") {
                    nextCmmnExtension.intakeMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_AAN.name
                    nextCmmnExtension.afrondenMail shouldBe ZaakafhandelparametersStatusMailOption.NIET_BESCHIKBAAR.name
                    with(nextCmmnExtension.getUserEventListenerParametersCollection().single()) {
                        planItemDefinitionID shouldBe "fakeUserEventListenerId"
                        toelichting shouldBe "fakeToelichting"
                    }
                }

                and("the new configuration is coupled to the same reference table") {
                    with(nextCmmnExtension.getHumanTaskParametersCollection().single()) {
                        planItemDefinitionID shouldBe "ADVIES"
                        with(getReferentieTabellen().single()) {
                            veld shouldBe "ADVIES"
                            tabel shouldBeSameInstanceAs adviesReferenceTable
                        }
                    }
                }

                and("the previous configuration keeps its own coupling, so its zaken keep their advies options") {
                    previousHumanTaskParameters.getReferentieTabellen().single() shouldBeSameInstanceAs
                        previousReferentieTabel
                    previousReferentieTabel.humantask shouldBeSameInstanceAs previousHumanTaskParameters
                }

                and("the coupling of the new configuration is a new, unsaved record of its own human task") {
                    val nextHumanTaskParameters = nextCmmnExtension.getHumanTaskParametersCollection().single()
                    with(nextHumanTaskParameters.getReferentieTabellen().single()) {
                        this shouldNotBeSameInstanceAs previousReferentieTabel
                        id.shouldBeNull()
                        humantask shouldBeSameInstanceAs nextHumanTaskParameters
                    }
                }
            }
        }

        given("a previous configuration in which every property has a non-default value") {
            val identityFields = setOf("id", "zaaktypeUuid", "zaaktypeOmschrijving", "creatiedatum")
            val copiedFields = ZaaktypeConfiguration::class.java.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic || it.name in identityFields }
                .onEach { it.isAccessible = true }
            val defaultZaaktypeConfiguration = ZaaktypeConfiguration()
            fun Field.hasDefaultValue(zaaktypeConfiguration: ZaaktypeConfiguration) =
                get(zaaktypeConfiguration).let {
                    it == null || it == get(defaultZaaktypeConfiguration) || (it is Collection<*> && it.isEmpty())
                }
            val newToegekendUuid = UUID.randomUUID()
            val newNietOntvankelijkUuid = UUID.randomUUID()
            val newZaaktype = createZaakType(
                resultTypes = listOf(newToegekendUuid, newNietOntvankelijkUuid).map(::resultaattypeUri),
                servicenorm = "P30D"
            )
            stubResultaattype(newToegekendUuid, "Toegekend")
            stubResultaattype(newNietOntvankelijkUuid, "Niet ontvankelijk")
            val previousZaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                nietOntvankelijkResultaattypeOmschrijving = "Niet ontvankelijk",
                groupId = "fakeGroupId",
                defaultBehandelaarId = "fakeDefaultBehandelaarId",
                productaanvraagtype = "fakeProductaanvraagtype",
                smartDocumentsEnabled = true,
                einddatumGeplandWaarschuwing = 3
            ).apply {
                uiterlijkeEinddatumAfdoeningWaarschuwing = 2
                setZaakbeeindigParameters(
                    listOf(
                        completionParameter(
                            zaaktypeConfiguration = this,
                            zaakbeeindigReden = zaakbeeindigReden(id = 2L, naam = "Zaak is toegekend"),
                            resultaattypeOmschrijving = "Toegekend"
                        )
                    )
                )
            }

            `when`("the next version is created") {
                val nextZaaktypeConfiguration =
                    zaaktypeConfigurationVersioning.createNextVersion(previousZaaktypeConfiguration, newZaaktype)

                then("the previous configuration has no property left at its default value") {
                    copiedFields.filter { it.hasDefaultValue(previousZaaktypeConfiguration) }
                        .map { it.name }
                        .shouldBeEmpty()
                }

                and("the next version copies every property that is not an identity property") {
                    copiedFields.filter { it.hasDefaultValue(nextZaaktypeConfiguration) }
                        .map { it.name }
                        .shouldBeEmpty()
                }
            }
        }
    }
})
