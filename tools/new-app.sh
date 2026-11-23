#!/usr/bin/env bash
# Start a new Android product from this repo's full layered skeleton.
#
#   tools/new-app.sh invoicer ~/Documents/projects/invoicer com.acme.invoicer
#
# Copies the committed tree (git archive HEAD), so the new product keeps AGENTS.md, CLAUDE.md,
# the scaffolding scripts, the hooks, the Jenkinsfile and the arch-qube gate. Copying only some
# packages into a fresh Android Studio project loses all of that (AGENTS.md §1).
#
# The applicationId is also used as the Kotlin package / Gradle namespace.
set -euo pipefail

[ $# -ge 2 ] && [ $# -le 3 ] || { echo "usage: $0 <app-name> <dest-dir> [applicationId]" >&2; exit 1; }
APP="$1"
DEST="$2"
[[ "$APP" =~ ^[a-z][a-z0-9-]*$ ]] || { echo "app-name: lower-case letters, digits or -" >&2; exit 1; }
APPID="${3:-com.example.${APP//-/}}"
[[ "$APPID" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] || { echo "applicationId must look like com.company.app (lower-case segments)" >&2; exit 1; }
[ ! -e "$DEST" ] || { echo "$DEST already exists — refusing to overwrite" >&2; exit 1; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
git -C "$ROOT" diff --quiet HEAD -- AGENTS.md tools/ || echo "note: uncommitted changes in AGENTS.md/tools are NOT copied (git archive HEAD)" >&2

OLD_NS="$(sed -nE 's/^[[:space:]]*namespace[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$ROOT/app/build.gradle.kts" | head -1)"
OLD_APPID="$(sed -nE 's/^[[:space:]]*applicationId[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$ROOT/app/build.gradle.kts" | head -1)"
OLD_ROOTNAME="$(sed -nE 's/^rootProject\.name[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$ROOT/settings.gradle.kts")"
OLD_KEY="$(sed -nE 's/^sonar\.projectKey=(.*)$/\1/p' "$ROOT/sonar-project.properties")"
OLD_REPO="$(basename "$(git -C "$ROOT" config --get remote.origin.url 2>/dev/null || echo arcana-android)" .git)"
[ -n "$OLD_NS" ] && [ -n "$OLD_APPID" ] && [ -n "$OLD_ROOTNAME" ] && [ -n "$OLD_KEY" ] \
    || { echo "could not read the template identity (namespace/applicationId/rootProject.name/sonar key)" >&2; exit 1; }
OLD_PATH="${OLD_NS//.//}"
NEW_PATH="${APPID//.//}"
SRC_SHA="$(git -C "$ROOT" rev-parse --short HEAD)"

mkdir -p "$DEST"
git -C "$ROOT" archive HEAD | tar -x -C "$DEST"
cd "$DEST"

# Template-only material that describes the template, not the product.
rm -rf docs/api build CHANGELOG.md
printf '{\n  ".": "0.1.0"\n}\n' > .release-please-manifest.json

# 1. Kotlin package directories (app main/test/androidTest and buildSrc).
for base in app/src/main/java app/src/test/java app/src/androidTest/java buildSrc/src/main/kotlin; do
    [ -d "$base/$OLD_PATH" ] || continue
    mkdir -p "$base/$NEW_PATH"
    tmp="$(mktemp -d)"
    mv "$base/$OLD_PATH"/* "$tmp"/
    mv "$tmp"/* "$base/$NEW_PATH"/
    rmdir "$tmp"
    # remove now-empty old package dirs
    find "$base" -type d -empty -delete
done
# buildSrc plugin descriptor file name carries the package
for f in buildSrc/src/main/resources/META-INF/gradle-plugins/"$OLD_NS".*.properties; do
    [ -e "$f" ] && mv "$f" "${f/$OLD_NS/$APPID}"
done

# 2. Text identity in every text file: package statements/imports, namespace, plugin id,
#    applicationId (build.gradle.kts, fastlane/Appfile), Gradle root name, Sonar key,
#    Jenkins APP_NAME/PROJECT_NAME/image names, repo name.
OLD_NS_RE="${OLD_NS//./\\.}"
OLD_APPID_RE="${OLD_APPID//./\\.}"
find . -type f ! -path './.git/*' -print0 | while IFS= read -r -d '' f; do
    grep -Iq . "$f" 2>/dev/null || continue   # skip binaries / empty files
    if grep -qE "${OLD_NS_RE}|${OLD_APPID_RE}|${OLD_KEY}|${OLD_REPO}" "$f"; then
        sed -i.bak -E \
            -e "s/${OLD_NS_RE}/${APPID}/g" \
            -e "s/${OLD_APPID_RE}/${APPID}/g" \
            -e "s/${OLD_KEY}/${APP}/g" \
            -e "s/${OLD_REPO}/${APP}/g" "$f"
        rm -f "$f.bak"
    fi
done
sed -i.bak -E "s/^rootProject\.name[[:space:]]*=.*/rootProject.name = \"${APP}\"/" settings.gradle.kts && rm -f settings.gradle.kts.bak
sed -i.bak -E "s/versionCode = [0-9]+/versionCode = 1/; s/versionName = \"[^\"]*\"/versionName = \"0.1.0\"/" app/build.gradle.kts && rm -f app/build.gradle.kts.bak
for res in app/src/main/res/values*/strings.xml; do
    sed -i.bak -E "s|<string name=\"app_name\">[^<]*</string>|<string name=\"app_name\">${APP}</string>|" "$res" && rm -f "$res.bak"
done

# Fail loudly if any template identity survived in build-relevant files.
if grep -rIlE "${OLD_NS_RE}|${OLD_APPID_RE}|\"${OLD_ROOTNAME}\"" --include='*.kt' --include='*.kts' \
        --include='*.xml' --include='*.properties' --include='Jenkinsfile' --include='Appfile' --include='*.yml' . ; then
    echo "new-app: template identity still present in the files above" >&2; exit 1
fi

cat >> AGENTS.md <<EOF

## Origin

Created from ${OLD_REPO} @ \`${SRC_SHA}\` by \`tools/new-app.sh\` (package \`${APPID}\`). The
layers, rules and definition of done above apply unchanged. Remove template features you do not
need (User, analytics) by deleting their slice in every layer, not by flattening.
EOF

git init -q
tools/install-hooks.sh >/dev/null
git add -A
# --no-verify only for this import commit: every file counts as "added" here, so the hook would
# judge the whole inherited template at once (and the template itself already passes
# tools/arch-check.sh). Every later commit in the new product goes through the hook.
git -c user.name="${GIT_AUTHOR_NAME:-new-app}" -c user.email="${GIT_AUTHOR_EMAIL:-new-app@localhost}" \
    commit -q --no-verify -m "chore: start ${APP} from ${OLD_REPO} ${SRC_SHA}"

echo "created $DEST (${APP}, ${APPID}) from ${OLD_REPO} ${SRC_SHA}"
echo "next: cd $DEST && tools/new-feature.sh <Name>   # add features as vertical slices"
