#!/usr/bin/env python3

#  SPDX-FileCopyrightText: 2026 INFO.nl
#  SPDX-License-Identifier: EUPL-1.2+

"""
Reads and updates the version tables on a sprint hero page in Confluence.

  hero_page_tables.py latest
      Prints the id, title and link of the newest "Sprint <n> - Hero" page in the Confluence space PZW.
  hero_page_tables.py show <page-url-or-id>
      Prints the version tables as JSON: {table: {row: {column: value}}}, with the row and column names exactly as
      they are on the page.
  hero_page_tables.py plan <page-url-or-id> <values.json> <plan.json>
      Compares the values with the page, prints every change and saves the new page body to plan.json.
      Writes nothing to Confluence.
  hero_page_tables.py apply <plan.json>
      Writes the planned page body, only if the page has not changed since the plan was made.

values.json has the same shape as the output of show and holds only the cells to set. Only version cells can be
set: never the first column, Last Checked, Location or Remarks / Stories. For every row in values.json the
Last Checked date becomes today. In the tables whose legend is "major / minor / patch behind", the colour of the
Our Version on TEST cell follows that legend.

Reading uses the Confluence REST API with an API token, or acli when there is no token. Writing needs the token:
ATLASSIAN_EMAIL and ATLASSIAN_API_TOKEN, or the email of `acli confluence auth status` and the macOS Keychain item
atlassian-api-token.
"""

from __future__ import annotations

import base64
import datetime
import hashlib
import html
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

SITE = os.environ.get('CONFLUENCE_SITE', 'https://dimpact.atlassian.net')
SPACE = 'PZW'

# First header cell of a version table on the page -> name of the table in the JSON.
TABLES = {
    'PodiumD Component': 'PodiumD componenten',
    'Supporting Component': 'Supporting componenten',
    "Externe API's": "Externe API's",
}
LAST_CHECKED = 'Last Checked'
OUR_VERSION = 'Our Version on TEST'
PROTECTED_COLUMNS = {LAST_CHECKED, 'Location', 'Remarks / Stories'}

# Tables whose legend is: major behind, minor behind, patch behind, aligned.
VERSION_DISTANCE_TABLES = {'Supporting componenten', "Externe API's"}
VERSION_DISTANCE_COLOURS = {
    'major': '#ff8f73',
    'minor': '#ffc400',
    'patch': '#abf5d1',
    'aligned': '#57d9a3',
}

# Labels of a prerelease, such as '1.2.3-rc.1' or '37.0.0.Beta1'; a build number or '.Final' is no prerelease.
PRERELEASE = re.compile(r'[-.](?:alpha|beta|rc|cr|preview|pre|snapshot|dev|milestone)\d*(?:\b|[.-])', re.I)

CELL = re.compile(r'<(td|th)\b([^>]*?)(?:/>|>(.*?)</\1>)', re.S)


class HeroPageError(Exception):
    pass


# ---------------------------------------------------------------- Confluence access

def _credentials():
    email = os.environ.get('ATLASSIAN_EMAIL')
    if not email:
        status = _run(['acli', 'confluence', 'auth', 'status'])
        match = re.search(r'Email:\s*(\S+)', status or '')
        email = match.group(1) if match else None
    token = os.environ.get('ATLASSIAN_API_TOKEN')
    if not token:
        token = _run(['security', 'find-generic-password', '-a', os.environ.get('USER', ''),
                      '-s', 'atlassian-api-token', '-w'])
    return (email, token.strip()) if email and token else None


def _run(command):
    try:
        result = subprocess.run(command, capture_output=True, text=True, check=False)
    except FileNotFoundError:
        return None
    return result.stdout if result.returncode == 0 else None


def _request(method, path, credentials, payload=None):
    email, token = credentials
    request = urllib.request.Request(
        f'{SITE}{path}', method=method,
        data=json.dumps(payload).encode() if payload is not None else None,
        headers={
            'Authorization': 'Basic ' + base64.b64encode(f'{email}:{token}'.encode()).decode(),
            'Accept': 'application/json',
            'Content-Type': 'application/json',
        })
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise HeroPageError(f'{method} {path} failed: HTTP {error.code} {error.read().decode()[:500]}') from error


def page_id_of(page):
    match = (re.search(r'/pages/(\d+)', page) or re.search(r'[?&]pageId=(\d+)', page)
             or re.fullmatch(r'(\d+)', page))
    if not match:
        raise HeroPageError(f'not a Confluence page link or id: {page}')
    return match.group(1)


