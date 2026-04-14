-- My Library tables
-- Run on MySQL 8.x

CREATE TABLE IF NOT EXISTS `my_library_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `owner_username` VARCHAR(50) NOT NULL,
  `post_type` ENUM('DAILY_CHAT','OTHER','PROP_SHARE','TACTIC_SHARE') NOT NULL,
  `map_name` VARCHAR(50) DEFAULT NULL,
  `prop_name` VARCHAR(100) DEFAULT NULL,
  `tool_type` ENUM('FLASHBANG','HE_GRENADE','MOLOTOV','SMOKE_GRENADE') DEFAULT NULL,
  `throw_method` TEXT DEFAULT NULL,
  `prop_position` VARCHAR(255) DEFAULT NULL,
  `stance_image_url` VARCHAR(500) DEFAULT NULL,
  `aim_image_url` VARCHAR(500) DEFAULT NULL,
  `landing_image_url` VARCHAR(500) DEFAULT NULL,
  `tactic_name` VARCHAR(100) DEFAULT NULL,
  `tactic_type` VARCHAR(50) DEFAULT NULL,
  `tactic_description` TEXT DEFAULT NULL,
  `member1` VARCHAR(50) DEFAULT NULL,
  `member1_role` VARCHAR(50) DEFAULT NULL,
  `member2` VARCHAR(50) DEFAULT NULL,
  `member2_role` VARCHAR(50) DEFAULT NULL,
  `member3` VARCHAR(50) DEFAULT NULL,
  `member3_role` VARCHAR(50) DEFAULT NULL,
  `member4` VARCHAR(50) DEFAULT NULL,
  `member4_role` VARCHAR(50) DEFAULT NULL,
  `member5` VARCHAR(50) DEFAULT NULL,
  `member5_role` VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_my_library_item_owner_type_created` (`owner_username`, `post_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `my_library_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `owner_username` VARCHAR(50) NOT NULL,
  `forum_post_id` BIGINT NOT NULL,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  `deleted_at` DATETIME(6) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_my_library_owner_post` (`owner_username`, `forum_post_id`),
  KEY `idx_my_library_favorite_owner_deleted_updated` (`owner_username`, `deleted`, `updated_at`),
  KEY `idx_my_library_favorite_post` (`forum_post_id`),
  CONSTRAINT `fk_my_library_favorite_forum_post`
    FOREIGN KEY (`forum_post_id`) REFERENCES `forum_post` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
