#!/usr/bin/env sh
APP_HOME="$(cd "${0%/*}" >/dev/null 2>&1; pwd -P)"
exec gradle "$@"
