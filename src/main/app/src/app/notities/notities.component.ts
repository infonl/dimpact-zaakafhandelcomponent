/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CdkTextareaAutosize } from "@angular/cdk/text-field";
import { NgFor, NgIf } from "@angular/common";
import {
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  ViewChild,
} from "@angular/core";
import { MatBadgeModule } from "@angular/material/badge";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { ObjectType } from "../core/websocket/model/object-type";
import { Opcode } from "../core/websocket/model/opcode";
import { WebsocketService } from "../core/websocket/websocket.service";
import { IdentityService } from "../identity/identity.service";
import { injectMutation } from "../shared/http/inject-mutation";
import { DatumPipe } from "../shared/pipes/datum.pipe";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitieService } from "./notities.service";

@Component({
  selector: "zac-notities",
  templateUrl: "./notities.component.html",
  styleUrls: ["./notities.component.less"],
  standalone: true,
  imports: [
    NgIf,
    NgFor,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    CdkTextareaAutosize,
    TranslateModule,
    DatumPipe,
  ],
})
export class NotitiesComponent {
  private readonly identityService = inject(IdentityService);
  private readonly notitieService = inject(NotitieService);
  private readonly websocketService = inject(WebsocketService);
  private readonly queryClient = inject(QueryClient);

  readonly zaakUuid = input.required<string>();
  readonly notitieRechten = input<GeneratedType<"RestNotitieRechten">>();

  @ViewChild("notitieTekst") notitieTekst!: {
    nativeElement: HTMLTextAreaElement;
  };
  @ViewChild("scrollTarget") scrollTarget!: ElementRef;

  private readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );
  private readonly notitiesQuery = injectQuery(() =>
    this.notitieService.listNotities(this.zaakUuid()),
  );
  protected readonly notities = computed(() =>
    [...(this.notitiesQuery.data() ?? [])].sort((a, b) => {
      if (!a.tijdstipLaatsteWijziging) return -1;
      if (!b.tijdstipLaatsteWijziging) return 1;

      return b.tijdstipLaatsteWijziging.localeCompare(
        a.tijdstipLaatsteWijziging,
      );
    }),
  );

  private readonly createNotitieMutation = injectMutation(
    () => this.notitieService.createNotitie(),
    {
      onSuccess: (notitie) => {
        this.updateCachedNotities((notities) => [notitie, ...notities]);
        this.notitieTekst.nativeElement.value = "";
        this.scrollTarget.nativeElement.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
      },
    },
  );
  private readonly updateNotitieMutation = injectMutation(
    () => this.notitieService.updateNotitie(),
    {
      onSuccess: (updatedNotitie, { id }) => {
        this.updateCachedNotities((notities) =>
          notities.map((notitie) =>
            notitie.id === id ? { ...notitie, ...updatedNotitie } : notitie,
          ),
        );
        this.geselecteerdeNotitieId = null;
      },
    },
  );
  private readonly deleteNotitieMutation = injectMutation(
    () => this.notitieService.deleteNotitie(),
    {
      onSuccess: (_data, id) => {
        this.updateCachedNotities((notities) =>
          notities.filter((notitie) => notitie.id !== id),
        );
      },
    },
  );

  protected showNotes = false;
  protected geselecteerdeNotitieId: number | null = null;
  protected maxLengteTextArea = 1000;

  constructor() {
    effect((onCleanup) => {
      const zaakUuid = this.zaakUuid();
      const notitiesListener = this.websocketService.addListener(
        Opcode.UPDATED,
        ObjectType.ZAAK_NOTITIES,
        zaakUuid,
        () =>
          void this.queryClient.invalidateQueries({
            queryKey: this.notitieService.listNotities(zaakUuid).queryKey,
          }),
      );
      onCleanup(() => this.websocketService.removeListener(notitiesListener));
    });
  }

  protected toggleNotitieContainer() {
    this.showNotes = !this.showNotes;
  }

  protected pasNotitieAan(id: GeneratedType<"RestNote">["id"]) {
    this.geselecteerdeNotitieId = id ?? null;
  }

  private updateCachedNotities(
    update: (
      notities: GeneratedType<"RestNote">[],
    ) => GeneratedType<"RestNote">[],
  ) {
    this.queryClient.setQueryData(
      this.notitieService.listNotities(this.zaakUuid()).queryKey,
      (notities) => update(notities ?? []),
    );
  }

  protected maakNotitieAan(tekst: string) {
    const loggedInUser = this.loggedInUserQuery.data();
    if (!loggedInUser?.id) return;
    if (tekst.length === 0) return;
    if (tekst.length > this.maxLengteTextArea) return;

    this.createNotitieMutation.mutate({
      zaakUUID: this.zaakUuid(),
      tekst: tekst,
      gebruikersnaamMedewerker: loggedInUser.id,
    });
  }

  protected updateNotitie(notitie: GeneratedType<"RestNote">, tekst: string) {
    const loggedInUser = this.loggedInUserQuery.data();
    if (!loggedInUser?.id) return;

    if (tekst.length === 0) return;
    if (tekst.length > this.maxLengteTextArea) return;

    this.updateNotitieMutation.mutate({
      ...notitie,
      tekst,
      gebruikersnaamMedewerker: loggedInUser.id,
    });
  }

  protected annuleerUpdateNotitie() {
    this.notitieTekst.nativeElement.value = "";
    this.geselecteerdeNotitieId = null;
  }

  protected verwijderNotitie(id: GeneratedType<"RestNote">["id"]) {
    if (id == null) return;

    this.deleteNotitieMutation.mutate(id);
  }
}
