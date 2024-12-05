---
SPDX-FileCopyrightText: 2024 Lifely
SPDX-License-Identifier: EUPL-1.2+

layout: page
title: Releases
permalink: /releases/
---

### Latest release {{ site.github.latest_release.tag_name }} at {{ site.github.latest_release.published_at }}
{{ site.github.latest_release.name }}

Check [**release {{ site.github.latest_release.tag_name }}**]({{ site.github.latest_release.url }}) for more details.

### Previous releases

| Version                                  | 
|------------------------------------------|
 {% for release in site.github.releases %} | [{{ release.tag_name }}]({{ release.url }}) | 
{% endfor %}
