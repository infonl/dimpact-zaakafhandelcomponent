def severity_to_level:
  if .severity == "critical" then "error"
  elif .severity == "high" then "error"
  elif .severity == "moderate" then "warning"
  elif .severity == "low" then "note"
  else "none"
  end;

def severity_to_numeric:
  if .severity == "critical" then "9.0"
  elif .severity == "high" then "7.0"
  elif .severity == "moderate" then "5.0"
  elif .severity == "low" then "3.0"
  else "0.0"
  end;

{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [
    {
      "tool": {
        "driver": {
          "name": "npm audit",
          "informationUri": "https://docs.npmjs.com/cli/v10/commands/npm-audit",
          "version": "1.0.0",
          "rules": 
            .vulnerabilities | to_entries | map(
              select(.value.via | type == "array" and (map(type == "object") | any)) |
              .value.via | map(select(type == "object")) | .[]
            ) | unique_by(.source) | map({
              "id": (.source | tostring),
              "name": .name,
              "shortDescription": {
                "text": .title
              },
              "fullDescription": {
                "text": .title
              },
              "helpUri": .url,
              "defaultConfiguration": {
                "level": (
                  severity_to_level
                )
              },
              "properties": {
                "security-severity": (
                  severity_to_numeric
                )
              }
            })
        }
      },
      "results": 
        .vulnerabilities | to_entries | map(
          {
            package: .key,
            data: .value
          } | 
          (.data.via // []) | map(select(type == "object")) | map({
            "ruleId": (.source | tostring),
            "level": (
              severity_to_level
            ),
            "message": {
              "text": "\(.title) - Affects package: \(.dependency) (via \(.name))"
            },
            "locations": [
              {
                "physicalLocation": {
                  "artifactLocation": {
                    "uri": "package.json"
                  }
                },
                "logicalLocations": [
                  {
                    "name": .dependency,
                    "kind": "package"
                  }
                ]
              }
            ]
          })
        ) | flatten
    }
  ]
}
