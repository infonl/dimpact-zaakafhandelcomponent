/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

type PersonalDetails = {
  firstName: string;
  initials: string;
  prefix: string | null;
  lastName: string;
  postalCode?: string;
  houseNumber?: string;
};

export type DamageReport = {
  personalDetails: PersonalDetails;
  incidentDetails?: {
    description: string;
    date: string;
    damageType: string;
    witnesses: boolean;
    attachments: boolean;
    location: {
      city: string;
      street: string;
      furtherDescription: string;
    };
    reasonForMunicipalityLiability: string;
  };
  damageDetails?: {
    description: string;
    vehicle: {
      make: string;
      licensePlate: string;
    };
    insurance: {
      companyName: string;
      policyNumber: string;
      damageReported: boolean;
      insuranceType: string;
    };
  };
  witnessDetails?: PersonalDetails[];
  documents?: {
    photo?: string;
    invoice?: string;
  };
};
