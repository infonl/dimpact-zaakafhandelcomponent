/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import {
  RedenDialogComponent,
  RedenDialogData,
} from "../shared/dialog/reden-dialog/reden-dialog.component";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZaakAfbrekenDialogComponent } from "./zaak-afbreken-dialog/zaak-afbreken-dialog.component";
import { ZaakDialogService } from "./zaak-dialog.service";

const setup = () => {
  const open = jest.fn().mockReturnValue({ afterClosed: () => of(true) });

  TestBed.configureTestingModule({
    imports: [TranslateModule.forRoot()],
    providers: [
      ZaakDialogService,
      { provide: MatDialog, useValue: { open } },
    ],
  });

  return { service: TestBed.inject(ZaakDialogService), open };
};

// Reads the RedenDialogData passed to the most recent MatDialog.open call.
const openedRedenData = (open: jest.Mock): RedenDialogData =>
  open.mock.calls.at(-1)![1].data;

describe(ZaakDialogService.name, () => {
  it("heropenen opens a reden dialog with title, maxlength and callback", () => {
    const { service, open } = setup();
    const callback = jest.fn();

    service.heropenen(callback);

    expect(open).toHaveBeenCalledWith(RedenDialogComponent, expect.anything());
    const data = openedRedenData(open);
    expect(data.titleKey).toBe("actie.zaak.heropenen");
    expect(data.maxlength).toBe(100);
    expect(data.callback).toBe(callback);
  });

  it("hervatten opens a textarea-less reden dialog with a translated melding", () => {
    const { service, open } = setup();

    service.hervatten({ duur: 3, verwachteDuur: 5 }, jest.fn());

    const data = openedRedenData(open);
    expect(data.titleKey).toBe("actie.zaak.hervatten");
    expect(data.maxlength).toBe(200);
    expect(data.melding).toBe("msg.zaak.hervatten");
  });

  it("wijzigInitiator opens a multiline reden dialog", () => {
    const { service, open } = setup();

    service.wijzigInitiator("Jan Jansen", jest.fn());

    const data = openedRedenData(open);
    expect(data.titleKey).toBe("actie.initiator.wijzigen");
    expect(data.multiline).toBe(true);
  });

  it("ontkoppelBetrokkene opens a multiline reden dialog", () => {
    const { service, open } = setup();

    service.ontkoppelBetrokkene("Initiator Jan", jest.fn());

    const data = openedRedenData(open);
    expect(data.titleKey).toBe("actie.betrokkene.ontkoppelen");
    expect(data.multiline).toBe(true);
  });

  it("verwijderBagObject opens a reden dialog with uitleg and maxlength 80", () => {
    const { service, open } = setup();

    service.verwijderBagObject("Straat 1", jest.fn());

    const data = openedRedenData(open);
    expect(data.titleKey).toBe("actie.bagObject.ontkoppelen");
    expect(data.maxlength).toBe(80);
    expect(data.uitleg).toBe("msg.bagObject.ontkoppelen.bevestigen");
  });

  it("afbreken opens the dedicated afbreken dialog with options and callback", () => {
    const { service, open } = setup();
    const options = of([
      fromPartial<GeneratedType<"RestZaakbeeindigReden">>({ id: "1" }),
    ]);
    const callback = jest.fn();

    service.afbreken(options, callback);

    expect(open).toHaveBeenCalledWith(
      ZaakAfbrekenDialogComponent,
      expect.anything(),
    );
    const data = open.mock.calls.at(-1)![1].data;
    expect(data.options).toBe(options);
    expect(data.callback).toBe(callback);
  });
});
