# JG Nexus — College Social Network Platform

## Tech Stack
| Layer | Technology |
|-------|-----------|
| Backend | Java 17 + Spring Boot 3.2 |
| Database | PostgreSQL |
| Cache | Redis (Spring Cache + RedisTemplate) |
| Real-time Chat | WebSocket + STOMP |
| Security | Spring Security + JWT (jjwt 0.12) |
| Frontend | HTML / CSS / JS (or swap for React) |

---

## Project Structure

```
jg-nexus/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/jgnexus/
│       ├── JgNexusApplication.java
│       ├── config/
│       │   ├── SecurityConfig.java          ← Spring Security + CORS + JWT filter chain
│       │   ├── WebSocketConfig.java         ← STOMP endpoints + JWT channel interceptor
│       │   ├── RedisConfig.java             ← Redis template + cache manager
│       │   └── GlobalExceptionHandler.java  ← REST error handling
│       ├── entity/
│       │   ├── User.java                    ← Users with followers/following ManyToMany
│       │   ├── Post.java                    ← Posts with likes ManyToMany, comments
│       │   ├── Comment.java                 ← Threaded comments (parent/child)
│       │   ├── ChatMessage.java             ← Persisted chat messages
│       │   └── Notification.java            ← Push notifications
│       ├── dto/
│       │   └── Dtos.java                    ← All request/response DTOs
│       ├── repository/
│       │   ├── UserRepository.java
│       │   ├── PostRepository.java          ← Feed query, trending, search
│       │   ├── CommentRepository.java
│       │   ├── ChatMessageRepository.java
│       │   └── NotificationRepository.java
│       ├── service/
│       │   ├── AuthService.java             ← Register, login, refresh token
│       │   ├── UserService.java             ← Profile, follow, search (Redis cached)
│       │   ├── PostService.java             ← CRUD, feed, like, comments (Redis cached)
│       │   ├── ChatService.java             ← Messages, online status (Redis)
│       │   └── NotificationService.java     ← Push via WebSocket + persist
│       ├── controller/
│       │   ├── AuthController.java          ← POST /auth/register, /login, /refresh
│       │   ├── UserController.java          ← GET/PUT /users, follow, search
│       │   ├── PostController.java          ← CRUD /posts, feed, like, comments
│       │   ├── ChatController.java          ← @MessageMapping /chat + REST /chat
│       │   └── NotificationController.java  ← GET /notifications
│       └── security/
│           ├── JwtTokenProvider.java        ← Generate, validate, extract JWT
│           ├── JwtAuthenticationFilter.java ← OncePerRequestFilter for all routes
│           └── UserDetailsServiceImpl.java  ← Load user from DB for Spring Security
│
└── frontend/
    └── index.html                           ← Complete responsive SPA
```

---

## Setup & Run

### Prerequisites
- Java 17+
- PostgreSQL running on port 5432
- Redis running on port 6379
- Maven

### 1. PostgreSQL
```sql
CREATE DATABASE jgnexus;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE jgnexus TO postgres;
```

### 2. Redis
```bash
redis-server   # or: docker run -p 6379:6379 redis
```

### 3. Run Backend
```bash
cd backend
mvn spring-boot:run
# Starts on http://localhost:8080/api
```

### 4. Open Frontend
Open `frontend/index.html` in your browser.
> For full API integration, use a local server: `npx serve frontend`

---

## REST API Reference

### Auth
```
POST /api/auth/register   → { fullName, username, email, password, collegeName, branch, yearOfStudy }
POST /api/auth/login      → { usernameOrEmail, password }
POST /api/auth/refresh    → Header: Refresh-Token: <token>
```

### Users
```
GET  /api/users/{username}/profile    → User profile (public)
PUT  /api/users/me                    → Update own profile (auth)
POST /api/users/{userId}/follow       → Follow/unfollow toggle (auth)
GET  /api/users/search?q=vraj         → Search users
GET  /api/users/suggestions           → Suggested users from same college (auth)
GET  /api/users/{userId}/followers    → Followers list
GET  /api/users/{userId}/following    → Following list
```

### Posts
```
POST   /api/posts                          → Create post (auth)
GET    /api/posts/feed?page=0&size=10      → Personalized feed (auth)
GET    /api/posts/trending                 → Trending posts
GET    /api/posts/user/{userId}            → User's posts
GET    /api/posts/{postId}                 → Single post
POST   /api/posts/{postId}/like            → Toggle like (auth)
POST   /api/posts/{postId}/comments        → Add comment (auth)
GET    /api/posts/{postId}/comments        → Get comments
DELETE /api/posts/{postId}                 → Delete post (auth)
GET    /api/posts/search?q=spring          → Search posts
```

### Chat (REST + WebSocket)
```
POST /api/chat/send/{receiverId}           → Send message (auth)
GET  /api/chat/conversation/{userId}       → Message history (auth)
POST /api/chat/conversation/{userId}/read  → Mark as read (auth)
GET  /api/chat/online/{username}           → Check online status

WebSocket: ws://localhost:8080/api/ws
  Subscribe: /user/queue/messages          → Receive messages
  Subscribe: /user/queue/notifications     → Receive notifications
  Send to:   /app/chat                     → Send message (WsChatMessage payload)
```

### Notifications
```
GET  /api/notifications                    → All notifications (paged)
GET  /api/notifications/unread-count       → Count unread
POST /api/notifications/read-all           → Mark all read
```

---

## Security Flow

```
Client → Bearer JWT in Authorization header
       → JwtAuthenticationFilter extracts username
       → Validates against UserDetailsService (DB lookup)
       → Sets SecurityContext
       → Spring Security checks route permissions

WebSocket:
Client → CONNECT with Authorization header
       → WebSocketConfig ChannelInterceptor
       → Validates JWT on CONNECT frame only
       → Sets Principal for user-targeted messaging
```

---

## Redis Usage

| Key Pattern | TTL | Purpose |
|-------------|-----|---------|
| `userProfile::{username}` | 10 min | Cached user profiles |
| `feed::{username}_0` | 10 min | Paginated feed pages |
| `trending::0` | 10 min | Trending posts |
| `online::{username}` | 5 min | Online presence (refreshed on activity) |
| `typing::{user}::{partner}` | 10 sec | Typing indicator |
