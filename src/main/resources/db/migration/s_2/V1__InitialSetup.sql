USE social_network;

CREATE TABLE IF NOT EXISTS user_dialog (
    message_id VARCHAR(36) NOT NULL,
    v_bucket INT NOT NULL,
    dialog_id VARCHAR(200) NOT NULL,
    message_time TIMESTAMP NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    sender_login VARCHAR(50) NOT NULL,
    delivered BOOLEAN NOT NULL,
    body VARCHAR(1000) NOT NULL,
    PRIMARY KEY msg_bkt_id (message_id, v_bucket)
) ENGINE = InnoDB;

ALTER TABLE user_dialog PARTITION BY HASH(v_bucket) PARTITIONS 128;

CREATE INDEX idx_dialog ON user_dialog (dialog_id);
CREATE INDEX idx_sender ON user_dialog (sender_id);
CREATE INDEX idx_delivered ON user_dialog (delivered);