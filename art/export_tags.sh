#!/bin/bash
aseprite="D:/Program Files (x86)/SteamLibrary/steamapps/common/Aseprite/Aseprite.exe"
fileName=""
excludeFiles=()
for file in ./ase/*.aseprite; do
  fileName="${file##*/}"
  fileName="${fileName%.*}"
  # shellcheck disable=SC2076
  # shellcheck disable=SC2199
  if [[ ! "${excludeFiles[@]}" =~ "${fileName}" ]]; then
    # shellcheck disable=SC1083
    "$aseprite" -b "$file" --save-as ./export_tiles/"${fileName}"{tag}0.png
  fi
done
