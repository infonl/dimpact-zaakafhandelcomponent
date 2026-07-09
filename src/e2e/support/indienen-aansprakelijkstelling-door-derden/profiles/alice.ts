/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { robert } from "./robert";
import { DamageReport } from "./types";

export const Alice: DamageReport = {
  personalDetails: {
    firstName: "Alice",
    initials: "A",
    prefix: "den",
    lastName: "Test",
  },
  incidentDetails: {
    description:
      "Achterstallig onderhoud aan de weg heeft schade aan mijn auto aangebracht",
    date: "10-10-2024 00:00_",
    damageType: "materiële schade aan een voertuig",
    witnesses: true,
    attachments: true,
    location: {
      city: "Enschede",
      street: "teststraat",
      furtherDescription: "teststraat heeft behoorlijke gaten in de weg",
    },
    reasonForMunicipalityLiability: "Achterstallig onderhoud aan de weg",
  },
  damageDetails: {
    description: "klapband door gaten met scherpe randen in de weg",
    vehicle: {
      make: "CITROËN",
      licensePlate: "EE-RP-10",
    },
    insurance: {
      companyName: "Verzekeraar bv",
      policyNumber: "111111111",
      damageReported: true,
      insuranceType: "AllRisk",
    },
  },
  witnessDetails: [robert.personalDetails],
  documents: {
    photo: "../files/dent.jpg",
    invoice: "../files/invoice.pdf",
  },
};
