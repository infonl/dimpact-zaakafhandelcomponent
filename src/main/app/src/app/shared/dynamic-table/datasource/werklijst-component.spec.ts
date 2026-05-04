/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { Component, inject } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { PageEvent } from "@angular/material/paginator";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { GebruikersvoorkeurenService } from "../../../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { GeneratedType } from "../../utils/generated-types";
import { TabelGegevens } from "../model/tabel-gegevens";
import { WerklijstComponent } from "./werklijst-component";

@Component({ template: "", standalone: true })
class ConcreteWerklijstComponent extends WerklijstComponent {
  gebruikersvoorkeurenService = inject(GebruikersvoorkeurenService);
  route = inject(ActivatedRoute);

  constructor() {
    super();
  }

  getWerklijst() {
    return "TAKEN_MIJN" as GeneratedType<"Werklijst">;
  }
}

const mockTabelGegevens: TabelGegevens = {
  aantalPerPagina: 25,
  pageSizeOptions: [10, 25, 50],
  werklijstRechten: fromPartial<GeneratedType<"RestWerklijstRechten">>({
    inbox: true,
    zakenTaken: true,
  }),
};

describe(WerklijstComponent.name, () => {
  let component: ConcreteWerklijstComponent;
  let fixture: ComponentFixture<ConcreteWerklijstComponent>;
  let gebruikersvoorkeurenService: GebruikersvoorkeurenService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ConcreteWerklijstComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { data: of({ tabelGegevens: mockTabelGegevens }) },
        },
      ],
    }).compileComponents();

    gebruikersvoorkeurenService = TestBed.inject(GebruikersvoorkeurenService);
    jest
      .spyOn(gebruikersvoorkeurenService, "updateAantalPerPagina")
      .mockReturnValue(of(undefined) as never);

    fixture = TestBed.createComponent(ConcreteWerklijstComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("reads aantalPerPagina from route tabelGegevens on init", () => {
    expect(component["aantalPerPagina"]).toBe(25);
  });

  it("reads pageSizeOptions from route tabelGegevens on init", () => {
    expect(component["pageSizeOptions"]).toEqual([10, 25, 50]);
  });

  it("reads werklijstRechten from route tabelGegevens on init", () => {
    expect(component["werklijstRechten"]).toEqual(
      mockTabelGegevens.werklijstRechten,
    );
  });

  it("calls updateAantalPerPagina and updates aantalPerPagina when page size changes", () => {
    const event: PageEvent = { pageIndex: 0, pageSize: 50, length: 100 };

    component["paginatorChanged"](event);

    expect(
      gebruikersvoorkeurenService.updateAantalPerPagina,
    ).toHaveBeenCalledWith("TAKEN_MIJN", 50);
    expect(component["aantalPerPagina"]).toBe(50);
  });

  it("does not call updateAantalPerPagina when page size is unchanged", () => {
    const event: PageEvent = { pageIndex: 1, pageSize: 25, length: 100 };

    component["paginatorChanged"](event);

    expect(
      gebruikersvoorkeurenService.updateAantalPerPagina,
    ).not.toHaveBeenCalled();
  });
});
