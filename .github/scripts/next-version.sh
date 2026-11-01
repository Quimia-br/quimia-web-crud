#!/usr/bin/env bash
# Calcula a próxima versão (sem o "v") a partir dos commits desde a última tag vX.Y.Z, pelo padrão Conventional Commits.
#   - "tipo!:" ou "BREAKING CHANGE:" -> major (enquanto a versão é 0.x, sobe o minor)
#   - "feat:"                        -> minor
#   - qualquer outro commit          -> patch
# Imprime a versão, ou nada se não há commits novos desde a última tag. Sem nenhuma tag, a primeira versão é 0.1.0.
set -euo pipefail

last=$(git tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | head -n 1)

if [ -z "$last" ]; then
  echo "0.1.0"
  exit 0
fi

range="$last..HEAD"
if [ -z "$(git log "$range" --no-merges --format=%H)" ]; then
  exit 0
fi

subjects=$(git log "$range" --no-merges --format=%s)
bodies=$(git log "$range" --no-merges --format=%b)

IFS=. read -r major minor patch <<< "${last#v}"

if echo "$subjects" | grep -Eq '^[a-z]+(\([^)]+\))?!:' || echo "$bodies" | grep -q '^BREAKING CHANGE:'; then
  if [ "$major" -eq 0 ]; then
    minor=$((minor + 1)); patch=0
  else
    major=$((major + 1)); minor=0; patch=0
  fi
elif echo "$subjects" | grep -Eq '^feat(\([^)]+\))?:'; then
  minor=$((minor + 1)); patch=0
else
  patch=$((patch + 1))
fi

echo "$major.$minor.$patch"
