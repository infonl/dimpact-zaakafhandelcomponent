// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { LocationService } from "../../shared/location/location.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { CaseLocationEditComponent } from "./zaak-locatie-wijzigen.component";

describe(CaseLocationEditComponent.name, () => {
  let fixture: ComponentFixture<CaseLocationEditComponent>;
  let component: CaseLocationEditComponent;

  const mockLocationService = {
    coordinateToAddress: jest.fn(),
    addressSuggest: jest.fn(),
    addressLookup: jest.fn(),
  };

  const mockZakenService = {
    updateZaakLocatie: jest.fn(),
  };

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn(),
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CaseLocationEditComponent,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: LocationService, useValue: mockLocationService },
        { provide: ZakenService, useValue: mockZakenService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseLocationEditComponent);
    component = fixture.componentInstance;

    component.zaak = fromPartial<GeneratedType<"RestZaak">>({
      uuid: "zaak-123",
      rechten: { wijzigenLocatie: true },
    });
    component.sideNav = mockSideNav;
  });

  it("should create", () => expect(component).toBeTruthy());
});
