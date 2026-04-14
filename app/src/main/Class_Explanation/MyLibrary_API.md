# My Library API

## 1. Overview
- Module purpose: personal prop/tactic library.
- Visibility: only current logged-in user can access their own data.
- Auth: all endpoints require JWT (`Authorization: Bearer <token>`).
- Supported types: `PROP_SHARE`, `TACTIC_SHARE`.

## 2. Create Personal Item
- Endpoint: `POST /api/my/library/items`
- Purpose: create your own prop/tactic record (not public in forum).
- Request body:
```json
{
  "postType": "PROP_SHARE",
  "mapName": "Mirage",
  "propName": "A smoke",
  "toolType": "SMOKE_GRENADE",
  "throwMethod": "run + jumpthrow",
  "propPosition": "T spawn left wall",
  "stanceImageUrl": "https://xx/stance.png",
  "aimImageUrl": "https://xx/aim.png",
  "landingImageUrl": "https://xx/land.png"
}
```
- Validation rules:
  - `postType=PROP_SHARE`: `propName/mapName/toolType/throwMethod/propPosition/stanceImageUrl/aimImageUrl/landingImageUrl` required.
  - `postType=TACTIC_SHARE`: `tacticName/mapName/tacticType/tacticDescription/member1..member5/member1Role..member5Role` required.
  - only `PROP_SHARE` and `TACTIC_SHARE` are accepted.
- Response: `201 Created`, `MyLibraryItemResponse`.

## 3. List Personal Items
- Endpoint: `GET /api/my/library/items`
- Query:
  - `postType` (optional): `PROP_SHARE | TACTIC_SHARE`
  - `page` (optional, default `0`)
  - `size` (optional, default `20`, range `1~100`)
- Response: `PageResult<MyLibraryItemResponse>`.
- Notes: only current user's own created records are returned.

## 4. Favorite Forum Post
- Endpoint: `POST /api/my/library/favorites/{postId}`
- Purpose: favorite someone else's forum prop/tactic post.
- Rules:
  - forum post must exist and `reviewStatus=APPROVED`.
  - forum post type must be `PROP_SHARE` or `TACTIC_SHARE`.
  - cannot favorite your own forum post.
  - if a favorite row exists but is soft-deleted, this call restores that row (no new row).
- Response: `MyFavoriteStatusResponse`.
```json
{
  "id": 10,
  "forumPostId": 101,
  "favorited": true,
  "updatedAt": "2026-03-27T17:00:00",
  "deletedAt": null
}
```

## 5. Unfavorite Forum Post (Soft Delete)
- Endpoint: `DELETE /api/my/library/favorites/{postId}`
- Purpose: cancel favorite by soft delete.
- Behavior:
  - sets `deleted=true` and writes `deletedAt`.
  - keeps old row for future restore.
- Response: `MyFavoriteStatusResponse` with `favorited=false`.

## 6. List Favorite Items
- Endpoint: `GET /api/my/library/favorites`
- Query:
  - `postType` (optional): `PROP_SHARE | TACTIC_SHARE`
  - `page` (optional, default `0`)
  - `size` (optional, default `20`, range `1~100`)
- Response: `PageResult<MyLibraryItemResponse>`.
- Notes:
  - only active favorites (`deleted=false`) are returned.
  - favorite content is read from forum post data.

## 7. DTO Summary
- `CreateMyLibraryItemRequest`: create personal prop/tactic data.
- `MyLibraryItemResponse`: unified item response for personal items and favorites.
  - `sourceType`: `SELF_CREATED | FAVORITE_FORUM`
  - `forumPostId`: only set for favorites.
- `MyFavoriteStatusResponse`: favorite/unfavorite status result.

## 8. Main Code Paths
- Controller: `src/main/java/com/luankin/luankinstation/mylibrary/MyLibraryApi.java`
- Service: `src/main/java/com/luankin/luankinstation/mylibrary/service/MyLibraryService.java`
- Repositories:
  - `src/main/java/com/luankin/luankinstation/mylibrary/repository/MyLibraryItemRepository.java`
  - `src/main/java/com/luankin/luankinstation/mylibrary/repository/MyLibraryFavoriteRepository.java`
- Entities:
  - `src/main/java/com/luankin/luankinstation/mylibrary/entity/MyLibraryItem.java`
  - `src/main/java/com/luankin/luankinstation/mylibrary/entity/MyLibraryFavorite.java`
