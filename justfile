##
# See the manual to add more tasks https://just.systems/man/en/
#
# Note: just lists recipes in alphabetical order.
# Recipes that are related to one-another should share a common prefix,
# making them appear together in the just --list output.
#
# Run `just --fmt --unstable` after editing this file
##


# Terminal colors
NO_COLOR := '\033[0m'
GREEN := '\033[0;32m'


# Global variables
URL := "http://localhost:8888/v1"

log message:
  curl -X POST -H "Content-Type: text/plain" {{ URL }}/log -d "{{ message }}"