def read_page(page_id):
    credentials = _credentials()
    if credentials:
        data = _request('GET', f'/wiki/api/v2/pages/{page_id}?body-format=storage', credentials)
    else:
        output = _run(['acli', 'confluence', 'page', 'view', '--id', page_id, '--body-format', 'storage', '--json'])
        if output is None:
            raise HeroPageError('cannot read the page: no API token and acli is not logged in')
        data = json.loads(output)
    return {
        'id': page_id,
        'title': data['title'],
        'version': data['version']['number'],
        'body': data['body']['storage']['value'],
    }


# ---------------------------------------------------------------- table parsing

def _text(fragment):
    return ' '.join(html.unescape(re.sub(r'<[^>]+>', ' ', fragment or '')).split())


def _key(name):
    return ' '.join(name.split()).casefold()


def parse_tables(body):
    """
    Finds the version tables by their first header cell. Returns {table: {'columns': [...], 'rows': {row: {column:
    cell}}}}, where a cell holds its absolute position in the body, so it can be replaced without touching the rest.
    """
    tables = {}
    for table_match in re.finditer(r'<table\b.*?</table>', body, re.S):
        table_html = table_match.group(0)
        if table_html.count('<table') > 1:
            continue
        rows = []
        pending_rowspans = {}
        for row_match in re.finditer(r'<tr\b.*?</tr>', table_html, re.S):
            cells = {}
            column = 0
            for cell_match in CELL.finditer(row_match.group(0)):
                while pending_rowspans.get(column, 0) > 0:
                    column += 1
                attributes = cell_match.group(2)
                colspan = int((re.search(r'colspan="(\d+)"', attributes) or [0, 1])[1])
                rowspan = int((re.search(r'rowspan="(\d+)"', attributes) or [0, 1])[1])
                start = table_match.start() + row_match.start() + cell_match.start()
                cells[column] = {
                    'tag': cell_match.group(1),
                    'attributes': attributes,
                    'inner': cell_match.group(3),
                    'start': start,
                    'end': start + len(cell_match.group(0)),
                }
                for spanned in range(column, column + colspan):
                    if rowspan > 1:
                        pending_rowspans[spanned] = rowspan
                column += colspan
            rows.append(cells)
            pending_rowspans = {spanned: left - 1 for spanned, left in pending_rowspans.items() if left > 1}
        if not rows or 0 not in rows[0]:
            continue
        table_name = TABLES.get(_text(rows[0][0]['inner']))
        if table_name is None:
            continue
        if table_name in tables:
            raise HeroPageError(f'the page has more than one "{table_name}" table')
        header = rows[0]
        columns = {index: _text(cell['inner']) for index, cell in header.items()}
        table_rows = {}
        for cells in rows[1:]:
            if 0 not in cells:
                continue
            row_name = _text(cells[0]['inner'])
            table_rows[row_name] = {columns[index]: cell for index, cell in cells.items() if index in columns}
        tables[table_name] = {'columns': [columns[index] for index in sorted(columns)], 'rows': table_rows}
    missing = set(TABLES.values()) - set(tables)
    if missing:
        raise HeroPageError(f'version tables not found on the page: {", ".join(sorted(missing))}')
    return tables


def _cell_value(column, cell):
    if column == LAST_CHECKED:
        date = re.search(r'<time\b[^>]*datetime="([^"]*)"', cell['inner'] or '')
        return date.group(1) if date else _text(cell['inner'])
    return _text(cell['inner'])


def tables_as_values(tables):
    return {table_name: {row_name: {column: _cell_value(column, cell) for column, cell in row.items()}
                         for row_name, row in table['rows'].items()}
            for table_name, table in tables.items()}


def cell_colour(cell):
    match = re.search(r'data-highlight-colour="([^"]*)"', cell['attributes'])
    return match.group(1) if match else None


# ---------------------------------------------------------------- versions and colours

def _numbers(version):
    return [int(number) for number in re.findall(r'\d+', version)]


