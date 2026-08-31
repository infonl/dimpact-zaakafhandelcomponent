/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakHistorieComponent } from "./zaak-historie.component";

const zaak = fromPartial<GeneratedType<"RestZaak">>({ uuid: "fakeZaakUuid" });

const historieRegel = (fields: Partial<GeneratedType<"HistoryLine">> = {}) =>
  fromPartial<GeneratedType<"HistoryLine">>({
    attribuutLabel: "fakeAttribuutLabel",
    oudeWaarde: "fakeOudeWaarde",
    nieuweWaarde: "fakeNieuweWaarde",
    toelichting: "fakeToelichting",
    datumTijd: "2026-01-01T00:00:00Z",
    door: "fakeGebruiker",
    ...fields,
  });

describe(ZaakHistorieComponent.name, () => {
  let fixture: ComponentFixture<ZaakHistorieComponent>;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup();

  beforeEach(() => {
    notifyManager.setScheduler((fn) => fn());
  });

  afterEach(() => {
    notifyManager.setScheduler(queueMicrotask);
  });

  async function setup(zaakToShow: GeneratedType<"RestZaak"> = zaak) {
    const rendered = await render(ZaakHistorieComponent, {
      inputs: { zaak: zaakToShow },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);
    return rendered;
  }

  async function respondWith(
    historie: GeneratedType<"HistoryLine">[],
    uuid = zaak.uuid,
  ) {
    httpTestingController
      .expectOne(`/rest/zaken/zaak/${uuid}/historie`)
      .flush(historie);
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  }

  function gebruikersInRowOrder() {
    return screen
      .getAllByRole("row")
      .slice(1)
      .map((row) => within(row).getAllByRole("cell")[1]?.textContent?.trim());
  }

  it("shows a loading message until the historie arrives", async () => {
    await setup();

    expect(screen.getByText("msg.loading")).toBeVisible();

    await respondWith([]);
  });

  it("shows the no-data message when the zaak has no historie", async () => {
    await setup();

    await respondWith([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("renders a row per historie regel", async () => {
    await setup();

    await respondWith([
      historieRegel({ toelichting: "eersteToelichting" }),
      historieRegel({ toelichting: "tweedeToelichting" }),
    ]);

    expect(
      screen.getByRole("row", { name: /eersteToelichting/ }),
    ).toBeVisible();
    expect(
      screen.getByRole("row", { name: /tweedeToelichting/ }),
    ).toBeVisible();
  });

  it("asks for the historie of the zaak it is given", async () => {
    await setup(
      fromPartial<GeneratedType<"RestZaak">>({ uuid: "andereZaakUuid" }),
    );

    await respondWith(
      [historieRegel({ toelichting: "andereToelichting" })],
      "andereZaakUuid",
    );

    expect(
      screen.getByRole("row", { name: /andereToelichting/ }),
    ).toBeVisible();
  });

  it("sorts the rows on the moment of the change", async () => {
    await setup();
    await respondWith([
      historieRegel({ door: "Aap", datumTijd: "2026-03-01T00:00:00Z" }),
      historieRegel({ door: "Noot", datumTijd: "2026-01-01T00:00:00Z" }),
    ]);

    await user.click(screen.getByRole("columnheader", { name: "datum" }));
    fixture.detectChanges();

    expect(gebruikersInRowOrder()).toEqual(["Noot", "Aap"]);
  });

  it("sorts the rows on the user that made the change", async () => {
    await setup();
    await respondWith([
      historieRegel({ door: "Aap", datumTijd: "2026-03-01T00:00:00Z" }),
      historieRegel({ door: "Noot", datumTijd: "2026-01-01T00:00:00Z" }),
    ]);

    await user.click(screen.getByRole("columnheader", { name: "door" }));
    fixture.detectChanges();
    expect(gebruikersInRowOrder()).toEqual(["Aap", "Noot"]);

    await user.click(screen.getByRole("columnheader", { name: "door" }));
    fixture.detectChanges();
    expect(gebruikersInRowOrder()).toEqual(["Noot", "Aap"]);
  });

  it("sorts columns without a dedicated accessor on their own value", async () => {
    await setup();
    await respondWith([historieRegel({ toelichting: "fakeToelichting" })]);

    expect(
      fixture.componentInstance["historie"].sortingDataAccessor(
        historieRegel({ toelichting: "fakeToelichting" }),
        "toelichting",
      ),
    ).toBe("fakeToelichting");
  });
});
