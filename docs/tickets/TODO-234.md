# TODO-234: Keep the theme in sync across open tabs

**Type:** Improvement
**Area:** Frontend
**Priority:** Low
**Raised by:** /code-review of TODO-231

## Story

The dashboard stays open all day on the wall screen and on people's second monitors,
often in more than one tab. Since TODO-231 the theme is stored in `localStorage`, but an
open tab does not notice when another tab changes it. A user who switches to dark in one
tab still sees light in the other, and toggling there overwrites the choice made first.

## Acceptance criteria

- **AC-1** When the theme changes in one tab, every other open tab of the dashboard
  switches to it without a refresh (listen for the `storage` event on
  `ops-dashboard-theme`, and validate the value as `app.js` already does).
- **AC-2** A Jest test covers it.

## Why not in TODO-231

TODO-231 asks that a refresh keeps the user's choice (AC-3), which it does. Live
sync between tabs was not in its scope; it is a small, separate improvement.
