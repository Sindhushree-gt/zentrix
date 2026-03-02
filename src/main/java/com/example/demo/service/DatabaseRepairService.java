package com.example.demo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseRepairService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    @Transactional
    public void repairDatabase() {
        System.out.println("[DB-REPAIR] Starting database orphaned record cleanup...");

        try {
            // 1. Clean up child tables first

            // message_read_receipt depends on chat_message and users
            jdbcTemplate.execute(
                    "DELETE FROM message_read_receipt WHERE message_id NOT IN (SELECT id FROM chat_message) OR user_id NOT IN (SELECT id FROM users)");

            // message_reactions depends on chat_message and users
            jdbcTemplate.execute(
                    "DELETE FROM message_reactions WHERE message_id NOT IN (SELECT id FROM chat_message) OR user_id NOT IN (SELECT id FROM users)");

            // conversation_participants depends on conversation and users
            jdbcTemplate.execute(
                    "DELETE FROM conversation_participants WHERE conversation_id NOT IN (SELECT id FROM conversation) OR user_id NOT IN (SELECT id FROM users)");

            // 2. Clean up major entities

            // chat_message depends on sender_id (users)
            jdbcTemplate.execute(
                    "DELETE FROM chat_message WHERE sender_id NOT IN (SELECT id FROM users) AND sender_id IS NOT NULL");

            // conversation depends on creator_id (users)
            jdbcTemplate.execute(
                    "DELETE FROM conversation WHERE creator_id NOT IN (SELECT id FROM users) AND creator_id IS NOT NULL");

            // 3. Clean up events
            try {
                jdbcTemplate.execute(
                        "DELETE FROM event_registration WHERE event_id NOT IN (SELECT id FROM event) OR user_id NOT IN (SELECT id FROM users)");
                jdbcTemplate.execute(
                        "DELETE FROM event WHERE creator_id NOT IN (SELECT id FROM users) AND creator_id IS NOT NULL");
            } catch (Exception e) {
                // Table might not exist yet
            }

            System.out.println("[DB-REPAIR] Database cleanup completed successfully.");
        } catch (Exception e) {
            System.err.println("[DB-REPAIR] Error during database cleanup: " + e.getMessage());
        }
    }
}