def version_distance(ours, latest):
    """
    'aligned', 'patch', 'minor', 'major' or 'ahead'; 'prerelease' when a version is a prerelease, which numbers alone
    cannot order; None when a version has no numbers.
    """
    if PRERELEASE.search(ours) or PRERELEASE.search(latest):
        return 'prerelease'
    ours_numbers = _numbers(ours)
    if not ours_numbers:
        return None
    candidates = re.findall(r'([\d.]+[-\w.]*)\s*\((?:mainline|stable)\)', latest) or [latest]
    candidate_numbers = [_numbers(candidate) for candidate in candidates if _numbers(candidate)]
    if not candidate_numbers:
        return None
    # nginx has a mainline and a stable line: compare with the line we are on, else with the first (mainline).
    latest_numbers = next((numbers for numbers in candidate_numbers if numbers[:2] == ours_numbers[:2]),
                          candidate_numbers[0])
    if ours_numbers == latest_numbers:
        return 'aligned'
    if ours_numbers > latest_numbers:
        return 'ahead'
    if ours_numbers[0] != latest_numbers[0]:
        return 'major'
    if ours_numbers[1:2] != latest_numbers[1:2]:
        return 'minor'
    return 'patch'


# ---------------------------------------------------------------- editing

def _replace_text(inner, value):
    """Replaces the only text of a cell, keeping its formatting (paragraph, bold, colour span)."""
    escaped = html.escape(value, quote=False)
    if inner is None or not _text(inner):
        empty_paragraph = re.fullmatch(r'\s*<p\b([^>]*?)\s*/>\s*', inner or '')
        if empty_paragraph:
            return f'<p{empty_paragraph.group(1)}>{escaped}</p>'
        if inner is None or not inner.strip():
            return f'<p>{escaped}</p>'
        raise HeroPageError(f'cannot place a value in this empty cell: {inner[:200]}')
    parts = re.split(r'(<[^>]+>)', inner)
    text_parts = [index for index, part in enumerate(parts) if not part.startswith('<') and part.strip()]
    if len(text_parts) != 1:
        raise HeroPageError(f'cell has more than one piece of text, update it by hand: {_text(inner)}')
    part = parts[text_parts[0]]
    leading = part[:len(part) - len(part.lstrip())]
    trailing = part[len(part.rstrip()):]
    parts[text_parts[0]] = f'{leading}{escaped}{trailing}'
    return ''.join(parts)


def _set_date(inner, date):
    if re.search(r'<time\b[^>]*datetime="[^"]*"', inner or ''):
        return re.sub(r'(<time\b[^>]*datetime=")[^"]*(")', rf'\g<1>{date}\g<2>', inner, count=1)
    empty_paragraph = re.fullmatch(r'\s*<p\b([^>]*?)\s*/>\s*', inner or '')
    if empty_paragraph:
        return f'<p{empty_paragraph.group(1)}><time datetime="{date}" /></p>'
    if inner is None or not inner.strip():
        return f'<p><time datetime="{date}" /></p>'
    raise HeroPageError(f'cannot place a date in this Last Checked cell: {_text(inner)}')


def _set_colour(attributes, colour):
    if 'data-highlight-colour="' in attributes:
        return re.sub(r'data-highlight-colour="[^"]*"', f'data-highlight-colour="{colour}"', attributes, count=1)
    return f' data-highlight-colour="{colour}"{attributes}'


def _row_span(attributes):
    match = re.search(r'\browspan="(\d+)"', attributes)
    return int(match.group(1)) if match else 1


def _cell_html(cell, attributes, inner):
    return f'<{cell["tag"]}{attributes}>{inner}</{cell["tag"]}>'


