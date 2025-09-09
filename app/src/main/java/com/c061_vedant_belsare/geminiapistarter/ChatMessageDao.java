package com.c061_vedant_belsare.geminiapistarter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatMessageDao {

    @Insert
    void insert(ChatMessage message);

    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    List<ChatMessage> getAllMessages();
}
