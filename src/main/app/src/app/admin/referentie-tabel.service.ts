/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
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
   * They return the `mutationOptions`/`body` pair that {@link injectServiceMutation}
   * takes, with the value the user typed as the mutation's variables.
   */
  private updateReferentieTabel(
    referenceTable: GeneratedType<"RestReferenceTable">,
    message: { key: string; args: Record<string, unknown> },
  ) {
    const id = referenceTable.id ?? -1;

    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/referentietabellen/{id}", {
        path: { id },
      }),
      {
        onSuccess: () => {
          void this.invalidateReferentieTabel(id);
          this.utilService.openSnackbar(message.key, message.args);
        },
      },
    );
  }

  renameReferentieTabel(referenceTable: GeneratedType<"RestReferenceTable">) {
    return {
      mutationOptions: () =>
        this.updateReferentieTabel(referenceTable, {
          key: "msg.referentietabel.gewijzigd",
          args: { tabel: referenceTable.code },
        }),
      body: (name: string) => ({
        code: referenceTable.code,
        name,
        values: referenceTable.values ?? [],
      }),
    };
  }

  addReferentieTabelValue(referenceTable: GeneratedType<"RestReferenceTable">) {
    return {
      mutationOptions: (name: string) =>
        this.updateReferentieTabel(referenceTable, {
          key: "msg.referentietabel.waarde-toegevoegd",
          args: { value: name },
        }),
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
      mutationOptions: (name: string) =>
        this.updateReferentieTabel(referenceTable, {
          key: "msg.referentietabel.waarde-gewijzigd",
          args: { value: name },
        }),
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
      mutationOptions: this.updateReferentieTabel(referenceTable, {
        key: "msg.referentietabel.waarde-verwijderd",
        args: { value: value.name },
      }),
      body: {
        code: referenceTable.code,
        name: referenceTable.name,
        values: (referenceTable.values ?? []).filter(
          (current) => current.id !== value.id,
        ),
      },
    };
  }

  deleteReferentieTabel(referenceTable: GeneratedType<"RestReferenceTable">) {
    const id = referenceTable.id ?? -1;

    return mergeMutationOptions(
      this.zacQueryClient.DELETE("/rest/referentietabellen/{id}", {
        path: { id },
      }),
      {
        onSuccess: () => {
          void this.invalidateReferentieTabel(id);
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
