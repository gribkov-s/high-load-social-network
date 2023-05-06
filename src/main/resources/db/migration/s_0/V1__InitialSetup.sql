USE social_network;

#----------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS user_profile (
    user_id VARCHAR(36) NOT NULL PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    age INT(100) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    city VARCHAR(50) NOT NULL,
    interests JSON NOT NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS user_auth (
    user_id VARCHAR(36) NOT NULL PRIMARY KEY REFERENCES user_profile(user_id),
    login VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(32)
) ENGINE = InnoDB;

#----------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS user_friendship (
    user_id VARCHAR(36) NOT NULL,
    login VARCHAR(50) NOT NULL,
    friend_id VARCHAR(36) NOT NULL,
    friend_login VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES user_profile(user_id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES user_profile(user_id) ON DELETE CASCADE
) ENGINE = InnoDB;

#----------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS user_post (
    post_id VARCHAR(36) NOT NULL PRIMARY KEY,
    post_time TIMESTAMP(3) NOT NULL,
    publisher_id VARCHAR(36) NOT NULL,
    publisher_login VARCHAR(50) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    FOREIGN KEY (publisher_id) REFERENCES user_profile(user_id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_publisher ON user_post (publisher_id);

CREATE TABLE IF NOT EXISTS user_post_sending (
    post_id VARCHAR(36) NOT NULL,
    post_time TIMESTAMP(3) NOT NULL,
    publisher_id VARCHAR(36) NOT NULL,
    subscriber_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (post_id, subscriber_id),
    FOREIGN KEY (post_id) REFERENCES user_post(post_id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_subscriber ON user_post_sending (subscriber_id);

#----------------------------------------------------------------------------------------------