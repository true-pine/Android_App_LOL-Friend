package com.capston.lolfriend.model;

import java.util.HashMap;
import java.util.Map;

public class ChatModel {

    public Map<String, Boolean> users = new HashMap<>(); //채팅방 유저들
    public Map<String, Comment> comments = new HashMap<>(); //채팅방의 대화내용
    public Info info;

    public static class Comment {
        public String uid;
        public String message;
        public Object timestamp;
        public String nickname;
        public String profileUrl;
        public Map<String, Object> readUsers = new HashMap<>();
    }

    public static class Info {
        public String roomName;
        public String gameType;
        public String roomTier;
        public String roomDescription;
        public String roomId;
        public String roomType;
    }
}
