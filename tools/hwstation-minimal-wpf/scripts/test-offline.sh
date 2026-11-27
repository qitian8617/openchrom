#!/bin/sh
set -eu
root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
cd "$root"
if [ -x "$HOME/.dotnet/dotnet" ]; then
  PATH="$HOME/.dotnet:$PATH"
  export PATH
fi
dotnet run --project tests/HwMinimal.Tests/HwMinimal.Tests.csproj -c Release --nologo
