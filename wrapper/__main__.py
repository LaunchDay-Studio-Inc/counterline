"""Allow ``python -m wrapper`` to run the UCI app."""
import sys

# Check for --profile flag before Typer parses args
_profile = None
_filtered_args: list[str] = []
_iter = iter(sys.argv[1:])
for arg in _iter:
    if arg == "--profile":
        _profile = next(_iter, None)
    else:
        _filtered_args.append(arg)
sys.argv = [sys.argv[0]] + _filtered_args

from wrapper.uci_app import cli, _ACTIVE_PROFILE  # noqa: E402

if _profile:
    _ACTIVE_PROFILE["name"] = _profile

cli()
