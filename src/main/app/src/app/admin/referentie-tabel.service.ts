/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { PutBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class ReferentieTabelService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly queryClient = inject(QueryClient, { optional: true });
  private readonly utilService = inject(UtilService);

  listReferentieTabellen() {
    return this.zacHttpClient.GET("/rest/referentietabellen");
  }

  listReferentieTabellenQuery() {
    return this.zacQueryClient.GET("/rest/referentietabellen");
  }

  readReferentieTabelQuery(id: number) {
    return this.zacQueryClient.GET("/rest/referentietabellen/{id}", {
      path: { id },
    });
  }

  private invalidateReferentieTabellen() {
    return this.queryClient?.invalidateQueries({
      queryKey: this.listReferentieTabellenQuery().queryKey,
    });
  }

  private invalidateReferentieTabel(id: number) {
    return Promise.all([
      this.invalidateReferentieTabellen(),
      this.queryClient?.invalidateQueries({
        queryKey: this.readReferentieTabelQuery(id).queryKey,
      }),
    ]);
  }

  createReferentieTabelMutation() {
    return mergeMutationOptions(
      this.zacQueryClient.POST("/rest/referentietabellen"),
      { onSuccess: () => void this.invalidateReferentieTabellen() },
    );
  }

  readReferentieTabelByCode(code: string) {
    return this.zacQueryClient.GET("/rest/referentietabellen/code/{code}", {
      path: { code },
    });
  }

  /**
   * Every edit of a reference table is the same `PUT` of the whole table, so the
   * four intents below each derive their own body and name their own outcome.
   * They return a `mutationOptions`/`body` pair: the body is the whole table as
   * it should become, and the message that names the outcome is read back off
   * that body, so what the snackbar reports is what was actually sent.
   */
  private updateReferentieTabel(
    referenceTable: GeneratedType<"RestReferenceTable">,
    message: (body: PutBody<"/rest/referentietabellen/{id}">) => {
      key: string;
      args: Record<string, unknown>;
    },
  ) {
    const id = referenceTable.id ?? -1;

    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/referentietabellen/{id}", {
        path: { id },
      }),
      {
        onSuccess: (_data, body) => {
          void this.invalidateReferentieTabel(id);
          const { key, args } = message(body);
          this.utilService.openSnackbar(key, args);
        },
      },
    );
  }

  renameReferentieTabel(referenceTable: GeneratedType<"RestReferenceTable">) {
    return {
      mutationOptions: this.updateReferentieTabel(referenceTable, () => ({
        key: "msg.referentietabel.gewijzigd",
        args: { tabel: referenceTable.code },
      })),
      body: (name: string) => ({
        code: referenceTable.code,
        name,
        values: referenceTable.values ?? [],
      }),
    };
  }

  addReferentieTabelValue(referenceTable: GeneratedType<"RestReferenceTable">) {
    return {
      mutationOptions: this.updateReferentieTabel(referenceTable, (body) => ({
        key: "msg.referentietabel.waarde-toegevoegd",
        args: { value: body.values?.at(-1)?.name },
      })),
      body: (name: string) => ({
        code: referenceTable.code,
        name: referenceTable.name,
        values: [...(referenceTable.values ?? []), { name }],
      }),
    };
  }

  updateReferentieTabelValue(
    referenceTable: GeneratedType<"RestReferenceTable">,
    value: GeneratedType<"RestReferenceTableValue">,
  ) {
    return {
      mutationOptions: this.updateReferentieTabel(referenceTable, (body) => ({
        key: "msg.referentietabel.waarde-gewijzigd",
        args: {
          value: body.values?.find((current) => current.id === value.id)?.name,
        },
      })),
      body: (name: string) => ({
        code: referenceTable.code,
        name: referenceTable.name,
        values: (referenceTable.values ?? []).map((current) =>
          current.id === value.id ? { ...current, name } : current,
        ),
      }),
    };
  }

  deleteReferentieTabelValue(
    referenceTable: GeneratedType<"RestReferenceTable">,
    value: GeneratedType<"RestReferenceTableValue">,
  ) {
    return {
      mutationOptions: this.updateReferentieTabel(referenceTable, () => ({
        key: "msg.referentietabel.waarde-verwijderd",
        args: { value: value.name },
      })),
      body: {
        code: referenceTable.code,
        name: referenceTable.name,
        values: (referenceTable.values ?? []).filter(
          (current) => current.id !== value.id,
        ),
      },
    };
  }

  deleteReferentieTabel() {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE(
        "/rest/referentietabellen/{id}",
        (referenceTable: GeneratedType<"RestReferenceTable">) => ({
          parameters: { path: { id: referenceTable.id ?? -1 } },
        }),
      ),
      {
        onSuccess: (_data, referenceTable) => {
          void this.invalidateReferentieTabel(referenceTable.id ?? -1);
          this.utilService.openSnackbar("msg.tabel.verwijderen.uitgevoerd", {
            tabel: referenceTable.code,
          });
        },
      },
    );
  }

  listAfzenders() {
    return this.zacHttpClient.GET("/rest/referentietabellen/afzender");
  }

  listCommunicatiekanalen(inclusiefEFormulier?: boolean) {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/communicatiekanaal/{inclusiefEFormulier}",
      {
        path: { inclusiefEFormulier: inclusiefEFormulier ?? false },
      },
    );
  }

  listServerErrorTexts() {
    return this.zacHttpClient.GET("/rest/referentietabellen/server-error-text");
  }

  listBrpSearchValues() {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/brp-doelbinding-zoek-waarde",
    );
  }

  listBrpViewValues() {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/brp-doelbinding-raadpleeg-waarde",
    );
  }

  listBrpProcessingValues() {
    return this.zacHttpClient.GET(
      "/rest/referentietabellen/brp-verwerkingregister-waarde",
    );
  }
}
