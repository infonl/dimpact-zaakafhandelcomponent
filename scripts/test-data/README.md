# Test data scripts

Scripts that fill the local [kind stack](../../docs/development/installKind.md) of ZAC with test data, so you can
develop and test a story without clicking the data together by hand. They only work against a local stack, and log
in as `beheerder1`. Run any script with `-h` or `--help` for its options.

Start the stack with ZAC first, for example with `./start-kind.sh -l`. The Python scripts need Python 3.10 or later;
the shell scripts need `curl` and `jq`.

## Scripts

| Script                                                                       | What it is for                                                                                       |
|------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| [`create-zaak.py`](create-zaak.py)                                           | Creates zaken of a test zaaktype, optionally with documents.                                         |
| [`create-inbox-documenten.sh`](create-inbox-documenten.sh)                   | Creates documents without a zaak, which ZAC shows in *Inbox documenten*.                             |
| [`create-ontkoppelde-documenten.sh`](create-ontkoppelde-documenten.sh)       | Creates *Ontkoppelde documenten*, optionally locked or missing in Open Zaak.                         |
| [`configure-zaakafhandelparameters.py`](configure-zaakafhandelparameters.py) | Configures one zaaktype in ZAC, so that you can create zaken of it.                                  |
| [`create-zaaktypes.py`](create-zaaktypes.py)                                 | Creates extra zaaktypes in Open Zaak and PABC, and configures them in ZAC.                           |
| [`create-load.py`](create-load.py)                                           | Load test: configures all 8 test zaaktypes and creates many zaken, optionally in parallel.           |

`zac_client.py`, `zac_testdata.py` and `zac_reporting.py` are shared modules of the Python scripts; they are not run
directly.

## Quick start

```shell
./scripts/test-data/create-zaak.py --zaaktype "Test zaaktype 2" --doc-count 2
./scripts/test-data/create-inbox-documenten.sh -n 5
./scripts/test-data/create-ontkoppelde-documenten.sh -n 6 -l 1 -m 1
```

## Good to know

- On a fresh stack you can create zaken of *Test zaaktype 2* right away. For another zaaktype, run
  `configure-zaakafhandelparameters.py` with its UUID first.
- `create-load.py` overwrites the configuration of the 8 test zaaktypes; pass `--skip-config` to keep yours.
- New zaaktypes from `create-zaaktypes.py --skip-config` only show up in ZAC after a restart, or after
  `GET /rest/admin/util/cache/ztc/clear`.
- To remove the test data again, start the stack from its seed data with `./start-kind.sh -l -d`.
- The scripts call the ZAC REST API with hand-written JSON. When you change a REST path, a payload or the validation
  of an endpoint they use, update them in the same pull request.
