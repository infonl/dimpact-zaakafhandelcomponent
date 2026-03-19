/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { EMPTY } from "rxjs";
import { HealthCheckService } from "../../admin/health-check.service";
import { VersionComponent, VersionLayout } from "./version.component";

describe(VersionComponent.name, () => {
  let fixture: ComponentFixture<VersionComponent>;
  let healthCheckService: HealthCheckService;
  let healthCheckServiceMock: Pick<HealthCheckService, "readBuildInformatie">;

  beforeEach(async () => {
    healthCheckServiceMock = {
      readBuildInformatie: jest.fn().mockReturnValue(EMPTY),
    };

    await TestBed.configureTestingModule({
      imports: [
        VersionComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        { provide: HealthCheckService, useValue: healthCheckServiceMock },
      ],
    }).compileComponents();

    healthCheckService = TestBed.inject(HealthCheckService);

    fixture = TestBed.createComponent(VersionComponent);
    fixture.detectChanges();
  });

  it("should show chip and not card in normal layout by default", () => {
    expect(fixture.nativeElement.querySelector("mat-chip")).toBeTruthy();
    expect(fixture.nativeElement.querySelector("mat-card")).toBeNull();
  });

  it("should show card and not chip in verbose layout", () => {
    fixture.componentRef.setInput("layout", VersionLayout.VERBOSE);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector("mat-card")).toBeTruthy();
    expect(fixture.nativeElement.querySelector("mat-chip")).toBeNull();
  });

  it("should call readBuildInformatie on init", () => {
    expect(healthCheckService.readBuildInformatie).toHaveBeenCalledTimes(1);
  });
});