def plan_changes(page, values, today):
    tables = parse_tables(page['body'])
    edits = []
    changes = []
    warnings = []

    for table_name, rows in values.items():
        if table_name not in tables:
            raise HeroPageError(f'unknown table "{table_name}"; the tables are: {", ".join(tables)}')
        table = tables[table_name]
        page_rows = {_key(name): name for name in table['rows']}
        page_columns = {_key(name): name for name in table['columns']}
        for row_input, columns_input in rows.items():
            row_name = page_rows.get(_key(row_input))
            if row_name is None:
                raise HeroPageError(f'unknown row "{row_input}" in "{table_name}"; the rows are: '
                                    f'{", ".join(table["rows"])}')
            row = table['rows'][row_name]
            final_values = {column: _text(cell['inner']) for column, cell in row.items()}
            cell_updates = {}
            verified_columns = set()
            for column_input, value in columns_input.items():
                column = page_columns.get(_key(column_input))
                if column is None:
                    raise HeroPageError(f'unknown column "{column_input}" in "{table_name}"; the columns are: '
                                        f'{", ".join(table["columns"])}')
                if column in PROTECTED_COLUMNS or column == table['columns'][0]:
                    raise HeroPageError(f'column "{column}" is not a version column and is never changed')
                if column not in row:
                    raise HeroPageError(f'"{row_name}" has no own cell in column "{column}" (merged cell)')
                value = ' '.join(str(value).split())
                final_values[column] = value
                verified_columns.add(column)
                if value != _text(row[column]['inner']):
                    cell_updates[column] = value

            colour_update = None
            if table_name in VERSION_DISTANCE_TABLES and OUR_VERSION in row:
                latest_column = next((column for column in table['columns'] if column.startswith('Latest')), None)
                unverified = [column for column in (latest_column, OUR_VERSION) if column not in verified_columns]
                distance = 'unverified' if unverified else version_distance(
                    final_values.get(OUR_VERSION, ''), final_values.get(latest_column, ''))
                if distance == 'unverified':
                    if latest_column in cell_updates or OUR_VERSION in cell_updates:
                        warnings.append(f'{table_name} / {row_name}: {" and ".join(unverified)} not verified in this '
                                        f'run; colour left as it is, check it')
                elif distance == 'ahead':
                    warnings.append(f'{table_name} / {row_name}: {OUR_VERSION} {final_values[OUR_VERSION]} is higher '
                                    f'than {latest_column} {final_values[latest_column]}; colour left as it is')
                elif distance == 'prerelease':
                    warnings.append(f'{table_name} / {row_name}: {OUR_VERSION} {final_values[OUR_VERSION]} or '
                                    f'{latest_column} {final_values[latest_column]} is a prerelease; colour left as it '
                                    f'is, check it')
                elif distance is None:
                    warnings.append(f'{table_name} / {row_name}: no version number to compare; colour left as it is')
                elif VERSION_DISTANCE_COLOURS[distance] != cell_colour(row[OUR_VERSION]):
                    colour_update = (distance, VERSION_DISTANCE_COLOURS[distance])
            elif table_name not in VERSION_DISTANCE_TABLES and cell_updates:
                warnings.append(f'{table_name} / {row_name}: values changed; the colour of this table is not set '
                                f'automatically, check it')

            version_columns = [column for column in table['columns']
                               if column not in PROTECTED_COLUMNS and column != table['columns'][0]]
            unverified_columns = [column for column in version_columns if column not in verified_columns]
            if unverified_columns and LAST_CHECKED in row:
                old_date = (re.search(r'datetime="([^"]*)"', row[LAST_CHECKED]['inner'] or '') or [None, 'none'])[1]
                warnings.append(f'{table_name} / {row_name}: {", ".join(unverified_columns)} not verified in this run; '
                                f'{LAST_CHECKED} kept at {old_date}')

            for column, cell in row.items():
                attributes = cell['attributes']
                inner = cell['inner']
                changed = False
                if column in cell_updates:
                    inner = _replace_text(inner, cell_updates[column])
                    changes.append((table_name, row_name, column, _text(cell['inner']), cell_updates[column]))
                    changed = True
                if column == OUR_VERSION and colour_update:
                    attributes = _set_colour(attributes, colour_update[1])
                    changes.append((table_name, row_name, f'{column} colour', cell_colour(cell) or 'none',
                                    f'{colour_update[1]} ({colour_update[0]})'))
                    changed = True
                if column == LAST_CHECKED and not unverified_columns:
                    old_date = (re.search(r'datetime="([^"]*)"', inner or '') or [None, 'none'])[1]
                    if _row_span(attributes) > 1:
                        warnings.append(f'{table_name} / {row_name}: {LAST_CHECKED} is shared with other rows; '
                                        f'kept at {old_date}, update it by hand')
                    elif old_date != today:
                        inner = _set_date(inner, today)
                        changes.append((table_name, row_name, LAST_CHECKED, old_date, today))
                        changed = True
                if changed:
                    edits.append((cell['start'], cell['end'], _cell_html(cell, attributes, inner)))

    body = page['body']
    for start, end, replacement in sorted(edits, reverse=True):
        body = body[:start] + replacement + body[end:]
    # The new body must parse to the intended values, and nothing outside the edited cells may change.
    check = tables_as_values(parse_tables(body))
    for table_name, rows in values.items():
        page_rows = {_key(name): name for name in check[table_name]}
        for row_input, columns_input in rows.items():
            row_values = check[table_name][page_rows[_key(row_input)]]
            for column_input, value in columns_input.items():
                column = next(column for column in row_values if _key(column) == _key(column_input))
                if row_values[column] != ' '.join(str(value).split()):
                    raise HeroPageError(f'self-check failed for {table_name} / {row_input} / {column_input}')
    return body, changes, warnings


