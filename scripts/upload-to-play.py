#!/usr/bin/env python3
"""Upload a release AAB to a Google Play track via the Play Developer API.

Defaults to the `alpha` (closed testing) track. Reads the service-account
JSON from ~/.bowpress-secrets/play-billing-key.json by default (override
with $PLAY_SERVICE_ACCOUNT_JSON or --service-account).

Pipeline:
  1. edits.insert  → opens an Edit transaction.
  2. edits.bundles.upload  → posts the .aab bytes (resumable upload).
  3. edits.tracks.update  → assigns the bundle's versionCode to the track,
                            optionally with a release-notes block.
  4. edits.commit  → finalizes the Edit (the moment Play rolls it out
                     to the track audience).

Usage:
    scripts/upload-to-play.py [--track alpha|internal|beta|production]
        [--aab PATH] [--package com.andrewnguyen.bowpress]
        [--release-name NAME] [--whats-new TEXT | --whats-new-file PATH]
        [--dry-run]

Defaults: --track alpha, --aab app/build/outputs/bundle/release/app-release.aab,
--package com.andrewnguyen.bowpress, --release-name = the bundle's versionName.
"""
import argparse
import json
import os
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
DEFAULT_AAB = REPO / "app" / "build" / "outputs" / "bundle" / "release" / "app-release.aab"
DEFAULT_SA = Path.home() / ".bowpress-secrets" / "play-billing-key.json"
DEFAULT_PACKAGE = "com.andrewnguyen.bowpress"

try:
    from google.oauth2 import service_account
    from googleapiclient.discovery import build
    from googleapiclient.http import MediaFileUpload
    from googleapiclient.errors import HttpError
except ImportError:
    sys.exit("missing deps — run: pip3 install --user --break-system-packages "
             "google-api-python-client google-auth")

SCOPE = "https://www.googleapis.com/auth/androidpublisher"


def log(msg: str) -> None:
    print(f"[upload-to-play] {msg}", flush=True)


def authed_service(sa_path: Path):
    if not sa_path.exists():
        sys.exit(f"service account JSON not found: {sa_path}")
    creds = service_account.Credentials.from_service_account_file(
        str(sa_path), scopes=[SCOPE],
    )
    # cache_discovery=False silences a noisy fs cache warning under
    # google-api-python-client; safe on every modern install.
    return build("androidpublisher", "v3", credentials=creds, cache_discovery=False)


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--track", default="alpha",
                    help="Play track: internal | alpha | beta | production (default: alpha).")
    ap.add_argument("--aab", default=str(DEFAULT_AAB),
                    help=f"Path to the .aab (default: {DEFAULT_AAB.relative_to(REPO)}).")
    ap.add_argument("--package", default=DEFAULT_PACKAGE,
                    help=f"Application id (default: {DEFAULT_PACKAGE}).")
    ap.add_argument("--service-account",
                    default=os.environ.get("PLAY_SERVICE_ACCOUNT_JSON", str(DEFAULT_SA)),
                    help=f"Service-account JSON (default: {DEFAULT_SA}).")
    ap.add_argument("--release-name",
                    help="Human-readable release name on the track (defaults to the bundle's "
                         "versionName as reported by the upload response).")
    grp = ap.add_mutually_exclusive_group()
    grp.add_argument("--whats-new", help="en-US release notes inline.")
    grp.add_argument("--whats-new-file", help="Path to a file with the en-US release notes.")
    ap.add_argument("--dry-run", action="store_true",
                    help="Open an edit + upload but DON'T commit. Edit is discarded.")
    args = ap.parse_args()

    aab = Path(args.aab)
    if not aab.exists():
        sys.exit(f"aab not found: {aab} — build it with ./gradlew :app:bundleRelease")

    whats_new_text = args.whats_new
    if args.whats_new_file:
        whats_new_text = Path(args.whats_new_file).read_text().strip()

    svc = authed_service(Path(args.service_account).expanduser())
    edits = svc.edits()

    log(f"opening Edit for {args.package}")
    edit = edits.insert(packageName=args.package, body={}).execute()
    edit_id = edit["id"]

    log(f"uploading {aab.name} ({aab.stat().st_size / 1024 / 1024:.1f} MB)")
    media = MediaFileUpload(str(aab),
                            mimetype="application/octet-stream",
                            resumable=True)
    try:
        upload = edits.bundles().upload(
            packageName=args.package, editId=edit_id, media_body=media,
        ).execute()
    except HttpError as e:
        try:
            err = json.loads(e.content)["error"]
            log(f"upload FAILED: {err.get('code')} {err.get('status')} — {err.get('message')}")
        except Exception:
            log(f"upload FAILED: {e}")
        sys.exit(1)
    version_code = upload["versionCode"]
    log(f"upload OK → versionCode={version_code} sha1={upload.get('sha1','')[:12]}…")

    log(f"assigning versionCode={version_code} to track={args.track}")
    release_body = {
        "name": args.release_name or str(version_code),
        "versionCodes": [str(version_code)],
        "status": "completed",
    }
    if whats_new_text:
        release_body["releaseNotes"] = [
            {"language": "en-US", "text": whats_new_text},
        ]
    edits.tracks().update(
        packageName=args.package, editId=edit_id, track=args.track,
        body={"track": args.track, "releases": [release_body]},
    ).execute()

    if args.dry_run:
        log("--dry-run: NOT committing the Edit; it'll expire on the server.")
        return
    edits.commit(packageName=args.package, editId=edit_id).execute()
    log(f"committed — versionCode {version_code} is live on track={args.track}.")


if __name__ == "__main__":
    main()
