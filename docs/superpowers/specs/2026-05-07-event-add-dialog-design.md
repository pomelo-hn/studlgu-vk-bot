# Event Add Dialog Design

## Goal

Make "Add event" a step-by-step editor flow instead of requiring one pipe-separated message, and allow optional time, description, and location.

## User Flow

1. Editor presses "Add event".
2. Bot asks for the event title and shows the cancel keyboard.
3. Bot asks for the event date in `YYYY-MM-DD`.
4. Bot asks for time in `HH:mm`; the user may press or type "Skip".
5. Bot asks for description; the user may skip.
6. Bot asks for location; the user may skip.
7. Bot saves the event and restores the standard keyboard.
8. At any step, "Cancel" clears the current state. For add-event drafts it also clears the draft data.

## Architecture

Use the existing callback route:

`CallbackService -> addEventInputHandler`

Add granular user states for each event input step and a small in-memory `EventDraftCache` keyed by VK user id. Keep the draft cache separate from `UserStateCache` so the state cache stays generic and existing handlers remain simple.

`AddEventCommandHandler` starts the flow by creating a draft, setting the title state, and sending the cancel keyboard. `AddEventInputHandler` advances the draft based on the current state and validates only the current field. `CancelCommandHandler` clears both user state and draft data.

## Optional Fields

Only title and date are required. Time, description, and location are stored as `null` when skipped or blank.

Sorting and detail rendering must tolerate `Event.time == null`. Untimed events sort after timed events on the same date and do not render the time line.

## Testing

Add focused unit tests for:

- `EventService` saving an event with only required fields.
- `CallbackService` routing every add-event step to `add_event_input`.
- `CancelCommandHandler` clearing add-event draft data.
- `AddEventInputHandler` advancing title/date/time/description/location steps, validating bad dates/times, and saving after the last step.
