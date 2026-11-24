#!/usr/bin/env bash
# Run the same arch-qube scan CI runs (Jenkinsfile stage "Architecture Qube").
# Exit 0 = pass. Exit 2 = could not check (NOT a pass). Anything else = not done (AGENTS.md §5).
#
#   tools/arch-check.sh
#   ARCH_QUBE_IMAGE=... tools/arch-check.sh
#
# Streams the source in with tar | docker cp (a bind mount scans an empty tree under
# Docker-in-Docker — that is how the old CI gate passed while checking nothing), and copies
# the report out to arch-qube-reports/.
#
# The version comes from tools/arch-qube.version, which the Jenkinsfile reads too. Images are
# tried in order: ghcr.io (public, multi-arch), then arcana.boo. There is deliberately NO
# fallback to :latest — an unpinned image changes the verdict without a commit in this repo.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$ROOT/tools/arch-qube.version")"
THRESHOLD=90          # keep equal to the Jenkinsfile stage "Architecture Qube" (--threshold 90)
FRAMEWORK=android     # keep equal to the Jenkinsfile (--framework android)
NAME="arch-check-$$"

command -v docker >/dev/null || { echo "arch-check: docker not found — cannot check (this is NOT a pass)" >&2; exit 2; }
docker info >/dev/null 2>&1 || { echo "arch-check: docker daemon not reachable — cannot check (this is NOT a pass)" >&2; exit 2; }

IMAGE=""
for cand in ${ARCH_QUBE_IMAGE:-} "ghcr.io/jrjohn/arch-qube:${VERSION}" "arcana.boo/arcana/arch-qube:${VERSION}"; do
    if docker image inspect "$cand" >/dev/null 2>&1 || docker pull -q "$cand" >/dev/null 2>&1; then
        IMAGE="$cand"; break
    fi
done
[ -n "$IMAGE" ] || { echo "arch-check: arch-qube ${VERSION} image not available from ghcr.io or arcana.boo — cannot check (this is NOT a pass)" >&2; exit 2; }

cleanup() { docker rm -fv "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT

docker create --name "$NAME" -v /src -v /output "$IMAGE" \
    scan /src --framework "$FRAMEWORK" --no-ai --ci \
    --format json,markdown -o /output --threshold "$THRESHOLD" >/dev/null \
    || { echo "arch-check: could not create the arch-qube container (image $IMAGE) — NOT a pass" >&2; exit 2; }

# macOS bsdtar carries extended attributes (com.apple.provenance) that docker cp refuses.
if tar --version 2>/dev/null | grep -q bsdtar; then
    TAR_NOXATTR=(--no-xattrs --no-mac-metadata)
    export COPYFILE_DISABLE=1
else
    TAR_NOXATTR=(--no-xattrs)
fi
# Same exclusions as the Jenkinsfile stage.
tar "${TAR_NOXATTR[@]}" --exclude=./.git --exclude=./app/build --exclude=./build \
    --exclude=./.gradle --exclude=./.scannerwork --exclude=./arch-qube-reports \
    --exclude=./buildSrc/build --exclude=./buildSrc/.gradle \
    -C "$ROOT" -cf - . | docker cp - "$NAME":/src

set +e
docker start -a "$NAME"
RC=$?
set -e

mkdir -p "$ROOT/arch-qube-reports"
docker cp "$NAME":/output/. "$ROOT/arch-qube-reports/" >/dev/null 2>&1 || true

if [ "$RC" -eq 0 ]; then
    echo "arch-check: PASS ($IMAGE; report: arch-qube-reports/)"
else
    echo "arch-check: FAIL rc=$RC — --ci prints nothing on failure: read arch-qube-reports/arch-qube.md. Fix the structure; never lower the threshold (AGENTS.md §5)" >&2
fi
exit "$RC"
