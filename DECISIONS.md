# Decisions

Judgment calls made autonomously while building Vaani, per the build brief's
rule #2 (cheap-to-reverse calls where reasonable developers could disagree).

1. **Persistence split**: `Settings` (scalar toggles + the `perApp` map) is
   stored in Jetpack DataStore (Preferences). Vocabulary, Snippets, Models
   and History — all list-shaped, CRUD data with stable ids — are stored in
   a Room database. Both are "fine" per the brief; this split keeps each
   store doing what it's naturally good at rather than forcing one shape
   onto both kinds of data.

2. **Long-press popup hit-testing** (diacritics, `.` suffixes): implemented
   by tracking each popup character's on-screen bounds (via
   `onGloballyPositioned`) and checking the final pointer-up position
   against them, inside the *same* key's `pointerInput` gesture — rather
   than giving each popup character its own independent click handler.
   Compose keeps delivering pointer move/up events to the handler that
   received the initial "down" for that pointer id regardless of where it
   ends up on screen, so this is the natural way to express the prototype's
   tap/hold race (and its "release on the base key falls back to the base
   character" rule) in Compose's gesture model, which doesn't have a direct
   equivalent of the DOM's per-element `pointerup` + `setPointerCapture`.

3. **Per-model "Import…" button**: the companion app's Models screen gives
   each not-yet-installed model its own "Import…" row action (which knows
   which model id the picked file is for) rather than one generic "Import a
   model file…" button below the list that would need to infer which model
   the user meant afterward. Clearer for someone with near-zero technical
   background, and cheap to change back.

4. **Streaming transcription (Moonshine) is implemented as "re-decode the
   growing buffer on each chunk,"** not true incremental/windowed streaming.
   It is simple and correct (same code path as the one-shot case, just
   called more often) but not the most efficient approach, and gets slower
   as an utterance grows long. Flagged in HANDOVER.md as a real follow-up,
   not blocking MVP.

5. **Password fields** (`InputType.TYPE_TEXT_VARIATION_PASSWORD` and
   friends): the suggestion strip is disabled, dictation history logging is
   skipped, and delivery always goes straight to the field (never into the
   review-before-insert holding state) — per the brief's own suggestion in
   the IME ARCHITECTURE section, treated as worth doing rather than a nice-
   to-have.

6. **Undo after a dictation insert** removes exactly as many characters as
   were just committed (`deleteSurroundingText(text.length, 0)`), rather
   than trying to restore a full snapshot of the field the way the
   prototype's simulated field could. `InputConnection` has no "restore
   whole field to an earlier string" operation; this is correct as long as
   the caret hasn't moved since the insertion, which is the only time
   pressing Undo is meaningful.

7. **Launcher icon assets**: the provided 1024px adaptive foreground/
   background PNGs are used directly as `drawable-nodpi` resources (scaled
   by the system, not pre-sliced per density), with `app_icon_192.png`
   placed at `mipmap-xxxhdpi` as the one density bucket provided. Android
   Studio's Image Asset Studio (the brief's suggested tool) isn't available
   in a headless environment; hand-writing the adaptive-icon XML against
   the already-safe-zoned foreground/background, as the brief explicitly
   allows, was the fallback.

8. **whisper.cpp version pin**: `app/src/main/cpp/CMakeLists.txt` pins
   `GIT_TAG v1.7.2` via CMake `FetchContent` rather than tracking a moving
   branch, for reproducible builds. This repository's sandbox could not
   reach github.com (its network policy only allows `dl.google.com`/
   `dl-ssl.google.com`), so this was never actually fetched or built here —
   verify the tag still exists and the build succeeds in CI before relying
   on it. See HANDOVER.md.
