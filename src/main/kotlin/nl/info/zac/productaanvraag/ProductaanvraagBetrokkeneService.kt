/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import nl.info.client.kvk.util.validateKvKVestigingsnummer
import nl.info.client.kvk.util.validateKvkNummer
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.RolType
import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import nl.info.zac.productaanvraag.util.performAction
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ProductaanvraagBetrokkeneService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
) {
    companion object {
        private val LOG = Logger.getLogger(ProductaanvraagBetrokkeneService::class.java.name)

        private const val ROLTYPE_OMSCHRIJVING_INITIATOR = "Initiator"
        private const val ROLTYPE_OMSCHRIJVING_BEHANDELAAR = "Behandelaar"
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
            val betrokkeneAddedAsInitiator = if (it.roltypeOmschrijving == null) {
                addBetrokkenenWithGenericRole(it, initiatorBetrokkene != null, zaak)
            } else {
                addBetrokkenenWithRole(it, initiatorBetrokkene != null, zaak)
            }
            if (initiatorBetrokkene == null && betrokkeneAddedAsInitiator) {
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
        val genericRole = this.roltypeOmschrijving == null
        val rolTypeDescription = this.roltypeOmschrijving ?: this.rolOmschrijvingGeneriek.toString()
        val prefix = if (genericRole) "generic " else ""

        return this.performAction(
            onNatuurlijkPersoonIdentity = {
                if (!brpEnabled) {
                    LOG.warning {
                        "Betrokkene with ${prefix}roletype description `$rolTypeDescription` has BSN-based identity, but BRP " +
                            "is not enabled for zaak type ${zaak.zaaktype}. No betrokkene role created for zaak ${zaak.identificatie}"
                    }
                    false
                } else {
                    true
                }
            },
            onKvkIdentity = { _, _ ->
                if (!kvkEnabled) {
                    LOG.warning {
                        "Betrokkene with ${prefix}roletype description `$rolTypeDescription` has KVK-based identity, but KVK " +
                            "is not enabled for zaak type ${zaak.zaaktype}. No betrokkene role created for zaak ${zaak.identificatie}"
                    }
                    false
                } else {
                    true
                }
            },
            onNoIdentity = {
                LOG.warning {
                    "Betrokkene with ${prefix}roletype description `$rolTypeDescription` does not contain a BSN " +
                        "or KVK-number. No betrokkene role created for zaak ${zaak.identificatie}"
                }
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
                        "Multiple initiator betrokkenen found in productaanvraag for zaak ${zaak.identificatie}. " +
                            "Only the first one will be used."
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
                    "Betrokkene with generic role '${betrokkene.rolOmschrijvingGeneriek}' is not supported in the " +
                        "mapping from a productaanvraag. No role created for zaak ${zaak.identificatie}."
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
                        "Multiple initiator betrokkenen found in productaanvraag for zaak ${zaak.identificatie}. " +
                            "Only the first one will be used."
                    )
                } else {
                    addBetrokkene(betrokkene, ROLTYPE_OMSCHRIJVING_INITIATOR, zaak)
                }
                return true
            }

            ROLTYPE_OMSCHRIJVING_BEHANDELAAR -> {
                LOG.warning(
                    "Betrokkene with role 'Behandelaar' is not supported in the mapping from a productaanvraag. " +
                        "No betrokkene role created for zaak ${zaak.identificatie}."
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
            .also { logRoltypenWarnings(it, zaak, roltypeOmschrijvingGeneriek.toString(), true) }
            .firstOrNull()?.let {
                addRole(betrokkene, it, zaak, roltypeOmschrijvingGeneriek.toString(), true)
            }
    }

    private fun addBetrokkene(
        betrokkene: Betrokkene,
        roltypeOmschrijving: String,
        zaak: Zaak
    ) {
        ztcClientService.findRoltypen(zaak.zaaktype, roltypeOmschrijving)
            .also { logRoltypenWarnings(it, zaak, roltypeOmschrijving) }
            .firstOrNull()?.let { addRole(betrokkene, it, zaak, roltypeOmschrijving) }
            ?: LOG.warning(
                "Betrokkene with role '$roltypeOmschrijving' is not supported in the mapping from a " +
                    "productaanvraag. No betrokkene role created for zaak ${zaak.identificatie}."
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
                    type,
                    kvkNummer,
                    vestigingsNummer,
                    zaak.url
                )
            },
            onNoIdentity = {
                val prefix = if (genericRolType) "generic " else ""
                LOG.warning(
                    "Betrokkene with ${prefix}roletype description `$roltypeOmschrijving` does not contain a BSN " +
                        "or KVK-number. No betrokkene role created for zaak ${zaak.identificatie}"
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
        val prefix = if (generiek) "generic " else ""
        when {
            types.isEmpty() -> LOG.warning(
                "No roltypen found for zaaktype '${zaak.zaaktype}' and ${prefix}roltype description " +
                    "'$roltypeOmschrijving'. No betrokkene role created for zaak '$zaak'."
            )

            types.size > 1 -> LOG.warning(
                "Multiple ${prefix}roltypen found for zaaktype '${zaak.zaaktype}', ${prefix}roltype description " +
                    "'$roltypeOmschrijving' and zaak ${zaak.identificatie}. " +
                    "Using the first one (description: '${types.first().omschrijving}')."
            )
        }
    }

    private fun addNatuurlijkPersoonRole(rolType: RolType, bsn: String, zaak: URI) {
        zrcClientService.createRol(
            RolNatuurlijkPersoon(
                zaak,
                rolType,
                ROL_TOELICHTING,
                NatuurlijkPersoonIdentificatie().apply { this.inpBsn = bsn }
            )
        )
    }

    private fun addRechtspersoonOrVestiging(rolType: RolType, kvkNummer: String, vestigingsNummer: String?, zaakUri: URI) {
        try {
            kvkNummer.validateKvkNummer()
            vestigingsNummer?.validateKvKVestigingsnummer()
        } catch (illegalArgumentException: IllegalArgumentException) {
            LOG.warning {
                "Betrokkene with roletype '${rolType.omschrijving}' contains invalid KVK number '$kvkNummer' or vestigings number " +
                    "'$vestigingsNummer'. ${illegalArgumentException.message}. No betrokkene role created for zaak with URI '$zaakUri'."
            }
            return
        }

        zrcClientService.createRol(
            // note that niet-natuurlijk persoon roles can be used both for KVK niet-natuurlijk personen (with an RSIN)
            // and for KVK vestigingen
            RolNietNatuurlijkPersoon(
                zaakUri,
                rolType,
                ROL_TOELICHTING,
                NietNatuurlijkPersoonIdentificatie().apply {
                    this.vestigingsNummer = vestigingsNummer
                    this.kvkNummer = kvkNummer
                }
            )
        )
    }
}
