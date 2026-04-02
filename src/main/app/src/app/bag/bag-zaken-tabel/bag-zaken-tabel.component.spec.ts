/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { BagZakenTabelComponent } from "./bag-zaken-tabel.component";

const makeZoekResultaat = (
  fields: Partial<ZoekResultaat<ZaakZoekObject>> = {},
): ZoekResultaat<ZaakZoekObject> =>
  ({
    totaal: 0,
    resultaten: [],
    filters: {},
    ...fields,
  }) as Partial<ZoekResultaat<ZaakZoekObject>> as unknown as ZoekResultaat<ZaakZoekObject>;

describe(BagZakenTabelComponent.name, () => {
  let component: BagZakenTabelComponent;
  let fixture: ComponentFixture<BagZakenTabelComponent>;
  let zoekenService: ZoekenService;

  const mockPaginator = {
    pageIndex: 0,
    pageSize: 10,
    length: 0,
    page: { subscribe: jest.fn() },
  } as unknown as MatPaginator;

  const mockSort = {
    direction: "asc",
    active: "ZAAK_IDENTIFICATIE",
    sortChange: { subscribe: jest.fn() },
  } as unknown as MatSort;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BagZakenTabelComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();

    zoekenService = TestBed.inject(ZoekenService);
    jest
      .spyOn(zoekenService, "list")
      .mockReturnValue(of(makeZoekResultaat()) as ReturnType<ZoekenService["list"]>);

    fixture = TestBed.createComponent(BagZakenTabelComponent);
    component = fixture.componentInstance;
    component.BagObjectIdentificatie = "0363010000000001";

    // Set up ViewChild refs before detectChanges to prevent ngAfterViewInit
    // from crashing on null paginator/sort references
    component["paginator"] = mockPaginator;
    component["sort"] = mockSort;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("ngOnInit sets zoekParameters.type to ZAAK and sets ZAAK_BAGOBJECTEN", () => {
    component.ngOnInit();

    expect(component["zoekParameters"].type).toBe("ZAAK");
    expect(component["zoekParameters"].zoeken?.ZAAK_BAGOBJECTEN).toBe(
      "0363010000000001",
    );
  });

  it("inclusiefAfgerondeZaken FormControl starts as false", () => {
    expect(component["inclusiefAfgerondeZaken"].value).toBe(false);
  });

  it("filtersChanged resets paginator.pageIndex to 0 and emits filterChange", () => {
    mockPaginator.pageIndex = 5;
    const emitted: unknown[] = [];
    component.filterChange.subscribe(() => emitted.push(true));

    component["filtersChanged"]();

    expect(mockPaginator.pageIndex).toBe(0);
    expect(emitted).toHaveLength(1);
  });

  it("ngOnChanges calls filtersChanged when init is true", () => {
    component["init"] = true;
    const filtersChangedSpy = jest.spyOn(
      component as unknown as { filtersChanged(): void },
      "filtersChanged",
    );

    component.ngOnChanges();

    expect(filtersChangedSpy).toHaveBeenCalledTimes(1);
  });

  it("ngOnChanges does NOT call filtersChanged when init is false", () => {
    component["init"] = false;
    const filtersChangedSpy = jest.spyOn(
      component as unknown as { filtersChanged(): void },
      "filtersChanged",
    );

    component.ngOnChanges();

    expect(filtersChangedSpy).not.toHaveBeenCalled();
  });
});
