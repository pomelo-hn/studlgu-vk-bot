# Appeals Design

## Goal

Add an appeals flow so every VK user can send a request with optional pictures, and users whose VK manager role is listed in `vkbot.editor-roles` can view open appeals and answer them with text. After an editor answers, the original user receives the response.

## User Flow

1. User presses "Appeal".
2. Bot asks the user to send the appeal text and optionally attach pictures in the same message.
3. Bot saves the appeal with status `OPEN`, restores the standard keyboard, and confirms that the appeal was accepted.
4. Editor presses "Appeals".
5. Bot shows open appeals as buttons with short ids. If there are no open appeals, it sends an empty-list message and restores the standard editor keyboard.
6. Editor chooses an appeal.
7. Bot sends the selected appeal details: short id, author VK id, text, creation time, and picture links when present. Then it asks the editor to send a text answer.
8. Editor sends the answer text.
9. Bot marks the appeal as `ANSWERED`, stores answer metadata, restores the editor keyboard, and sends the answer text to the original appeal author.
10. At any appeal creation or answer step, "Cancel" clears the current state and restores the standard keyboard.

## Scope

Appeal answers are text-only. Users may attach pictures only when creating an appeal. The first version stores VK photo URLs from callback attachments instead of downloading files, because editors only need to inspect the pictures and VK already provides usable attachment URLs in callback payloads.

Appeal creation requires non-blank text. Pictures are optional. If a user starts the flow and then sends only photos without text, the bot asks for text again and keeps the user in the appeal creation state.

## Architecture

Use the existing callback and command route:

`CallbackController -> CallbackService -> ICallbackHandler -> CommandHandlerService -> CommandHandler`

Add:

- `CommandType.CREATE_APPEAL` and `CreateAppealCommandHandler` to start user appeal creation.
- `CommandType.APPEALS` and `AppealsCommandHandler` to show open appeals to editors.
- `CommandType.SELECT_APPEAL` and `SelectAppealCommandHandler` to choose an appeal for answering.
- `AppealInputHandler` for incoming appeal text and editor answer text.
- `Appeal`, `AppealStatus`, `AppealRepository`, and `AppealService` under the existing model and utils style.
- `vkbot.appeals.storage.path` for JSON storage, following `vkbot.events.storage.path`.

Extend `UserState` with:

- `AWAITING_APPEAL_TEXT`
- `AWAITING_APPEAL_ANSWER`

For `AWAITING_APPEAL_ANSWER`, add a small in-memory `AppealAnswerDraftCache` keyed by editor VK user id and containing the selected appeal id. This mirrors `EventDraftCache` and keeps `UserStateCache` generic.

## Data Storage

Appeals are stored as a JSON array in the configured file path. Each appeal contains:

- `id`
- `userId`
- `text`
- `photoUrls`
- `status`
- `createdAt`
- `answeredAt`
- `answeredByUserId`
- `answerText`

`AppealRepository` is responsible only for file IO and serialization. `AppealService` owns ids, status transitions, filtering open appeals, and validation such as non-blank appeal text and answer text.

## Routing

`CallbackService.defineType` routes `message_new` to `appeal_input` when the user state is `AWAITING_APPEAL_TEXT` or `AWAITING_APPEAL_ANSWER`. The existing special handling for `cancel` stays first so cancellation works from both appeal states.

Photo attachments should not automatically route to `uploadPhotoHandler` when the user is creating an appeal. `CallbackService` should check active user state before using the generic `upload_photo` route, so appeal messages with photos are handled by `AppealInputHandler`.

## Keyboard

The standard keyboard gets an "Appeal" button for all users. The editor-only section gets an "Appeals" button.

Add an appeals list keyboard that renders open appeal ids in rows and includes "Cancel". Selecting a button sends payload:

`{"command": "select_appeal", "appeal_id": "<id>"}`

Extend `Payload` with `appealId`.

## Permissions

Only editor-role users can list and select appeals. `AppealsCommandHandler` and `SelectAppealCommandHandler` both check `RoleIdentifier.hasEditorRights(...)`. If a non-editor triggers those commands manually, the bot declines and restores the normal keyboard.

Any user can create an appeal.

## Error Handling

If an editor selects an appeal that no longer exists or is already answered, the bot sends a clear message and shows the current open appeals list again.

If sending the final answer to the original user fails through VK API, the handler should surface the error as a runtime exception like existing handlers. The appeal should be marked answered only after a successful send to the original user, so the appeal is not silently lost.

## Testing

Add focused tests for:

- `AppealService` creating appeals, listing only open appeals, and marking an appeal answered.
- `CallbackService` routing appeal states to `appeal_input`, including messages with photo attachments.
- `StandardKeyboard` showing "Appeal" for all users and "Appeals" only for editors.
- `CancelCommandHandler` clearing appeal answer draft data.
- `AppealInputHandler` validating blank user text, saving appeal text with photo URLs, validating blank editor answers, sending answer to original user, and marking the appeal answered after successful send.
