#!/bin/sh
# Gradle wrapper stub — replace with full script from a Gradle distribution
# once Gradle is installed (`gradle wrapper`), or download gradle-wrapper.jar.
# For now delegates to system gradle if present, else prints help.
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "Gradle wrapper jar not yet installed."
  echo "Install Android Studio or run: brew install gradle && gradle wrapper"
  exit 1
fi
