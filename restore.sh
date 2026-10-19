#!/usr/bin/env bash
# Restore the realme TV (SW4H_FF, Android 11) to the state captured in ./backup on 2026-10-19.
# Safe to run any number of times.
# Usage: ./restore.sh [target]   target = ip:port (network) or adb serial (USB, from `adb devices`)
#        default 192.168.1.128:5555
set -u
cd "$(dirname "$0")"
command -v adb >/dev/null || PATH="$HOME/Android/Sdk/platform-tools:$PATH" # system android-tools may be gone; SDK copy works
TV="${1:-192.168.1.128:5555}"
[[ "$TV" == *:* ]] && adb connect "$TV" >/dev/null
A() { adb -s "$TV" shell "$@"; }
pk() { sed 's/^package://' "backup/$1"; }

echo "== Re-installing / re-enabling every package that was enabled originally"
for p in $(pk packages_enabled.txt); do
  A cmd package install-existing --user 0 "$p" >/dev/null 2>&1
  A pm enable --user 0 "$p" >/dev/null 2>&1
done

echo "== Re-disabling packages that were already disabled originally"
for p in $(pk packages_disabled.txt); do
  A cmd package install-existing --user 0 "$p" >/dev/null 2>&1
  A pm disable-user --user 0 "$p" >/dev/null 2>&1
done

echo "== Revoking access granted to Glass (uninstalling clears it as well)"
A cmd notification disallow_listener com.geekyabs.glass/com.geekyabs.glass.MediaListener >/dev/null 2>&1
A appops set com.geekyabs.glass GET_USAGE_STATS default >/dev/null 2>&1

echo "== Uninstalling apps added during de-bloat (not present originally)"
ADDED=(com.geekyabs.glass)   # appended to as apps get installed
for p in "${ADDED[@]}"; do A pm uninstall "$p"; done

echo "== Restoring stock launcher as Home"
A cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity

echo "== Restoring changed settings"
# appended to as settings get changed: A settings put <ns> <key> <original value>
A settings put secure enabled_notification_listeners com.google.android.tvrecommendations/com.google.android.tvrecommendations.NotificationsService

echo "Done. Rebooting TV."; adb -s "$TV" reboot
