/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.kvk.util.validateKvKVestigingsnummer
import nl.info.client.kvk.util.validateKvkNummer
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.RolNatuurlijkPersoon
import nl.info.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.RolType
import nl.info.zac.log.ZacLogger
import nl.info.zac.productaanvraag.log.BetrokkeneRoleNotCreated
import nl.info.zac.productaanvraag.log.BetrokkeneSkipReason
import nl.info.zac.productaanvraag.log.MultipleRoltypenFound
import nl.info.zac.productaanvraag.log.RoltypenNotFound
import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import nl.info.zac.productaanvraag.util.performAction
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ProductaanvraagBetrokkeneService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
) {
    companion object {
        private val LOG = ZacLogger.getLogger(ProductaanvraagBetrokkeneService::class)

        private const val ROLTYPE_OMSCHRIJVING_INITIATOR = "Initiator"
        private const val ROL_TOELICHTING = "Overgenomen vanuit de product aanvraag"
    }

    /**
     * Adds all betrokkenen that are present in the provided productaanvraag to the zaak for the set
     * of provided role types, [Betrokkene.rolOmschrijvingGeneriek] or [Betrokkene.roltypeOmschrijving], but only for those
     * role types which are defined in the zaaktype of the specified zaak.
     * An exception is made for betrokkenen of role type (behandelaar)[Betrokkene.RolOmschrijvingGeneriek.BEHANDELAAR].
     * Behandelaar betrokkenen cannot be set from a productaanvraag.
     *
     * For all supported role types except for (initiator)[Betrokkene.RolOmschrijvingGeneriek.INITIATOR] there can be
     * multiple betrokkenen. Either a (BSN)[Betrokkene.inpBsn] or a (KVK vestigingsnummer)[Betrokkene.vestigingsNummer]
     * are supported as identification of the betrokkene.
     *
     * The following logic applies for adding betrokkenen:
     * - If a betrokkene only specifies a [Betrokkene.rolOmschrijvingGeneriek] field then that is used
     * - If a betrokkene only specifies a [Betrokkene.roltypeOmschrijving] field then that is used
     * - If a betrokkene specifies both a [Betrokkene.rolOmschrijvingGeneriek] and a
     * [Betrokkene.roltypeOmschrijving], then the [Betrokkene.roltypeOmschrijving] field is used,
     * because it is more specific
     *
     * @param productaanvraag the productaanvraag to add the betrokkenen from
     * @param zaak the zaak to add the betrokkenen to
     * @param brpEnabled whether BRP is enabled for the zaak
     * @param kvkEnabled whether KVK is enabled for the zaak
     * @return betrokkene added as initiator
     */
    fun addInitiatorAndBetrokkenenToZaak(
        productaanvraag: ProductaanvraagDimpact,
        zaak: Zaak,
        brpEnabled: Boolean,
        kvkEnabled: Boolean
    ): Betrokkene? {
        var initiatorBetrokkene: Betrokkene? = null
        productaanvraag.betrokkenen?.filter { it.canBeProcessed(zaak, brpEnabled, kvkEnabled) }?.forEach {
            val isBetrokkeneAddedAsInitiator = if (it.roltypeOmschrijving == null) {
                addBetrokkenenWithGenericRole(it, initiatorBetrokkene != null, zaak)
            } else {
                addBetrokkenenWithRole(it, initiatorBetrokkene != null, zaak)
            }
            if (initiatorBetrokkene == null && isBetrokkeneAddedAsInitiator) {
                initiatorBetrokkene = it
            }
        }
        return initiatorBetrokkene
    }

    private fun Betrokkene.canBeProcessed(
        zaak: Zaak,
        brpEnabled: Boolean,
        kvkEnabled: Boolean
    ): Boolean {
        val isGenericRole = this.roltypeOmschrijving == null
        val rolTypeDescription = this.roltypeOmschrijving ?: this.rolOmschrijvingGeneriek.toString()

        return this.performAction(
            onNatuurlijkPersoonIdentity = {
                if (!brpEnabled) {
                    LOG.warning(
                    BetrokkeneRoleNotCreated(
                        zaakIdentificatie = zaak.identificatie,
                        roltypeOmschrijving = rolTypeDescription,
                        isGenericRole = isGenericRole,
                        skipReason = BetrokkeneSkipReason.BRP_NOT_ENABLED
                    )
                    )
                    false
                } else {
                    true
                }
            },
            onKvkIdentity = { _, _ ->
                if (!kvkEnabled) {
                    LOG.warning(
                    BetrokkeneRoleNotCreated(
                        zaakIdentificatie = zaak.identificatie,
                        roltypeOmschrijving = rolTypeDescription,
                        isGenericRole = isGenericRole,
                        skipReason = BetrokkeneSkipReason.KVK_NOT_ENABLED
                    )
                    )
                    false
                } else {
                    true
                }
            },
            onNoIdentity = {
                LOG.warning(
                    BetrokkeneRoleNotCreated(
                        zaakIdentificatie = zaak.identificatie,
                        roltypeOmschrijving = rolTypeDescription,
                        isGenericRole = isGenericRole,
                        skipReason = BetrokkeneSkipReason.NO_BSN_OR_KVK_NUMBER
                    )
                )
                false
            }
        )
    }

    private fun addBetrokkenenWithGenericRole(
        betrokkene: Betrokkene,
        initiatorAdded: Boolean,
        zaak: Zaak
    ): Boolean {
        when (betrokkene.rolOmschrijvingGeneriek) {
            Betrokkene.RolOmschrijvingGeneriek.ADVISEUR -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.ADVISEUR, zaak)
            }

            Betrokkene.RolOmschrijvingGeneriek.BELANGHEBBENDE -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.BELANGHEBBENDE, zaak)
            }

            Betrokkene.RolOmschrijvingGeneriek.BESLISSER -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.BESLISSER, zaak)
            }

            Betrokkene.RolOmschrijvingGeneriek.INITIATOR -> {
                if (initiatorAdded) {
                    LOG.warning(
                    BetrokkeneRoleNotCreated(
                        zaakIdentificatie = zaak.identificatie,
                        roltypeOmschrijving = "initiator",
                        isGenericRole = true,
                        skipReason = BetrokkeneSkipReason.MULTIPLE_INITIATORS
                    )
                    )
                } else {
                    addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.INITIATOR, zaak)
                }
                return true
            }

            Betrokkene.RolOmschrijvingGeneriek.KLANTCONTACTER -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.KLANTCONTACTER, zaak)
            }

            Betrokkene.RolOmschrijvingGeneriek.MEDE_INITIATOR -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.MEDE_INITIATOR, zaak)
            }

            Betrokkene.RolOmschrijvingGeneriek.ZAAKCOORDINATOR -> {
                addBetrokkeneGeneriek(betrokkene, OmschrijvingGeneriekEnum.ZAAKCOORDINATOR, zaak)
            }

            else -> {
                LOG.warning(
                    BetrokkeneRoleNotCreated(
                        zaakIdentificatie = zaak.identificatie,
                        roltypeOmschrijving = betrokkene.rolOmschrijvingGeneriek?.toString(),
                        isGenericRole = true,
                        skipReason = BetrokkeneSkipReason.ROLE_NOT_SUPPORTED
                    )
                )
            }
        }
        return initiatorAdded
    }

    private fun addBetrokkenenWithRole(
        betrokkene: Betrokkene,
        initiatorAdded: Boolean,
        zaak: Zaak
    ): Boolean {
        when (betrokkene.roltypeOmschrijving) {
            ROLTYPE_OMSCHRIJVING_INITIATOR -> {
                if (initiatorAdded) {
                    LOG.warning(
                    BetrokkeneRoleNotCreated(
                        zaakIdentificatie = zaak.identificatie,
                        roltypeOmschrijving = ROLTYPE_OMSCHRIJVING_INITIATOR,
                        isGenericRole = false,
                        skipReason = BetrokkeneSkipReason.MULTIPLE_INITIATORS
                    )
                    )
                } else {
                    addBetrokkene(betrokkene, ROLTYPE_OMSCHRIJVING_INITIATOR, zaak)
                }
                return true
            }

            ZgwApiService.ROLTYPE_OMSCHRIJVING_BEHANDELAAR -> {
                LOG.warning(
                    BetrokkeneRoleNotCreated(
                        zaakIdentificatie = zaak.identificatie,
                        roltypeOmschrijving = "Behandelaar",
                        isGenericRole = false,
                        skipReason = BetrokkeneSkipReason.ROLE_NOT_SUPPORTED
                    )
                )
            }

            else -> {
                addBetrokkene(betrokkene, betrokkene.roltypeOmschrijving, zaak)
            }
        }

        return initiatorAdded
    }

    private fun addBetrokkeneGeneriek(
        betrokkene: Betrokkene,
        roltypeOmschrijvingGeneriek: OmschrijvingGeneriekEnum,
        zaak: Zaak
    ) {
        ztcClientService.findRoltypen(zaak.zaaktype, roltypeOmschrijvingGeneriek)
            .also {
                logRoltypenWarnings(
                    types = it,
                    zaak = zaak,
                    roltypeOmschrijving = roltypeOmschrijvingGeneriek.toString(),
                    generiek = true
                )
            }
            .firstOrNull()?.let {
                addRole(
                    betrokkene = betrokkene,
                    type = it,
                    zaak = zaak,
                    roltypeOmschrijving = roltypeOmschrijvingGeneriek.toString(),
                    genericRolType = true
                )
            }
    }

    private fun addBetrokkene(
        betrokkene: Betrokkene,
        roltypeOmschrijving: String,
        zaak: Zaak
    ) {
        ztcClientService.findRoltypen(zaak.zaaktype, roltypeOmschrijving)
            .also { logRoltypenWarnings(it, zaak, roltypeOmschrijving) }
            .firstOrNull()?.let {
                addRole(betrokkene = betrokkene, type = it, zaak = zaak, roltypeOmschrijving = roltypeOmschrijving)
            }
            ?: LOG.warning(
                BetrokkeneRoleNotCreated(
                        zaakIdentificatie = zaak.identificatie,
                        roltypeOmschrijving = roltypeOmschrijving,
                        isGenericRole = false,
                        skipReason = BetrokkeneSkipReason.ROLE_NOT_SUPPORTED
                    )
            )
    }

    private fun addRole(
        betrokkene: Betrokkene,
        type: RolType,
        zaak: Zaak,
        roltypeOmschrijving: String,
        genericRolType: Boolean = false
    ) {
        betrokkene.performAction(
            onNatuurlijkPersoonIdentity = { addNatuurlijkPersoonRole(type, it, zaak.url) },
            onKvkIdentity = { kvkNummer, vestigingsNummer ->
                addRechtspersoonOrVestiging(
                    rolType = type,
                    kvkNummer = kvkNummer,
                    vestigingsNummer = vestigingsNummer,
                    zaakUri = zaak.url
                )
            },
            onNoIdentity = {
                LOG.warning(
                    BetrokkeneRoleNotCreated(
                        zaakIdentificatie = zaak.identificatie,
                        roltypeOmschrijving = roltypeOmschrijving,
                        isGenericRole = genericRolType,
                        skipReason = BetrokkeneSkipReason.NO_BSN_OR_KVK_NUMBER
                    )
                )
            }
        )
    }

    private fun logRoltypenWarnings(
        types: List<RolType>,
        zaak: Zaak,
        roltypeOmschrijving: String?,
        generiek: Boolean = false
    ) {
        when {
            types.isEmpty() -> LOG.warning(
                RoltypenNotFound(
                    zaaktypeUri = zaak.zaaktype,
                    roltypeOmschrijving = roltypeOmschrijving,
                    isGenericRole = generiek,
                    zaakIdentificatie = zaak.identificatie
                )
            )

            types.size > 1 -> LOG.warning(
                MultipleRoltypenFound(
                    zaaktypeUri = zaak.zaaktype,
                    roltypeOmschrijving = roltypeOmschrijving,
                    isGenericRole = generiek,
                    zaakIdentificatie = zaak.identificatie,
                    usedRoltypeOmschrijving = types.first().omschrijving
                )
            )
        }
    }

    private fun addNatuurlijkPersoonRole(rolType: RolType, bsn: String, zaak: URI) {
        zrcClientService.createRol(
            RolNatuurlijkPersoon(
                zaak = zaak,
                roltype = rolType,
                roltoelichting = ROL_TOELICHTING,
                betrokkeneIdentificatie = NatuurlijkPersoonIdentificatie().apply { this.inpBsn = bsn }
            )
        )
    }

    private fun addRechtspersoonOrVestiging(rolType: RolType, kvkNummer: String, vestigingsNummer: String?, zaakUri: URI) {
        try {
            kvkNummer.validateKvkNummer()
            vestigingsNummer?.validateKvKVestigingsnummer()
        } catch (illegalArgumentException: IllegalArgumentException) {
            LOG.warning(
                BetrokkeneRoleNotCreated(
                    zaakIdentificatie = null,
                    roltypeOmschrijving = rolType.omschrijving,
                    isGenericRole = false,
                    skipReason = BetrokkeneSkipReason.INVALID_KVK_NUMBER
                ),
                illegalArgumentException
            )
            return
        }

        zrcClientService.createRol(
            // note that niet-natuurlijk persoon roles can be used both for KVK niet-natuurlijk personen (with an RSIN)
            // and for KVK vestigingen
            RolNietNatuurlijkPersoon(
                zaak = zaakUri,
                roltype = rolType,
                roltoelichting = ROL_TOELICHTING,
                betrokkeneIdentificatie = NietNatuurlijkPersoonIdentificatie().apply {
                    this.vestigingsNummer = vestigingsNummer
                    this.kvkNummer = kvkNummer
                }
            )
        )
    }
}
