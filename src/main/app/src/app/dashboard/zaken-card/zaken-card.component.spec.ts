/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginator } from "@angular/material/paginator";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { SharedModule } from "../../shared/shared.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenService } from "../../signaleringen.service";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { ZakenCardComponent } from "./zaken-card.component";

const makeResultaat = (totaal: number, count = totaal) =>
  fromPartial<{
    resultaten: GeneratedType<"RestZaakOverzicht">[];
    totaal: number;
  }>({
    resultaten: Array.from({ length: count }, (_, i) =>
      fromPartial<GeneratedType<"RestZaakOverzicht">>({
        identificatie: `ZAAK-${i}`,
      }),
    ),
    totaal,
  });

const cardData = new DashboardCard(
  DashboardCardId.MIJN_ZAKEN_NIEUW,
  DashboardCardType.ZAKEN,
  "ZAAK_OP_NAAM",
);

describe(ZakenCardComponent.name, () => {
  let fixture: ComponentFixture<ZakenCardComponent>;
  let signaleringenService: SignaleringenService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ZakenCardComponent],
      imports: [SharedModule, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        {
          provide: WebsocketService,
          useValue: { addListener: jest.fn() },
        },
      ],
    }).compileComponents();

    signaleringenService = TestBed.inject(SignaleringenService);
    const identityService = TestBed.inject(IdentityService);
    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      fromPartial<GeneratedType<"RestUser">>({ id: "user", naam: "Test" }),
    );
  });

  function getPaginator(): MatPaginator {
    return fixture.debugElement.query(
      (el) => el.componentInstance instanceof MatPaginator,
    )!.componentInstance as MatPaginator;
  }

  it("renders paginator length from cached query data on first detect cycle", async () => {
    const params = {
      signaleringType: cardData.signaleringType,
      page: 0,
      pageSize: 5,
    };
    testQueryClient.setQueryData(
      ["aan mij toegekende zaken signaleringen", params],
      makeResultaat(17),
    );

    jest
      .spyOn(signaleringenService, "listZakenSignalering")
      .mockReturnValue(of(makeResultaat(17)));

    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(getPaginator().length).toBe(17);
  });

  it("populates the data source with rows from the query result", async () => {
    const params = {
      signaleringType: cardData.signaleringType,
      page: 0,
      pageSize: 5,
    };
    testQueryClient.setQueryData(
      ["aan mij toegekende zaken signaleringen", params],
      makeResultaat(8),
    );
    jest
      .spyOn(signaleringenService, "listZakenSignalering")
      .mockReturnValue(of(makeResultaat(8)));

    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.dataSource.data).toHaveLength(8);
  });

  it("updates pageNumber when onPageChange is called", () => {
    fixture = TestBed.createComponent(ZakenCardComponent);
    fixture.componentInstance.data = cardData;

    fixture.componentInstance.onPageChange({ pageIndex: 4 });

    expect(fixture.componentInstance.pageNumber()).toBe(4);
  });
});
