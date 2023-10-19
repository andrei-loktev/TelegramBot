-- liquibase formatted sql

-- changeset andrey:123
CREATE TABLE notification_task (
    id bigserial primary key,
    chat_id bigint,
    text varchar,
    exec_Date timestamp
)