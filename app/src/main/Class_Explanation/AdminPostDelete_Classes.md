# Admin Delete Post (Soft Delete to Rejected)

## API
- Method: `DELETE`
- Path: `/api/forum/posts/{postId}/admin`
- Auth: `ROLE_ADMIN` required
- Behavior: convert post review status to `REJECTED` even if current status is `APPROVED`

Optional request body:
```json
{
  "reason": "违规内容，管理员删除"
}
```

Success response:
- Returns full `ForumPostResponse`
- `reviewStatus` will be `REJECTED`
- `reviewRemark` will be request reason (or default `"deleted by admin"`)

## Code Paths
- Controller:
  - `src/main/java/com/luankin/luankinstation/forum/ForumPost.java`
- Service:
  - `src/main/java/com/luankin/luankinstation/forum/service/ForumPostService.java`
- DTO:
  - `src/main/java/com/luankin/luankinstation/forum/dto/AdminDeletePostRequest.java`
