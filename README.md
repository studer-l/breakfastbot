# Breakfastbot

Every Monday the [Distran](https://distran.ch) team has breakfast together at
the office, with one person responsible for bringing (fresh!) food items.

This logistical challenge obviously mandated the use of **ChatOps**, hence I
created this Zulip chatbot! 😀


## Features

- Allows team members to sign on / off from events
- Chooses a responsible person for each breakfast
- Announce attendees / bringer each Thursday at 10:00

## Scheduling algorithm

Given a set of attendees, the attendee with the highest count of events attended
since last bringing breakfast (or total events attended, if they never brought
breakfast) is chosen to bring breakfast.

## Deployment

Requires a postgres database.
Also requires Zulip bot credentials.
See `test-config.edn` for configuration.
