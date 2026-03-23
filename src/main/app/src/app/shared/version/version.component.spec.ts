/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { EMPTY, of } from "rxjs";
import { HealthCheckService } from "../../admin/health-check.service";
import { GeneratedType } from "../utils/generated-types";
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

  describe("with build information", () => {
    const buildInfo: GeneratedType<"RESTBuildInformation"> = {
      versienummer: "1.2.3",
      buildId: "build-42",
      buildDatumTijd: "2024-01-15T12:00:00",
      commit: "abc123",
    };

    beforeEach(() => {
      jest
        .spyOn(healthCheckService, "readBuildInformatie")
        .mockReturnValue(of(buildInfo));
      fixture.destroy();
      fixture = TestBed.createComponent(VersionComponent);
      fixture.detectChanges();
    });

    it("should show build and commit paragraphs in verbose layout when all info is present", () => {
      fixture.componentRef.setInput("layout", VersionLayout.VERBOSE);
      fixture.detectChanges();

      const paragraphs =
        fixture.nativeElement.querySelectorAll("mat-card-content p");
      expect(paragraphs.length).toBe(3); // version + build + commit
    });

    it("should hide build paragraph in verbose layout when buildId is absent", () => {
      jest
        .spyOn(healthCheckService, "readBuildInformatie")
        .mockReturnValue(of({ ...buildInfo, buildId: null }));
      fixture.destroy();
      fixture = TestBed.createComponent(VersionComponent);
      fixture.componentRef.setInput("layout", VersionLayout.VERBOSE);
      fixture.detectChanges();

      const paragraphs =
        fixture.nativeElement.querySelectorAll("mat-card-content p");
      expect(paragraphs.length).toBe(2); // version + commit only
    });

    it("should hide commit paragraph in verbose layout when commit is absent", () => {
      jest
        .spyOn(healthCheckService, "readBuildInformatie")
        .mockReturnValue(of({ ...buildInfo, commit: null }));
      fixture.destroy();
      fixture = TestBed.createComponent(VersionComponent);
      fixture.componentRef.setInput("layout", VersionLayout.VERBOSE);
      fixture.detectChanges();

      const paragraphs =
        fixture.nativeElement.querySelectorAll("mat-card-content p");
      expect(paragraphs.length).toBe(2); // version + build only
    });

    it("should render chip in normal layout when build info is loaded", () => {
      expect(fixture.nativeElement.querySelector("mat-chip")).toBeTruthy();
    });
  });
});
