/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { NgZone, signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatProgressBarHarness } from "@angular/material/progress-bar/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { testQueryClient } from "../../../../setupJest";
import { Progress, UtilService } from "../service/util.service";
import { LoadingComponent } from "./loading.component";

describe(LoadingComponent.name, () => {
  let fixture: ComponentFixture<LoadingComponent>;
  let loader: HarnessLoader;
  let utilService: UtilService;

  const progressBarMode = async () =>
    loader.getHarness(MatProgressBarHarness).then((b) => b.getMode());

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        LoadingComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        {
          provide: UtilService,
          useValue: {
            loading: signal(false),
            progress: signal<Progress | null>(null),
          } satisfies Pick<UtilService, "loading" | "progress">,
        },
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    utilService = TestBed.inject(UtilService);

    fixture = TestBed.createComponent(LoadingComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it("should show a determinate progress bar at 100% when idle", async () => {
    const bar = await loader.getHarness(MatProgressBarHarness);
    expect(await bar.getMode()).toBe("determinate");
    expect(await bar.getValue()).toBe(100);
  });

  describe("when loading via UtilService", () => {
    beforeEach(() => {
      utilService.loading.set(true);
      fixture.detectChanges();
    });

    it("should show a query progress bar", async () => {
      expect(await progressBarMode()).toBe("query");
    });
  });

  describe("when something reports its progress", () => {
    beforeEach(() => {
      utilService.progress.set({
        percentage: 40,
        description: "msg.document.uploaden.voortgang",
      });
      fixture.detectChanges();
    });

    it("should show how far along it is", async () => {
      const bar = await loader.getHarness(MatProgressBarHarness);
      expect(await bar.getMode()).toBe("determinate");
      expect(await bar.getValue()).toBe(40);
    });

    it("should name the action it is reporting on, so the bar has an accessible name", async () => {
      const bar = await loader.getHarness(MatProgressBarHarness);
      expect(await (await bar.host()).getAttribute("aria-label")).toBe(
        "msg.document.uploaden.voortgang",
      );
    });

    it("should take precedence over the indeterminate bar of the mutation that carries it", async () => {
      utilService.loading.set(true);
      fixture.detectChanges();

      expect(await progressBarMode()).toBe("determinate");
    });
  });

  describe("when TanStack Query is active", () => {
    let ngZone: NgZone;

    beforeEach(() => {
      // Make notifyManager synchronous so cache subscribers fire immediately,
      // allowing injectIsMutating() / injectIsFetching() to update before detectChanges().
      notifyManager.setScheduler((fn) => fn());
      ngZone = TestBed.inject(NgZone);
    });
    afterEach(() =>
      notifyManager.setScheduler((fn) => Promise.resolve().then(fn)),
    );

    it("should show an indeterminate progress bar while mutating", async () => {
      const mutation = testQueryClient
        .getMutationCache()
        .build<void, Error, void, unknown>(testQueryClient, {
          mutationFn: () => new Promise<void>(() => {}),
        });
      ngZone.run(() => void mutation.execute(undefined));
      fixture.detectChanges();

      expect(await progressBarMode()).toBe("indeterminate");
    });

    it("should show a query progress bar while fetching", async () => {
      const query = testQueryClient
        .getQueryCache()
        .build<void, Error, void>(testQueryClient, {
          queryKey: ["test-fetching"],
          queryFn: () => new Promise<void>(() => {}),
        });
      // Swallow CancelledError thrown when testQueryClient.clear() cancels
      // the in-flight query in setupJest's global afterEach.
      ngZone.run(() => void query.fetch().catch(() => {}));
      fixture.detectChanges();

      expect(await progressBarMode()).toBe("query");
    });
  });
});
