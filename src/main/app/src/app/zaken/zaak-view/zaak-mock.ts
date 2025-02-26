/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { ZaakRechten } from "src/app/policy/model/zaak-rechten";
import { Zaak } from "../model/zaak";
import { Vertrouwelijkheidaanduiding } from "src/app/informatie-objecten/model/vertrouwelijkheidaanduiding.enum";

const zaakMock: Zaak = {
  uuid: "zaak-001",
  identificatie: "test",
  omschrijving: "test omschrijving",
  toelichting: "Some toelichting", // Required in Zaak
  zaaktype: {
    uuid: "zaaktype-001",  // Assuming Zaaktype requires a uuid property
    identificatie: "zaaktype-identificatie", // Assuming identificatie is a required field
    doel: "Doel example",
    omschrijving: "Omschrijving of Zaaktype",
    referentieproces: "Reference process",
    servicenorm: true,
    versiedatum: "2024-01-01",
    beginGeldigheid: "2024-01-01",
    eindeGeldigheid: "2024-12-31",
    nuGeldig: true,
    vertrouwelijkheidaanduiding: Vertrouwelijkheidaanduiding.openbaar, // Assuming it's a string or enum value
    opschortingMogelijk: true,
    verlengingMogelijk: true,
    verlengingstermijn: 30,
    zaaktypeRelaties: [],
    zaakafhandelparameters: null,
  },
  status: {
    naam: "open", // Assuming a status name
    toelichting: "Status toelichting",
  },
  resultaat: null,
  besluiten: [
    null,
  ],
  bronorganisatie: "Organization A",
  verantwoordelijkeOrganisatie: "Organization B",
  registratiedatum: "2024-01-01",
  startdatum: "2024-01-01",
  einddatumGepland: "2024-12-31",
  einddatum: "2024-12-31",
  uiterlijkeEinddatumAfdoening: "2024-12-31",
  publicatiedatum: "2024-01-01",
  archiefActiedatum: "2024-01-01",
  archiefNominatie: "2024-01-01",
  communicatiekanaal: "email",
  vertrouwelijkheidaanduiding: "openbaar",  // This can be an enum if needed
  zaakgeometrie: null,
  isOpgeschort: false,
  redenOpschorting: "",
  isVerlengd: false,
  isEerderOpgeschort: false,
  redenVerlenging: "",
  duurVerlenging: "",
  groep: null,
  behandelaar: {
    id: "user-001",
    naam: "John Doe",
  },
  gerelateerdeZaken: null,
  kenmerken: [],
  initiatorIdentificatieType: null,
  initiatorIdentificatie: "initiator-001",
  isOpen: true,
  isHeropend: false,
  isHoofdzaak: true,
  isDeelzaak: false,
  isOntvangstbevestigingVerstuurd: true,
  isBesluittypeAanwezig: false,
  isInIntakeFase: false,
  isProcesGestuurd: false,
  rechten: new ZaakRechten(),
  indicaties: [],
  zaakdata: {},
};

export default zaakMock;
