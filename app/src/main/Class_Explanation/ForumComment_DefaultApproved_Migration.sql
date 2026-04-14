ALTER TABLE forum_comment
MODIFY review_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

-- Optional: migrate historical pending comments to approved
-- UPDATE forum_comment
-- SET review_status = 'APPROVED',
--     review_remark = 'auto migrated to approved'
-- WHERE review_status = 'PENDING';
