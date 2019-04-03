# Breakfastbot
[![CircleCI](https://circleci.com/gh/studer-l/breakfastbot/tree/master.svg?style=svg)](https://circleci.com/gh/studer-l/breakfastbot/tree/master) [![codecov](https://codecov.io/gh/studer-l/breakfastbot/branch/master/graph/badge.svg)](https://codecov.io/gh/studer-l/breakfastbot)


Every Monday the [Distran](https://distran.ch) team has breakfast together at
the office, with one person responsible for bringing (fresh!) food items.

This logistical challenge obviously mandated the use of **ChatOps**, hence I
created this Zulip chatbot! 😀


## Features

- Allows team members to sign on / off from events
- Chooses a responsible person for each breakfast
- Announce attendees / bringer each Friday at 15:00


## Deployment

Requires a postgres database.
Also requires Zulip bot credentials.
See `test-config.edn` for configuration.
