/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr.schema

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.FieldType.BOOLEAN
import nl.info.zac.solr.FieldType.LOCATION
import nl.info.zac.solr.FieldType.PDATE
import nl.info.zac.solr.FieldType.PDOUBLE
import nl.info.zac.solr.FieldType.PINT
import nl.info.zac.solr.FieldType.PLONG
import nl.info.zac.solr.FieldType.STRING
import nl.info.zac.solr.FieldType.TEXT_GENERAL_REV
import nl.info.zac.solr.FieldType.TEXT_NL
import nl.info.zac.solr.FieldType.TEXT_WS
import nl.info.zac.solr.SolrSchemaUpdate
import nl.info.zac.solr.addCopyField
import nl.info.zac.solr.addDynamicField
import nl.info.zac.solr.addField
import nl.info.zac.solr.addFieldMultiValued
import org.apache.solr.client.solrj.request.schema.SchemaRequest

class SolrSchemaV1 : SolrSchemaUpdate {
    override val versie = 1

    override val teHerindexerenZoekObjectTypes =
        setOf(ZoekObjectType.ZAAK, ZoekObjectType.TAAK, ZoekObjectType.DOCUMENT)

    override val schemaUpdates: List<SchemaRequest.Update> =
        createGenericSchema() + createZaakSchema() + createTaakSchema() + createInformatieobjectSchema()

