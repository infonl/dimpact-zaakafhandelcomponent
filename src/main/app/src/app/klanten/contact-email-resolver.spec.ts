/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { firstValueFrom, of, throwError } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../shared/utils/generated-types";
import { ContactEmailResolver } from "./contact-email-resolver";
import { KlantenService } from "./klanten.service";

describe(ContactEmailResolver.name, () => {
  let contactEmailResolver: ContactEmailResolver;

  const getContactDetailsForPerson = jest.fn();

  beforeEach(() => {
    getContactDetailsForPerson.mockReset();
    getContactDetailsForPerson.mockReturnValue(
      of(
        fromPartial<GeneratedType<"RestContactDetails">>({
          emailadres: "initiator@example.com",
        }),
      ),
    );

    TestBed.configureTestingModule({
      providers: [
        {
          provide: KlantenService,
          useValue: fromPartial<KlantenService>({ getContactDetailsForPerson }),
        },
      ],
    });

    contactEmailResolver = TestBed.inject(ContactEmailResolver);
  });

  it("returns the zaak-specific contact email and skips the initiator lookup when it is present", async () => {
    const email = await firstValueFrom(
      contactEmailResolver.resolve(
        fromPartial<GeneratedType<"RestZaak">>({
          zaakSpecificContactDetails: fromPartial({
            emailAddress: "contact@example.com",
          }),
          initiatorIdentificatie: fromPartial({
            temporaryPersonId: "person-1",
          }),
        }),
      ),
    );

    expect(email).toBe("contact@example.com");
    expect(getContactDetailsForPerson).not.toHaveBeenCalled();
  });

  it("falls back to the initiator email when there is no zaak-specific contact email", async () => {
    const email = await firstValueFrom(
      contactEmailResolver.resolve(
        fromPartial<GeneratedType<"RestZaak">>({
          initiatorIdentificatie: fromPartial({
            temporaryPersonId: "person-1",
          }),
        }),
      ),
    );

    expect(getContactDetailsForPerson).toHaveBeenCalledWith("person-1");
    expect(email).toBe("initiator@example.com");
  });

  it("returns null when there is neither a zaak-specific contact email nor an initiator", async () => {
    const email = await firstValueFrom(
      contactEmailResolver.resolve(
        fromPartial<GeneratedType<"RestZaak">>({
          initiatorIdentificatie: undefined,
        }),
      ),
    );

    expect(email).toBeNull();
    expect(getContactDetailsForPerson).not.toHaveBeenCalled();
  });

  it("returns null when the initiator has no contact email address", async () => {
    getContactDetailsForPerson.mockReturnValue(
      of(fromPartial<GeneratedType<"RestContactDetails">>({})),
    );

    const email = await firstValueFrom(
      contactEmailResolver.resolve(
        fromPartial<GeneratedType<"RestZaak">>({
          initiatorIdentificatie: fromPartial({
            temporaryPersonId: "person-1",
          }),
        }),
      ),
    );

    expect(email).toBeNull();
  });

  it("resolves to null and logs an error when the contact details lookup fails", async () => {
    const consoleError = jest.spyOn(console, "error").mockImplementation();
    const lookupError = new Error("lookup failed");
    getContactDetailsForPerson.mockReturnValue(throwError(() => lookupError));

    const email = await firstValueFrom(
      contactEmailResolver.resolve(
        fromPartial<GeneratedType<"RestZaak">>({
          initiatorIdentificatie: fromPartial({
            temporaryPersonId: "person-1",
          }),
        }),
      ),
    );

    expect(email).toBeNull();
    expect(consoleError).toHaveBeenCalledWith(
      "Failed to resolve contact email address",
      lookupError,
    );

    consoleError.mockRestore();
  });
});
