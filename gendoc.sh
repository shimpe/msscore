#!/bin/sh
# Regenerate the MSScore HelpSource *.schelp files from the class sources using whelk.
# Set WHELK to whelk.py on your machine (defaults to the Panola quark's location).

HERE="$(cd "$(dirname "$0")" && pwd)"
WHELK="${WHELK:-$HOME/development/music/whelk/whelk.py}"

rm -f "$HERE/HelpSource/Classes/"*.schelp

python "$WHELK" -i "$HERE/Classes/"*.sc -o "$HERE/HelpSource/Classes/"
