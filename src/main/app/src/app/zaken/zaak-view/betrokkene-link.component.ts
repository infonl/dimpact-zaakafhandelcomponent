import { Component, computed, input } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { buildBedrijfRouteLink } from "../../klanten/klanten-routing.module";
import { KlantenService } from "../../klanten/klanten.service";
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";

@Component({
  selector: "betrokkene-link",
  templateUrl: "./betrokkene-link.html",
  styleUrls: [],
})
export class BetrokkeneLinkComponent {
  protected readonly persoonQuery = injectQuery(() => {
    const betrokkene = this.betrokkene();

    return this.klantService.readPersoon(betrokkene.identificatie, {
      context: "BetrokkeneLinkComponent",
      action: "view",
    });
  });

  protected bedrijfQuery = injectQuery(() => {
    const betrokkene = this.betrokkene();

    return this.klantService.readBedrijf(
      new BetrokkeneIdentificatie(betrokkene),
    );
  });

  protected readonly betrokkene =
    input.required<GeneratedType<"RestZaakBetrokkene">>();

  protected readonly bedrijfRouteLink = computed(() =>
    buildBedrijfRouteLink(this.betrokkene()),
  );

  constructor(private readonly klantService: KlantenService) {}
}
