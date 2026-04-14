# ForumPost Search API

## 1. Controller API
- Path: `src/main/java/com/luankin/luankinstation/forum/ForumPost.java`
- Endpoint: `GET /api/forum/posts/search`
- Auth: JWT required (`Authorization: Bearer <token>`)
- Purpose: Search approved forum posts with keyword and optional filters.
- Response type: `PageResult<ForumPostSummaryResponse>`

## 2. Query Parameters
- `keyword` (optional, string): fuzzy keyword match.
- `postType` (optional, enum): `PROP_SHARE | TACTIC_SHARE | DAILY_CHAT | OTHER`
- `mapName` (optional, string): fuzzy map name match.
- `createdByUsername` (optional, string): fuzzy author username match.
- `page` (optional, int, default `0`): page index, min `0`.
- `size` (optional, int, default `20`): page size, clamped to `1 ~ 100`.

## 3. Search Rules
- Only posts with `reviewStatus = APPROVED` are returned.
- `keyword` performs case-insensitive fuzzy match on:
  - `mapName`
  - `propName`
  - `propPosition`
  - `tacticName`
  - `tacticType`
  - `member1/member1Role ... member5/member5Role`
- `mapName` and `createdByUsername` are also case-insensitive fuzzy matches.
- Map alias expansion is enabled for `mapName` and `keyword`.
- Chinese and English map names can match each other (example: searching Chinese alias of Mirage can return posts with `mapName=Mirage`).
- Filters are combined with AND.
- Keyword field matching is OR across supported fields.
- Sort order is `createdAt DESC`.

## 4. Request Example
```http
GET /api/forum/posts/search?keyword=banana&postType=PROP_SHARE&mapName=Mirage&page=0&size=10
Authorization: Bearer <token>
```

## 5. Success Response Example (200)
```json
{
  "items": [
    {
      "id": 101,
      "postType": "PROP_SHARE",
      "title": "B smoke lineup",
      "mapName": "Mirage",
      "createdByUsername": "luankin",
      "reviewStatus": "APPROVED",
      "createdAt": "2026-03-27T10:25:12"
    }
  ],
  "page": 0,
  "size": 10,
  "total": 1,
  "totalPages": 1
}
```

## 6. Error Responses
- `400 Bad Request`: invalid query value (for example, unknown `postType`).
- `401 Unauthorized`: missing or invalid JWT token.

## 7. Related Classes
- Service: `src/main/java/com/luankin/luankinstation/forum/service/ForumPostService.java`
- Repository: `src/main/java/com/luankin/luankinstation/forum/repository/ForumPostRepository.java`
- Summary DTO: `src/main/java/com/luankin/luankinstation/forum/dto/ForumPostSummaryResponse.java`
- Page wrapper: `src/main/java/com/luankin/luankinstation/forum/dto/PageResult.java`
