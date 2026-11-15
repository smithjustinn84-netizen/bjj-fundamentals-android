# Differences from the web app

The web app (`../bjj`) is the reference. This app matches it except where listed here.

## Layout

- From 600dp wide the five tabs are a rail down the left, where the web app has a top bar from 600px to 1099px. The rail widens to show the brand and labels from 1200dp (web: 1100px).
- Lists run in two columns from 840dp (web: 900px).
- Controls are at least 48dp tall whatever the pointer. The web app shrinks them for a mouse.

- The position map responds to a tap, not to a pointer resting on a box, and its arrows do not draw themselves in when the page opens.
- The Positions page shows its details under the map at every width, where the web app puts them beside it from 700px.
- A review session is a page of its own, so Back leaves it. There are no keyboard shortcuts for revealing and grading.
- Wide windows keep the phone's single column inside each page for now; only the navigation and the map change.

## Forms

- The training log's day is typed as year-month-day; there is no calendar picker.
- "New cards this session" is the app's own slider.
- A form's working copy is lost if Android ends the app while it is in the background. A review session starts again from what is due.
- Choices that the web app offers in a menu open as a list in a dialog.

## Text

- Screen text is in the code, in English, as in the web app. Other apps by the same author keep theirs in string resources; moving it there is a follow-up.
- Dates are written in English ("Sat 14 Nov 2026") whatever the device's language.

## Data

- A video bookmark's start time is a whole number of seconds. The web app's own forms only ever store whole seconds.
- A time too long to be a time (more than about 68 years of seconds) is rejected by `parseTime`, where the web app would accept it.
- Fields this app does not know in an imported backup are dropped, where the web app keeps them. Both apps reject a backup from a newer schema version, so this only affects hand-edited files.

## Platform

- The manifest asks for `INTERNET` (YouTube's player) and `ACCESS_NETWORK_STATE` (to show the drawing instead when offline). Neither prompts the user.
- Backups are saved and opened through the system's file picker.
- The system's status and navigation icons follow the app's theme, not the device's.
- Starter text that changes in a later version does not reach a library that already holds the item (the web app once refreshed unedited items in a one-off migration).
