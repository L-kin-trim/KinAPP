# StationMessage Classes

## 1. Controller: `StationMessageApi`
- Path: `src/main/java/com/luankin/luankinstation/message/StationMessageApi.java`
- Base URL: `/api/messages`
- APIs:
  - `POST /api/messages` send a message
  - `GET /api/messages/inbox` inbox list (paged)
  - `GET /api/messages/sent` sent list (paged)

## 2. Service: `StationMessageService`
- Path: `src/main/java/com/luankin/luankinstation/message/service/StationMessageService.java`
- Responsibilities:
  - Validate recipient exists
  - Save message with sender, recipient, content, sent time
  - Query inbox and sent list by current user

## 3. Entity: `StationMessage`
- Path: `src/main/java/com/luankin/luankinstation/message/entity/StationMessage.java`
- Table: `station_message`
- Fields:
  - `id`
  - `sender_username`
  - `recipient_username`
  - `content`
  - `sent_at`

## 4. Repository: `StationMessageRepository`
- Path: `src/main/java/com/luankin/luankinstation/message/repository/StationMessageRepository.java`
- Methods:
  - `findByRecipientUsernameOrderBySentAtDesc`
  - `findBySenderUsernameOrderBySentAtDesc`

## 5. DTOs
- Request: `SendStationMessageRequest`
  - Path: `src/main/java/com/luankin/luankinstation/message/dto/SendStationMessageRequest.java`
  - Fields: `recipientUsername`, `content`
- Response: `StationMessageResponse`
  - Path: `src/main/java/com/luankin/luankinstation/message/dto/StationMessageResponse.java`
  - Fields: `id`, `senderUsername`, `recipientUsername`, `content`, `sentAt`
- Page wrapper: `MessagePageResult`
  - Path: `src/main/java/com/luankin/luankinstation/message/dto/MessagePageResult.java`
