---
SPDX-FileCopyrightText: 2024 Lifely
SPDX-License-Identifier: EUPL-1.2+

title: Docs
layout: page
permalink: /docs/
---

## Manuals
{% for doc in site.docs %}
 - [{{ doc.title }}]({{ doc.url }}) (in Dutch)
{% endfor %}
