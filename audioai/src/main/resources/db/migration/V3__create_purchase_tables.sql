-- Table: orders
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    story_id BIGINT NOT NULL,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) DEFAULT 'WALLET',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_story_id (story_id),
    INDEX idx_order_number (order_number),
    INDEX idx_created_at (created_at)
);

-- Table: story_ownerships
CREATE TABLE IF NOT EXISTS story_ownerships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    story_id BIGINT NOT NULL,
    order_id BIGINT,
    purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,
    UNIQUE KEY uk_user_story (user_id, story_id),
    INDEX idx_user_id (user_id),
    INDEX idx_story_id (story_id)
);

-- Table: transactions
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_number VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    balance_before DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    balance_after DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    description TEXT,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_transaction_number (transaction_number),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at),
    INDEX idx_reference (reference_type, reference_id)
);