def _sha256(text):
    return hashlib.sha256(text.encode()).hexdigest()


# ---------------------------------------------------------------- commands

def latest():
    credentials = _credentials()
    if not credentials:
        raise HeroPageError('searching needs an API token; ask the user for the link of the sprint hero page')
    query = urllib.parse.urlencode({'cql': f'space = {SPACE} and type = page and title ~ "Hero"', 'limit': 200})
    results = _request('GET', f'/wiki/rest/api/content/search?{query}', credentials)['results']
    sprints = []
    for result in results:
        match = re.fullmatch(r'Sprint (\d+) - Hero', result['title'].strip())
        if match:
            sprints.append((int(match.group(1)), result))
    if not sprints:
        raise HeroPageError(f'no "Sprint <n> - Hero" page found in space {SPACE}')
    _, newest = max(sprints, key=lambda sprint: sprint[0])
    print(json.dumps({'id': newest['id'], 'title': newest['title'],
                      'url': f'{SITE}/wiki/pages/viewpage.action?pageId={newest["id"]}'}, ensure_ascii=False))


def show(page):
    page_data = read_page(page_id_of(page))
    print(json.dumps({'page': {'id': page_data['id'], 'title': page_data['title'], 'version': page_data['version']},
                      'tables': tables_as_values(parse_tables(page_data['body']))}, indent=2, ensure_ascii=False))


def plan(page, values_file, plan_file):
    page_data = read_page(page_id_of(page))
    with open(values_file, encoding='utf-8') as file:
        values = json.load(file)
    values = values.get('tables', values)
    today = datetime.date.today().isoformat()
    body, changes, warnings = plan_changes(page_data, values, today)
    print(f'Page: {page_data["title"]} (id {page_data["id"]}, version {page_data["version"]})')
    if not changes:
        print('No changes.')
    current_table = None
    for table_name, row_name, column, old, new in changes:
        if table_name != current_table:
            print(f'\n## {table_name}')
            current_table = table_name
        print(f'- {row_name} / {column}: {old} -> {new}')
    for warning in warnings:
        print(f'WARNING: {warning}')
    with open(plan_file, 'w', encoding='utf-8') as file:
        json.dump({'id': page_data['id'], 'title': page_data['title'], 'version': page_data['version'],
                   'original_sha256': _sha256(page_data['body']), 'body': body, 'changes': len(changes)}, file)
    print(f'\nPlan saved to {plan_file}. Nothing has been written to Confluence.')


def apply(plan_file):
    with open(plan_file, encoding='utf-8') as file:
        planned = json.load(file)
    credentials = _credentials()
    if not credentials:
        raise HeroPageError('writing needs an API token: set ATLASSIAN_EMAIL and ATLASSIAN_API_TOKEN, or add the '
                            'Keychain item atlassian-api-token')
    if not planned['changes']:
        print('The plan has no changes; nothing written.')
        return
    current = read_page(planned['id'])
    if current['version'] != planned['version'] or _sha256(current['body']) != planned['original_sha256']:
        raise HeroPageError(f'the page changed since the plan (version {planned["version"]} -> {current["version"]});'
                            f' make a new plan')
    result = _request('PUT', f'/wiki/api/v2/pages/{planned["id"]}', credentials, {
        'id': planned['id'],
        'status': 'current',
        'title': planned['title'],
        'body': {'representation': 'storage', 'value': planned['body']},
        'version': {'number': planned['version'] + 1, 'message': 'sprint-hero-page: version tables'},
    })
    print(f'Updated "{result["title"]}" to version {result["version"]["number"]}: '
          f'{SITE}/wiki/pages/viewpage.action?pageId={planned["id"]}')


def main(arguments):
    commands = {'latest': (latest, 0), 'show': (show, 1), 'plan': (plan, 3), 'apply': (apply, 1)}
    if not arguments or arguments[0] not in commands or len(arguments) - 1 != commands[arguments[0]][1]:
        print(__doc__, file=sys.stderr)
        return 2
    function, _ = commands[arguments[0]]
    try:
        function(*arguments[1:])
    except HeroPageError as error:
        print(f'ERROR: {error}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
