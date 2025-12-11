# PlantUML diagrams

We prefer to use [Mermaid](https://mermaid.js.org/) for architecture diagrams, however for more complex architecture diagrams
we sometimes use [PlantUML](https://plantuml.com/) instead as it generates better diagrams.

This folder contains the PlantUML diagrams used in the ZAC solution architecture documentation.

## Generate image files from PlantUML diagrams

To generate PNG image files from the PlantUML diagrams you can use the PlantUML CLI tool.
Mac users can use the [PlantUML Homebrew formulae](https://formulae.brew.sh/formula/plantuml).

To generate a PNG image from a PlantUML diagram file (e.g. `diagram.puml`), run:

```sh
plantuml diagram.puml --output-dir ../images
```
