---
status: accepted
date: 2023-11-02
decision-makers: Team Genève
tags: [documentation, tooling]
---

# Use Mermaid as diagrams-as-code tool in our technical documentation

## Context and Problem Statement

The existing ZAC architecture documentation was in ArchiMate format
(`docs/solution-architecture/attachments/Vernieuwing e-Suite.archimate`), was out of date and was relatively hard to
maintain. We needed an easy to use and widely supported diagramming solution that fits our way of working.
See [PZ-664](https://dimpact.atlassian.net/browse/PZ-664).

## Decision Drivers

* No paid service, since that goes against the idea of open source
* Support for the [C4 model](https://c4model.com/); we only use the first three C4 levels
* Diagrams live in Git, so they are versioned and reviewed in pull requests

## Considered Options

* Structurizr
* Mermaid
* C4-PlantUML
* Diagrams (diagrams.mingrammer.com)
* Miro
* draw.io

## Decision Outcome

Chosen option: "Mermaid", because it renders directly in GitHub and Confluence without hosting, integrates with
Markdown, has a simple API and supports a wide range of diagram types.

### Consequences

* Good, because diagrams are code: versioned, merged and reviewed like any other change.
* Bad, because we depend on Mermaid, since there is no open standard for diagrams as code.
* Bad, because Mermaid support for C4 diagrams is limited.

### Confirmation

Not checked automatically.

## Pros and Cons of the Options

### Structurizr

* Good, because it is built for C4 diagrams by the creator of C4.
* Good, because it is free and code-based.
* Bad, because it does not support C4 level 4 diagrams.
* Bad, because it has a steep learning curve.
* Bad, because the diagrams need hosting to be rendered and shown.

### Mermaid

* Good, because diagrams render directly in GitHub and Confluence, without hosting.
* Good, because it is free and code-based.
* Good, because the API is simple, so the learning curve is low.
* Bad, because C4 support is limited.
* Bad, because the technology was young at the time, with many open GitHub issues.

### C4-PlantUML

* Good, because it is a mature, well-maintained tool.
* Good, because it is free, code-based, complete and extensible.
* Bad, because it needs a rendering step and therefore hosting.
* Bad, because diagrams are shown as images.

### Diagrams (diagrams.mingrammer.com)

* Good, because it is free and code-based, with many visuals available by default.
* Good, because a proof of concept showed a better developer experience than Structurizr.
* Bad, because it needs Python and Graphviz.
* Bad, because the diagrams need hosting to be rendered and shown.

### Miro

* Good, because it is easy to use for everyone and supports collaborative editing.
* Bad, because it is a paid service with vendor lock-in.
* Bad, because the diagrams live outside Git and have no version control.

### draw.io

* Good, because it is easy to use and built for software diagrams.
* Bad, because diagrams must be exported as images, with vendor lock-in.
* Bad, because the diagrams live outside Git and have no version control.

## More Information

Migrated from the Confluence decision log:
[Use Mermaid as diagrams-as-code tool in our technical documentation](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/10649601).
