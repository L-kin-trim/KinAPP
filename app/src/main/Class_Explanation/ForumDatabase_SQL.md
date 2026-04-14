# Forum Database SQL Notes

Script file:
- `Class_Explanation/ForumDatabase.sql`

This script creates:
1. Database `luankinstation`
2. User table ``user``
3. Forum post table `forum_post`
4. Forum post image table `forum_post_image`
5. Forum comment table `forum_comment`
6. Forum comment image table `forum_comment_image`
7. Station message table `station_message`
7. Indexes and foreign keys for forum and message modules

Run order:
1. Backup existing database
2. Execute `Class_Explanation/ForumDatabase.sql`
3. Start backend (`spring.jpa.hibernate.ddl-auto=update`)
