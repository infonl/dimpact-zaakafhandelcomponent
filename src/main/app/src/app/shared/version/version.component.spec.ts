/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatCardHarness } from "@angular/material/card/testing";
import { MatChipHarness } from "@angular/material/chips/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { EMPTY, of } from "rxjs";
import { HealthCheckService } from "../../admin/health-check.service";
import { GeneratedType } from "../utils/generated-types";
import { VersionComponent, VersionLayout } from "./version.component";

describe(VersionComponent.name, () => {
  let fixture: ComponentFixture<VersionComponent>;
  let loader: HarnessLoader;
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
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it("should show chip and not card in normal layout by default", async () => {
    await loader.getHarness(MatChipHarness);
    expect(await loader.getAllHarnesses(MatCardHarness)).toHaveLength(0);
  });

  it("should show card and not chip in verbose layout", async () => {
    fixture.componentRef.setInput("layout", VersionLayout.VERBOSE);
    fixture.detectChanges();
    await loader.getHarness(MatCardHarness);
    expect(await loader.getAllHarnesses(MatChipHarness)).toHaveLength(0);
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
      loader = TestbedHarnessEnvironment.loader(fixture);
    });

    it("should show version, build and commit in verbose layout when all info is present", async () => {
      fixture.componentRef.setInput("layout", VersionLayout.VERBOSE);
      fixture.detectChanges();

      const card = await loader.getHarness(MatCardHarness);
      const text = await card.getText();
      expect(text).toContain("healthCheck.build_informatie.version");
      expect(text).toContain("healthCheck.build_informatie.build");
      expect(text).toContain("healthCheck.build_informatie.commit");
    });

    it("should hide build info in verbose layout when buildId is absent", async () => {
      jest
        .spyOn(healthCheckService, "readBuildInformatie")
        .mockReturnValue(of({ ...buildInfo, buildId: null }));
      fixture.destroy();
      fixture = TestBed.createComponent(VersionComponent);
      fixture.componentRef.setInput("layout", VersionLayout.VERBOSE);
      fixture.detectChanges();
      loader = TestbedHarnessEnvironment.loader(fixture);

      const card = await loader.getHarness(MatCardHarness);
      const text = await card.getText();
      expect(text).toContain("healthCheck.build_informatie.version");
      expect(text).not.toContain("healthCheck.build_informatie.build");
      expect(text).toContain("healthCheck.build_informatie.commit");
    });

    it("should hide commit info in verbose layout when commit is absent", async () => {
      jest
        .spyOn(healthCheckService, "readBuildInformatie")
        .mockReturnValue(of({ ...buildInfo, commit: null }));
      fixture.destroy();
      fixture = TestBed.createComponent(VersionComponent);
      fixture.componentRef.setInput("layout", VersionLayout.VERBOSE);
      fixture.detectChanges();
      loader = TestbedHarnessEnvironment.loader(fixture);

      const card = await loader.getHarness(MatCardHarness);
      const text = await card.getText();
      expect(text).toContain("healthCheck.build_informatie.version");
      expect(text).toContain("healthCheck.build_informatie.build");
      expect(text).not.toContain("healthCheck.build_informatie.commit");
    });

    it("should render chip in normal layout when build info is loaded", async () => {
      await loader.getHarness(MatChipHarness);
    });
  });
});
