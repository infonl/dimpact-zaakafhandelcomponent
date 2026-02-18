/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import nl.info.client.pabc.model.createApplicationRolesResponse
import nl.info.client.pabc.model.createGetGroupsByApplicationRoleAndEntityTypeResponse
import nl.info.client.pabc.model.createPabcGroupRepresentation
import nl.info.client.pabc.model.generated.GetApplicationRolesRequest

class PabcClientServiceTest : BehaviorSpec({
    val pabcClient = mockk<PabcClient>()
    val pabcClientService = PabcClientService(pabcClient)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Getting application roles") {
        Given("A PABC application roles response for a certain PABC request") {
            val applicationName = "fakeApplicationName"
            val entityTypeId = "fakeEntityTypeId"
            val entityTypeType = "fakeEntityTypeType"
            val applicationRolesResponse = createApplicationRolesResponse(
                id = entityTypeId,
                applicationName = applicationName,
                type = entityTypeType,
            )
            val getApplicationRolesRequestSlot = slot<GetApplicationRolesRequest>()
            val functionalRoles = listOf("fakeRole1", "fakeRole2")
            every {
                pabcClient.getApplicationRolesPerEntityType(capture(getApplicationRolesRequestSlot))
            } returns applicationRolesResponse

            When("getApplicationRoles is called for a list of functional roles") {
                val result = pabcClientService.getApplicationRoles(functionalRoles)

                Then("it should invoke the client with the given roles") {
                    result shouldBe applicationRolesResponse
                    getApplicationRolesRequestSlot.captured.functionalRoleNames shouldBe functionalRoles
                    with(result.results[0]) {
                        entityType.id shouldBe entityTypeId
                        entityType.type shouldBe entityTypeType
                        applicationRoles.forEach {
                            it.application shouldBe applicationName
                        }
                    }
                }
            }
        }
    }

    Context("Getting groups by application role and zaaktype") {
        Given("A valid application role and zaaktype description") {
            val applicationRole = "validRole"
            val zaaktypeDescription = "validZaaktype"
            val groupRepresentation = createPabcGroupRepresentation("groupId", "groupName")
            every {
                pabcClient.getGroupsByApplicationRoleAndEntityType(
                    applicationName = APPLICATION_NAME_ZAC,
                    applicationRoleName = applicationRole,
                    entityTypeId = zaaktypeDescription,
                    entityType = ENTITY_TYPE_ZAAKTYPE
                )
            } returns createGetGroupsByApplicationRoleAndEntityTypeResponse(listOf(groupRepresentation))

            When("getGroupsByApplicationRoleAndZaaktype is called") {
                val result = pabcClientService.getGroupsByApplicationRoleAndZaaktype(
                    applicationRole,
                    zaaktypeDescription
                )

                Then("it should return the expected list of groups") {
                    result shouldBe listOf(groupRepresentation)
                }
            }
        }

        Given("The PABC client throws an RuntimeException when getting groups by application role and zaaktype") {
            val applicationRole = "invalidRole"
            val zaaktypeDescription = "invalidZaaktype"
            val runtimeException = RuntimeException("fakeExceptionMessage")
            every {
                pabcClient.getGroupsByApplicationRoleAndEntityType(
                    applicationName = APPLICATION_NAME_ZAC,
                    applicationRoleName = applicationRole,
                    entityTypeId = zaaktypeDescription,
                    entityType = ENTITY_TYPE_ZAAKTYPE
                )
            } throws runtimeException

            When("getGroupsByApplicationRoleAndZaaktype is called") {
                val exception = shouldThrow<RuntimeException> {
                    pabcClientService.getGroupsByApplicationRoleAndZaaktype(applicationRole, zaaktypeDescription)
                }

                Then("it should pass on the RuntimeException") {
                    exception shouldBe runtimeException
                }
            }
        }
    }

    Context("Application startup behavior") {
        Given("The PABC client is available and properly configured") {
            val applicationRolesResponse = createApplicationRolesResponse()
            every {
                pabcClient.getApplicationRolesPerEntityType(
                    GetApplicationRolesRequest().apply { functionalRoleNames = listOf("FAKE_NON_EXISTING_FUNCTIONAL_ROLE") }
                )
            } returns applicationRolesResponse

            When("onStartup is called") {
                pabcClientService.onStartup(Any())

                Then("it should call getApplicationRoles with the non-existing functional role") {
                    verify(exactly = 1) {
                        pabcClient.getApplicationRolesPerEntityType(
                            GetApplicationRolesRequest().apply { functionalRoleNames = listOf("FAKE_NON_EXISTING_FUNCTIONAL_ROLE") }
                        )
                    }
                }
            }
        }

        Given("A RuntimeException is thrown when the PABC client is called on application startup") {
            val runtimeException = RuntimeException("fakeExceptionMessage")
            every {
                pabcClient.getApplicationRolesPerEntityType(
                    GetApplicationRolesRequest().apply { functionalRoleNames = listOf("FAKE_NON_EXISTING_FUNCTIONAL_ROLE") }
                )
            } throws runtimeException

            When("onStartup is called") {
                val exception = shouldThrow<RuntimeException> {
                    pabcClientService.onStartup(Any())
                }

                Then("it should pass on the RuntimeException") {
                    exception shouldBe runtimeException
                }
            }
        }
    }
})
