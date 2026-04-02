// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { LocationService } from "../../shared/location/location.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { LocatieTonenComponent } from "./zaak-locatie-tonen.component";

describe(LocatieTonenComponent.name, () => {
  let fixture: ComponentFixture<LocatieTonenComponent>;
  let component: LocatieTonenComponent;

  const mockLocationService = {
    coordinateToAddress: jest.fn(),
    addressSuggest: jest.fn(),
    addressLookup: jest.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), LocatieTonenComponent],
      providers: [
        { provide: LocationService, useValue: mockLocationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LocatieTonenComponent);
    component = fixture.componentInstance;

    component.currentLocation = fromPartial<GeneratedType<"RestGeometry">>({
      type: "Point",
    });
  });

  it("should create", () => expect(component).toBeTruthy());
});
