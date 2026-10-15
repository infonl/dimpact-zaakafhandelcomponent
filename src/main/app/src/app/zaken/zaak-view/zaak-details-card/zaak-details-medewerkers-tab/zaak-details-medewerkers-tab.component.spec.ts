/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { within } from "@testing-library/angular";
import { testQueryClient } from "../../../../../../setupJest";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZakenService } from "../../../zaken.service";
import { ZaakDetailsMedewerkersTabComponent } from "./zaak-details-medewerkers-tab.component";

describe(ZaakDetailsMedewerkersTabComponent.name, () => {
  let fixture: ComponentFixture<ZaakDetailsMedewerkersTabComponent>;
  let zakenService: ZakenService;

  const zaakUuid = "fake-zaak-uuid";

  const screen = () => within(fixture.nativeElement as HTMLElement);

  const renderTab = (
    medewerkers: GeneratedType<"RestGeautoriseerdeMedewerker">[],
  ) => {
    testQueryClient.setQueryData(
      zakenService.listGeautoriseerdeMedewerkersQuery(zaakUuid).queryKey,
      medewerkers,
    );
    fixture = TestBed.createComponent(ZaakDetailsMedewerkersTabComponent);
    fixture.componentRef.setInput("zaakUuid", zaakUuid);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    notifyManager.setScheduler((fn) => fn());

    await TestBed.configureTestingModule({
      imports: [ZaakDetailsMedewerkersTabComponent, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);
  });

  afterEach(() => {
    notifyManager.setScheduler(queueMicrotask);
    testQueryClient.clear();
  });

  it("shows each medewerker with access to the zaak together with their rol, in the order the backend sorted them", () => {
    renderTab([
      {
        id: "fake-behandelaar-id",
        naam: "Anna de Vries",
        roltype: "Behandelaar",
      },
      {
        id: "fake-medewerker-id",
        naam: "Emma Jansen",
        roltype: "Zaakspecifiek geautoriseerde medewerker",
      },
    ]);

    const rows = screen().getAllByRole("row").slice(1);
    expect(
      rows.map((row) => row.textContent?.replace(/\s+/g, " ").trim()),
    ).toEqual([
      "Anna de Vries Behandelaar",
      "Emma Jansen Zaakspecifiek geautoriseerde medewerker",
    ]);
  });

  it("labels the columns with the naam and the rol", () => {
    renderTab([]);

    expect(
      screen().getByRole("columnheader", { name: "naam" }),
    ).toBeInTheDocument();
    expect(
      screen().getByRole("columnheader", { name: "rol" }),
    ).toBeInTheDocument();
  });

  it("tells the user that no medewerkers were found when nobody holds a rol on the zaak", () => {
    renderTab([]);

    expect(
      screen().getByRole("cell", { name: "msg.geen.gegevens.gevonden" }),
    ).toBeInTheDocument();
  });

  it("shows a placeholder for a medewerker whose name is unknown", () => {
    renderTab([
      { id: "fake-behandelaar-id", naam: null, roltype: "Behandelaar" },
    ]);

    expect(screen().getByRole("cell", { name: "-" })).toBeInTheDocument();
  });
});