    private fun createGenericSchema(): List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        addField("created", PDATE),
        addField("type", STRING),
        addField("timestamp", PDATE, "NOW"),
        addField("zaaktypeUuid", STRING),
        addField("zaaktypeIdentificatie", STRING),
        addField("zaaktypeOmschrijving", STRING, docValues = true),
        addField("behandelaarNaam", STRING, docValues = true),
        addField("groepNaam", STRING, docValues = true),
        addField("isToegekend", BOOLEAN, docValues = true),
        addField("startdatum", PDATE),
        addField("streefdatum", PDATE),
        addFieldMultiValued("text", TEXT_NL, indexed = true, stored = false),
        addFieldMultiValued("text_exact", TEXT_WS, indexed = true, stored = false),
        addFieldMultiValued("text_rev", TEXT_GENERAL_REV, indexed = true, stored = false),
        addDynamicField("*_coordinate", PDOUBLE, indexed = true, stored = false),
        addCopyField("id", "text", "text_exact")
    )

    private fun createZaakSchema(): List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        addField("zaak_identificatie", STRING),
        addCopyField("zaak_identificatie", "text", "text_exact"),
        addField("zaak_omschrijving", TEXT_NL),
        addCopyField("zaak_omschrijving", "text"),
        addField("zaak_toelichting", TEXT_NL),
        addCopyField("zaak_toelichting", "text"),
        addField("zaak_registratiedatum", PDATE),
        addCopyField("zaak_registratiedatum", "created"),
        addField("zaak_startdatum", PDATE),
        addCopyField("zaak_startdatum", "startdatum"),
        addField("zaak_einddatumGepland", PDATE),
        addCopyField("zaak_einddatumGepland", "streefdatum"),
        addField("zaak_einddatum", PDATE),
        addField("zaak_uiterlijkeEinddatumAfdoening", PDATE),
        addField("zaak_publicatiedatum", PDATE),
        addField("zaak_communicatiekanaal", STRING, docValues = true),
        addCopyField("zaak_communicatiekanaal", "text"),
        addField("zaak_vertrouwelijkheidaanduiding", STRING, docValues = true),
        addField("zaak_afgehandeld", BOOLEAN, docValues = true),
        addField("zaak_groepId", STRING),
        addField("zaak_groepNaam", STRING, docValues = true),
        addCopyField("zaak_groepNaam", "text", "text_exact", "groepNaam"),
        addField("zaak_behandelaarNaam", STRING, docValues = true),
        addCopyField("zaak_behandelaarNaam", "text", "text_exact", "behandelaarNaam"),
        addField("zaak_behandelaarGebruikersnaam", STRING),
        addCopyField("zaak_behandelaarGebruikersnaam", "text"),
        addField("zaak_initiatorIdentificatie", STRING),
        addCopyField("zaak_initiatorIdentificatie", "text", "text_exact"),
        addField("zaak_initiatorType", STRING, docValues = true),
        addField("zaak_locatie", LOCATION),
        addField("zaak_locatie_adres", TEXT_NL),
        addField("zaak_redenOpschorting", TEXT_NL),
        addCopyField("zaak_redenOpschorting", "text"),
        addField("zaak_redenVerlenging", TEXT_NL),
        addCopyField("zaak_redenVerlenging", "text"),
        addField("zaak_duurVerlenging", STRING, docValues = true),
        addField("zaak_zaaktypeUuid", STRING),
        addCopyField("zaak_zaaktypeUuid", "zaaktypeUuid"),
        addField("zaak_zaaktypeIdentificatie", STRING),
        addCopyField("zaak_zaaktypeIdentificatie", "text", "zaaktypeIdentificatie"),
        addField("zaak_zaaktypeOmschrijving", STRING, docValues = true),
        addCopyField("zaak_zaaktypeOmschrijving", "text", "zaaktypeOmschrijving"),
        addField("zaak_statustypeOmschrijving", STRING),
        addCopyField("zaak_statustypeOmschrijving", "text", "text_exact"),
        addField("zaak_statusDatumGezet", PDATE),
        addField("zaak_statusToelichting", TEXT_NL),
        addCopyField("zaak_statusToelichting", "text"),
        addField("zaak_statusEindstatus", BOOLEAN, docValues = true),
        addField("zaak_resultaattypeOmschrijving", STRING),
        addCopyField("zaak_resultaattypeOmschrijving", "text", "text_exact"),
        addField("zaak_resultaatToelichting", TEXT_NL),
        addCopyField("zaak_resultaatToelichting", "text"),
        addField("zaak_aantalOpenstaandeTaken", PINT),
        addFieldMultiValued("zaak_indicaties", STRING, docValues = true),
        addField("zaak_indicaties_sort", PLONG, indexed = true, stored = false, docValues = true)
    )

    private fun createTaakSchema(): List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        addField("taak_naam", STRING, docValues = true),
        addCopyField("taak_naam", "text"),
        addField("taak_toelichting", TEXT_NL),
        addCopyField("taak_toelichting", "text"),
        addField("taak_status", STRING, docValues = true),
        addCopyField("taak_status", "text", "text_exact"),
        addField("taak_zaaktypeUuid", STRING),
        addCopyField("taak_zaaktypeUuid", "zaaktypeUuid"),
        addField("taak_zaaktypeIdentificatie", STRING),
        addCopyField("taak_zaaktypeIdentificatie", "text", "zaaktypeIdentificatie"),
        addField("taak_zaaktypeOmschrijving", STRING, docValues = true),
        addCopyField("taak_zaaktypeOmschrijving", "text", "text_exact", "zaaktypeOmschrijving"),
        addField("taak_zaakUuid", STRING),
        addField("taak_zaakId", STRING),
        addCopyField("taak_zaakId", "text_exact"),
        addField("taak_creatiedatum", PDATE),
        addCopyField("taak_creatiedatum", "created", "startdatum"),
        addField("taak_toekenningsdatum", PDATE),
        addField("taak_streefdatum", PDATE),
        addCopyField("taak_streefdatum", "streefdatum"),
        addField("taak_groepId", STRING),
        addField("taak_groepNaam", STRING, docValues = true),
        addCopyField("taak_groepNaam", "text", "text_exact", "groepNaam"),
        addField("taak_behandelaarNaam", STRING, docValues = true),
        addCopyField("taak_behandelaarNaam", "text", "text_exact", "behandelaarNaam"),
        addField("taak_behandelaarGebruikersnaam", STRING),
        addCopyField("taak_behandelaarGebruikersnaam", "text"),
        addFieldMultiValued("taak_data", STRING),
        addCopyField("taak_data", "text"),
        addFieldMultiValued("taak_informatie", STRING)
    )

    private fun createInformatieobjectSchema(): List<SchemaRequest.Update> = listOf<SchemaRequest.Update>(
        addField("informatieobject_identificatie", STRING),
        addCopyField("informatieobject_identificatie", "text", "text_exact"),
        addField("informatieobject_titel", TEXT_NL),
        addCopyField("informatieobject_titel", "text"),
        addField("informatieobject_titel_sort", STRING, indexed = true, stored = false),
        addCopyField("informatieobject_titel", "informatieobject_titel_sort"),
        addField("informatieobject_beschrijving", TEXT_NL),
        addCopyField("informatieobject_beschrijving", "text"),
        addField("informatieobject_zaaktypeUuid", STRING),
        addCopyField("informatieobject_zaaktypeUuid", "zaaktypeUuid"),
        addField("informatieobject_zaaktypeIdentificatie", STRING),
        addCopyField("informatieobject_zaaktypeIdentificatie", "zaaktypeIdentificatie"),
        addField("informatieobject_zaaktypeOmschrijving", STRING, docValues = true),
        addCopyField("informatieobject_zaaktypeOmschrijving", "zaaktypeOmschrijving"),
        addField("informatieobject_zaakId", STRING),
        addCopyField("informatieobject_zaakId", "text_exact"),
        addField("informatieobject_zaakUuid", STRING),
        addCopyField("informatieobject_zaakUuid", "text_exact"),
        addField("informatieobject_zaakAfgehandeld", BOOLEAN, docValues = true),
        addField("informatieobject_zaakRelatie", STRING),
        addField("informatieobject_creatiedatum", PDATE),
        addField("informatieobject_vertrouwelijkheidaanduiding", STRING, docValues = true),
        addField("informatieobject_auteur", TEXT_NL),
        addCopyField("informatieobject_auteur", "text"),
        addField("informatieobject_auteur_sort", STRING, indexed = true, stored = false),
        addCopyField("informatieobject_auteur", "informatieobject_auteur_sort"),
        addField("informatieobject_status", STRING, docValues = true),
        addCopyField("informatieobject_status", "text"),
        addField("informatieobject_formaat", STRING, docValues = true),
        addField("informatieobject_versie", PINT),
        addField("informatieobject_registratiedatum", PDATE),
        addCopyField("informatieobject_registratiedatum", "created", "startdatum"),
        addField("informatieobject_bestandsnaam", STRING),
        addCopyField("informatieobject_bestandsnaam", "text"),
        addField("informatieobject_bestandsomvang", PLONG),
        addField("informatieobject_documentType", STRING, docValues = true),
        addCopyField("informatieobject_documentType", "text"),
        addField("informatieobject_ontvangstdatum", PDATE),
        addField("informatieobject_verzenddatum", PDATE),
        addField("informatieobject_ondertekeningDatum", PDATE),
        addField("informatieobject_ondertekeningSoort", STRING),
        addField("informatieobject_inhoudUrl", STRING),
        addField("informatieobject_vergrendeldDoorNaam", STRING, docValues = true),
        addField("informatieobject_vergrendeldDoorGebruikersnaam", STRING),
        addFieldMultiValued("informatieobject_indicaties", STRING, docValues = true),
        addField("informatieobject_indicaties_sort", PLONG, indexed = true, stored = false, docValues = true)
    )
}
