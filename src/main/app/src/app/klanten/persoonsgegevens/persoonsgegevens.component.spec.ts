/*
 * SPDX-FileCopyrightText:  2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { TestBed } from "@angular/core/testing";
import { MatExpansionPanelHarness } from "@angular/material/expansion/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  queryOptions,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { randomUUID } from "crypto";
import { of, throwError } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";
import { PersoonsgegevensComponent } from "./persoonsgegevens.component";

const testPerson: GeneratedType<"RestPersoon"> = {
  temporaryPersonId: randomUUID(),
  indicaties: [],
};

const testZaak = fromPartial<GeneratedType<"RestZaak">>({
  zaaktype: { uuid: "test-zaaktype-uuid" },
  initiatorIdentificatie: {
    temporaryPersonId: "f31b38f2-d336-431f-a045-2ce4240c6c7e",
  },
  rechten: { toevoegenInitiatorPersoon: false, verwijderenInitiator: false },
});

describe(PersoonsgegevensComponent.name, () => {
  let klantenServiceMock: Partial<KlantenService>;

  beforeEach(async () => {
    klantenServiceMock = {
      readPersoon: jest.fn().mockReturnValue(of(testPerson)),
    };

    await TestBed.configureTestingModule({
      imports: [
        PersoonsgegevensComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        RouterModule.forRoot([]),
      ],
      providers: [
        { provide: KlantenService, useValue: klantenServiceMock },
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PersoonsgegevensComponent);
    fixture.componentRef.setInput("zaak", testZaak);
    fixture.detectChanges();
  });

  it("should call service method just once", () => {
    expect(klantenServiceMock.readPersoon).toHaveBeenCalledTimes(1);
  });

  it("should disable expansion panel on error", async () => {
    klantenServiceMock.readPersoon = jest
      .fn()
      .mockReturnValue(throwError(() => new Error("Person not found")));

    const fixture = TestBed.createComponent(PersoonsgegevensComponent);
    fixture.componentRef.setInput(
      "zaak",
      fromPartial<GeneratedType<"RestZaak">>({
        zaaktype: { uuid: "test-zaaktype-uuid" },
        initiatorIdentificatie: { temporaryPersonId: "invalid-id" },
        rechten: {
          toevoegenInitiatorPersoon: false,
          verwijderenInitiator: false,
        },
      }),
    );
    fixture.detectChanges();

    const testLoader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();

    const panel = await testLoader.getHarness(MatExpansionPanelHarness);
    expect(await panel.isDisabled()).toBe(true);
  });

  it("should show warning icon on error", async () => {
    klantenServiceMock.readPersoon = jest
      .fn()
      .mockReturnValue(throwError(() => new Error("Person not found")));

    const fixture = TestBed.createComponent(PersoonsgegevensComponent);
    fixture.componentRef.setInput(
      "zaak",
      fromPartial<GeneratedType<"RestZaak">>({
        zaaktype: { uuid: "test-zaaktype-uuid" },
        initiatorIdentificatie: { temporaryPersonId: "invalid-id" },
        rechten: {
          toevoegenInitiatorPersoon: false,
          verwijderenInitiator: false,
        },
      }),
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector("mat-icon")).toBeTruthy();
  });

  describe("zaakSpecificContactDetails prevails over persoon contact info", () => {
    const personWithContactInfo: GeneratedType<"RestPersoon"> = {
      temporaryPersonId: randomUUID(),
      indicaties: [],
      telefoonnummer: "0201234567",
      emailadres: "persoon@example.com",
    };

    const personQueryKey = ["test-person-with-contact"];

    beforeEach(async () => {
      notifyManager.setScheduler((fn) => fn());
      klantenServiceMock.readPersoon = jest.fn().mockReturnValue(
        queryOptions({
          queryKey: personQueryKey,
          queryFn: async () => personWithContactInfo,
        }),
      );
      await testQueryClient.prefetchQuery({
        queryKey: personQueryKey,
        queryFn: async () => personWithContactInfo,
      });
    });

    afterEach(() => {
      notifyManager.setScheduler(queueMicrotask);
    });

    async function createExpandedFixture(
      zaakSpecificContactDetails: GeneratedType<"ContactDetails">,
    ) {
      const fixture = TestBed.createComponent(PersoonsgegevensComponent);
      fixture.componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          zaakSpecificContactDetails,
        }),
      );
      fixture.detectChanges();
      await sleep();
      fixture.detectChanges();

      const loader = TestbedHarnessEnvironment.loader(fixture);
      const panel = await loader.getHarness(MatExpansionPanelHarness);
      await panel.expand();
      fixture.detectChanges();

      return fixture;
    }

    it("should show telephoneNumber from contact details instead of persoon telefoonnummer", async () => {
      const fixture = await createExpandedFixture({
        telephoneNumber: "0612345678",
        emailAddress: null,
      });

      const content = fixture.nativeElement.textContent;
      expect(content).toContain("0612345678");
      expect(content).not.toContain("0201234567");
    });

    it("should show hint when telephoneNumber comes from contact details", async () => {
      const fixture = await createExpandedFixture({
        telephoneNumber: "0612345678",
        emailAddress: null,
      });

      expect(fixture.nativeElement.querySelector(".hint")).toBeTruthy();
    });

    it("should show emailAddress from contact details instead of persoon emailadres", async () => {
      const fixture = await createExpandedFixture({
        telephoneNumber: null,
        emailAddress: "contact@example.com",
      });

      const content = fixture.nativeElement.textContent;
      expect(content).toContain("contact@example.com");
      expect(content).not.toContain("persoon@example.com");
    });

    it("should show hint when emailAddress comes from contact details", async () => {
      const fixture = await createExpandedFixture({
        telephoneNumber: null,
        emailAddress: "contact@example.com",
      });

      expect(fixture.nativeElement.querySelector(".hint")).toBeTruthy();
    });
  });
});
