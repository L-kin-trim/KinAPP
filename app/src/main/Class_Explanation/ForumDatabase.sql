CREATE DATABASE IF NOT EXISTS luankinstation
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE luankinstation;

CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS forum_post (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_type VARCHAR(20) NOT NULL,
  map_name VARCHAR(50) NULL,
  prop_name VARCHAR(100) NULL,
  tool_type VARCHAR(30) NULL,
  throw_method TEXT NULL,
  prop_position VARCHAR(255) NULL,
  stance_image_url VARCHAR(500) NULL,
  aim_image_url VARCHAR(500) NULL,
  landing_image_url VARCHAR(500) NULL,
  tactic_name VARCHAR(100) NULL,
  tactic_type VARCHAR(50) NULL,
  tactic_description TEXT NULL,
  member1 VARCHAR(50) NULL,
  member1_role VARCHAR(50) NULL,
  member2 VARCHAR(50) NULL,
  member2_role VARCHAR(50) NULL,
  member3 VARCHAR(50) NULL,
  member3_role VARCHAR(50) NULL,
  member4 VARCHAR(50) NULL,
  member4_role VARCHAR(50) NULL,
  member5 VARCHAR(50) NULL,
  member5_role VARCHAR(50) NULL,
  content TEXT NULL,
  created_by_username VARCHAR(50) NOT NULL,
  review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  review_remark VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_post_status_created (review_status, created_at),
  INDEX idx_post_type_status_created (post_type, review_status, created_at),
  INDEX idx_post_creator (created_by_username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS forum_post_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  sort_order INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_post_image_post FOREIGN KEY (post_id) REFERENCES forum_post(id) ON DELETE CASCADE,
  INDEX idx_post_image_post_sort (post_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS forum_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  floor_number INT NOT NULL,
  content TEXT NOT NULL,
  username VARCHAR(50) NOT NULL,
  review_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  review_remark VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES forum_post(id) ON DELETE CASCADE,
  UNIQUE KEY uk_comment_post_floor (post_id, floor_number),
  INDEX idx_comment_status_created (review_status, created_at),
  INDEX idx_comment_post_status_floor (post_id, review_status, floor_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS forum_comment_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  comment_id BIGINT NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  sort_order INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_comment_image_comment FOREIGN KEY (comment_id) REFERENCES forum_comment(id) ON DELETE CASCADE,
  INDEX idx_comment_image_comment_sort (comment_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS station_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender_username VARCHAR(50) NOT NULL,
  recipient_username VARCHAR(50) NOT NULL,
  content TEXT NOT NULL,
  sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_station_message_sender
    FOREIGN KEY (sender_username) REFERENCES `user`(username)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_station_message_recipient
    FOREIGN KEY (recipient_username) REFERENCES `user`(username)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_station_message_recipient_time (recipient_username, sent_at),
  INDEX idx_station_message_sender_time (sender_username, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
