#!/bin/sh
set -e
TV_HOST=${TV_HOST:-$1}
if [ -z "$TV_HOST" ]; then
    echo "pass TV_HOST or set TV_HOST=<tv ip>" >&2
    exit 1
fi
here=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd) || exit 1
root=$(dirname "$(dirname "$here")")
tools="$root/tools"
py="$tools/venv/bin/python"
pkg="com.gathin.bingwallpaper"

if [ "$2" = "--uninstall" ]; then
    $py -I "$tools/tv.py" "pm uninstall $pkg"
    echo "Uninstalled $pkg"
    exit 0
fi

(cd "$here" && ./gradlew assembleRelease -q)
apk="$here/app/build/outputs/apk/release/app-release.apk"
$py -I "$tools/b64push.py" "$apk" /data/local/tmp/bing-wallpaper.apk
$py -I "$tools/tv.py" "pm install -r /data/local/tmp/bing-wallpaper.apk; rm -f /data/local/tmp/bing-wallpaper.apk"
$py -I "$tools/tv.py" "pm grant $pkg android.permission.WRITE_SECURE_SETTINGS 2>/dev/null; true"
$py -I "$tools/tv.py" "appops set $pkg AUTO_START allow 2>/dev/null; appops set $pkg APP_AUTO_START allow 2>/dev/null; appops write-settings 2>/dev/null; true"
echo "Installed $pkg and granted permissions."
