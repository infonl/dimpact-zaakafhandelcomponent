/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { HttpTestingController } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatCardHarness } from "@angular/material/card/testing";
import { MatChipHarness } from "@angular/material/chips/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { sleep } from "../../../../setupJest";
import { GeneratedType } from "../utils/generated-types";
import { VersionComponent, VersionLayout } from "./version.component";

const BUILD_INFORMATIE_URL = "/rest/health-check/build-informatie";

describe(VersionComponent.name, () => {
  let fixture: ComponentFixture<VersionComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const buildInformatie: GeneratedType<"RestBuildInformation"> = {
    versienummer: "1.2.3",
    buildId: "build-42",
    buildDatumTijd: "2024-01-15T12:00:00",
    commit: "abc123",
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        VersionComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
  });

  async function setup(
    layout: VersionLayout | undefined,
    response: GeneratedType<"RestBuildInformation"> = buildInformatie,
  ) {
    fixture = TestBed.createComponent(VersionComponent);
    if (layout) fixture.componentRef.setInput("layout", layout);
    fixture.detectChanges();
    await sleep();
    httpTestingController.expectOne(BUILD_INFORMATIE_URL).flush(response);
    await sleep();
    await sleep();
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
  }

  it("should show chip and not card in normal layout by default", async () => {
    await setup(undefined);

    await loader.getHarness(MatChipHarness);
    expect(await loader.getAllHarnesses(MatCardHarness)).toHaveLength(0);
  });

  it("should show card and not chip in verbose layout", async () => {
    await setup(VersionLayout.VERBOSE);

    await loader.getHarness(MatCardHarness);
    expect(await loader.getAllHarnesses(MatChipHarness)).toHaveLength(0);
  });

  it("shows the chip and not the card in NORMAL layout", async () => {
    await setup(VersionLayout.NORMAL);

    await loader.getHarness(MatChipHarness);
    expect(await loader.getAllHarnesses(MatCardHarness)).toHaveLength(0);
  });

  it("switches from the card back to the chip once the layout changes from VERBOSE to NORMAL", async () => {
    await setup(VersionLayout.VERBOSE);
    fixture.componentRef.setInput("layout", VersionLayout.NORMAL);
    fixture.detectChanges();

    await loader.getHarness(MatChipHarness);
    expect(await loader.getAllHarnesses(MatCardHarness)).toHaveLength(0);
  });

  it("does not read the build information again when the layout changes", async () => {
    await setup(undefined);

    fixture.componentRef.setInput("layout", VersionLayout.VERBOSE);
    fixture.detectChanges();
    await sleep();

    httpTestingController.expectNone(BUILD_INFORMATIE_URL);
  });

  it("should show version, build and commit in verbose layout when all info is present", async () => {
    await setup(VersionLayout.VERBOSE);

    const text = await (await loader.getHarness(MatCardHarness)).getText();
    expect(text).toContain("health-check.build-informatie.version");
    expect(text).toContain("health-check.build-informatie.build");
    expect(text).toContain("health-check.build-informatie.commit");
  });

  it("should hide build info in verbose layout when buildId is absent", async () => {
    await setup(VersionLayout.VERBOSE, { ...buildInformatie, buildId: null });

    const text = await (await loader.getHarness(MatCardHarness)).getText();
    expect(text).toContain("health-check.build-informatie.version");
    expect(text).not.toContain("health-check.build-informatie.build");
    expect(text).toContain("health-check.build-informatie.commit");
  });

  it("should hide commit info in verbose layout when commit is absent", async () => {
    await setup(VersionLayout.VERBOSE, { ...buildInformatie, commit: null });

    const text = await (await loader.getHarness(MatCardHarness)).getText();
    expect(text).toContain("health-check.build-informatie.version");
    expect(text).toContain("health-check.build-informatie.build");
    expect(text).not.toContain("health-check.build-informatie.commit");
  });
});
