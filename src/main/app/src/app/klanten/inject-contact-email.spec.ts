/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import {
  Component,
  provideZonelessChangeDetection,
  signal,
  Signal,
} from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../setupJest";
import { GeneratedType } from "../shared/utils/generated-types";
import { injectContactEmail } from "./inject-contact-email";
import { KlantenService } from "./klanten.service";

@Component({ template: "{{ contactEmail() }}", standalone: true })
class TestHostComponent {
  readonly zaak = signal<GeneratedType<"RestZaak"> | undefined>(undefined);
  readonly contactEmail: Signal<string | null> = injectContactEmail(this.zaak);
}

describe("injectContactEmail", () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let httpTestingController: HttpTestingController;

  const contactDetailsUrl = "/rest/klanten/contactdetails/person/";

  // The query resolves over several microtask + change-detection hops, so settle
  // by flushing both a few times before reading the resulting signal.
  const settle = async () => {
    for (let iteration = 0; iteration < 5; iteration++) {
      await new Promise(requestAnimationFrame);
      fixture.detectChanges();
    }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, TranslateModule.forRoot()],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        KlantenService,
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    testQueryClient.clear();
    httpTestingController.verify();
  });

  it("returns the zaak-specific email and performs no lookup when it is present", () => {
    component.zaak.set(
      fromPartial<GeneratedType<"RestZaak">>({
        zaakSpecificContactDetails: fromPartial({
          emailAddress: "contact@example.com",
        }),
        initiatorIdentificatie: fromPartial({ temporaryPersonId: "person-1" }),
      }),
    );
    fixture.detectChanges();

    expect(component.contactEmail()).toBe("contact@example.com");
    httpTestingController.expectNone((request) =>
      request.url.includes(contactDetailsUrl),
    );
  });

  it("falls back to the initiator email when there is no zaak-specific email", async () => {
    component.zaak.set(
      fromPartial<GeneratedType<"RestZaak">>({
        initiatorIdentificatie: fromPartial({ temporaryPersonId: "person-1" }),
      }),
    );
    fixture.detectChanges();

    httpTestingController
      .expectOne((request) =>
        request.url.includes(`${contactDetailsUrl}person-1`),
      )
      .flush(
        fromPartial<GeneratedType<"RestContactDetails">>({
          emailadres: "initiator@example.com",
        }),
      );
    await settle();

    expect(component.contactEmail()).toBe("initiator@example.com");
  });

  it("returns null when there is neither a zaak-specific email nor an initiator", () => {
    component.zaak.set(
      fromPartial<GeneratedType<"RestZaak">>({
        initiatorIdentificatie: undefined,
      }),
    );
    fixture.detectChanges();

    expect(component.contactEmail()).toBeNull();
    httpTestingController.expectNone((request) =>
      request.url.includes(contactDetailsUrl),
    );
  });

  it("returns null when the initiator has no contact email address", async () => {
    component.zaak.set(
      fromPartial<GeneratedType<"RestZaak">>({
        initiatorIdentificatie: fromPartial({ temporaryPersonId: "person-1" }),
      }),
    );
    fixture.detectChanges();

    httpTestingController
      .expectOne((request) =>
        request.url.includes(`${contactDetailsUrl}person-1`),
      )
      .flush(fromPartial<GeneratedType<"RestContactDetails">>({}));
    await settle();

    expect(component.contactEmail()).toBeNull();
  });

  it("returns null and logs an error when the contact details lookup fails", async () => {
    const consoleError = jest.spyOn(console, "error").mockImplementation();
    component.zaak.set(
      fromPartial<GeneratedType<"RestZaak">>({
        initiatorIdentificatie: fromPartial({ temporaryPersonId: "person-1" }),
      }),
    );
    fixture.detectChanges();

    httpTestingController
      .expectOne((request) =>
        request.url.includes(`${contactDetailsUrl}person-1`),
      )
      .flush("Not found", { status: 404, statusText: "Not Found" });
    await settle();

    expect(component.contactEmail()).toBeNull();
    expect(consoleError).toHaveBeenCalledWith(
      "Failed to resolve contact email address",
      expect.anything(),
    );

    consoleError.mockRestore();
  });
});
