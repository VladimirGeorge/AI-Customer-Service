-- 用户表
CREATE DATABASE IF NOT EXISTS ai_customer_service;
USE ai_customer_service;
-- 用户表
CREATE DATABASE IF NOT EXISTS ai_customer_service;
USE ai_customer_service;
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
                        `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                        `username` VARCHAR(50) NOT NULL UNIQUE,
                        `password` VARCHAR(100) NOT NULL,
                        `create_time` DATETIME NOT NULL,
                        `update_time` DATETIME NOT NULL
);

-- 问答历史表
DROP TABLE IF EXISTS `question_history`;
CREATE TABLE `question_history` (
                                    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    `user_id` VARCHAR(50) NOT NULL, -- 修改为 VARCHAR 类型
                                    `session_id` VARCHAR(50) NOT NULL,
                                    `question` TEXT NOT NULL,
                                    `answer` TEXT,
                                    `create_time` DATETIME NOT NULL
);

-- 知识库表
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
                                  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  `content` TEXT NOT NULL,
                                  `category` VARCHAR(50) NOT NULL,
                                  `create_time` DATETIME NOT NULL,
                                  `update_time` DATETIME NOT NULL,
                                  `is_active` TINYINT(1) DEFAULT 1
);

DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                question VARCHAR(255) NOT NULL,  -- 存储问题
                                answer TEXT NOT NULL,            -- 存储答案（内容可能较长，用TEXT）
                                create_time DATETIME NOT NULL,   -- 创建时间
                                update_time DATETIME NOT NULL    -- 更新时间
);

-- 会话表（多轮会话）
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation` (
                                `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                `user_id` BIGINT NOT NULL,
                                `session_id` VARCHAR(50) NOT NULL,
                                `create_time` DATETIME NOT NULL,
                                `last_update_time` DATETIME NOT NULL,
                                FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);