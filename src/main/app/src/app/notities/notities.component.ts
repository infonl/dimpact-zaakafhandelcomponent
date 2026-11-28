/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CdkTextareaAutosize } from "@angular/cdk/text-field";

import {
  afterRenderEffect,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  signal,
  viewChild,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { IdentityService } from "../identity/identity.service";
import { injectMutation } from "../shared/http/inject-mutation";
import { DatumPipe } from "../shared/pipes/datum.pipe";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitieService } from "./notities.service";

// The database stores tijdstip_laatste_wijziging as a date only, so it cannot order the notities of one day
function oldestFirst(
  notitie: GeneratedType<"RestNote">,
  andereNotitie: GeneratedType<"RestNote">,
) {
  return (notitie.id ?? 0) - (andereNotitie.id ?? 0);
}

@Component({
  selector: "zac-notities",
  templateUrl: "./notities.component.html",
  styleUrls: ["./notities.component.less"],
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatDividerModule,
    MatToolbarModule,
    MatFormFieldModule,
    MatInputModule,
    CdkTextareaAutosize,
    TranslatePipe,
    DatumPipe,
  ],
})
export class NotitiesComponent {
  private readonly identityService = inject(IdentityService);
  private readonly notitieService = inject(NotitieService);
  private readonly queryClient = inject(QueryClient);

  readonly sideNav = input.required<MatDrawer>();
  readonly zaakUuid = input.required<string>();
  readonly notitieRechten = input<GeneratedType<"RestNotitieRechten">>();

  private readonly noteBody =
    viewChild.required<ElementRef<HTMLElement>>("noteBody");

  private readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );
  private readonly notitiesQuery = injectQuery(() =>
    this.notitieService.listNotities(this.zaakUuid()),
  );
  protected readonly notities = computed(() =>
    [...(this.notitiesQuery.data() ?? [])].sort(oldestFirst),
  );
  protected readonly aantalNotitiesSuffix = computed(() => {
    const aantalNotities = this.notities().length;
    return aantalNotities ? ` (${aantalNotities})` : "";
  });

  private scrollToLastNoteAfterRender = false;
  private readonly scrollToLastNote = afterRenderEffect(() => {
    this.notities();
    if (!this.scrollToLastNoteAfterRender) return;

    this.scrollToLastNoteAfterRender = false;
    const noteBody = this.noteBody().nativeElement;
    noteBody.scrollTo({ top: noteBody.scrollHeight, behavior: "smooth" });
  });

  protected readonly nieuweNotitieTekst = signal("");
  protected readonly bewerkteNotitieTekst = signal("");

  private readonly createNotitieMutation = injectMutation(
    () => this.notitieService.createNotitie(),
    {
      onSuccess: (notitie) => {
        this.scrollToLastNoteAfterRender = true;
        this.updateCachedNotities((notities) => [notitie, ...notities]);
        this.nieuweNotitieTekst.set("");
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
        this.geselecteerdeNotitieId.set(null);
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

  protected readonly geselecteerdeNotitieId = signal<number | null>(null);
  protected readonly maxLengteTextArea = 1000;

  protected pasNotitieAan(notitie: GeneratedType<"RestNote">) {
    this.bewerkteNotitieTekst.set(notitie.tekst);
    this.geselecteerdeNotitieId.set(notitie.id ?? null);
  }

  protected tekstVan(event: Event) {
    return (event.target as HTMLTextAreaElement).value;
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

  protected maakNotitieAan() {
    const tekst = this.nieuweNotitieTekst();
    const loggedInUser = this.loggedInUserQuery.data();
    if (!loggedInUser?.username) return;
    if (tekst.length === 0) return;
    if (tekst.length > this.maxLengteTextArea) return;

    this.createNotitieMutation.mutate({
      zaakUUID: this.zaakUuid(),
      tekst: tekst,
      gebruikersnaamMedewerker: loggedInUser.username,
    });
  }

  protected updateNotitie(notitie: GeneratedType<"RestNote">) {
    const tekst = this.bewerkteNotitieTekst();
    const loggedInUser = this.loggedInUserQuery.data();
    if (!loggedInUser?.username) return;

    if (tekst.length === 0) return;
    if (tekst.length > this.maxLengteTextArea) return;

    this.updateNotitieMutation.mutate({
      ...notitie,
      tekst,
      gebruikersnaamMedewerker: loggedInUser.username,
    });
  }

  protected annuleerUpdateNotitie() {
    this.geselecteerdeNotitieId.set(null);
  }

  protected verwijderNotitie(id: GeneratedType<"RestNote">["id"]) {
    if (id == null) return;

    this.deleteNotitieMutation.mutate(id);
  }
}
