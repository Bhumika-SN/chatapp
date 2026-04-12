# 💬 Real-Time Chat Application with AI

A full-stack real-time chat application built using Spring Boot, WebSockets, and MySQL — enhanced with AI-powered chat summarization and modern messaging features.

---

## 🚀 Features

### 🔹 Real-Time Messaging
- Instant message delivery using WebSockets (STOMP)
- No page refresh required
- Supports multiple users simultaneously

### 🔹 Chat Rooms
- Create and join multiple chat rooms
- Messages are scoped to specific rooms
- Organized communication system

### 🔹 Persistent Chat History
- Messages stored in MySQL database
- Chat history loads on refresh
- Uses Spring Data JPA

### 🔹 AI Chat Summarization 🤖
- Summarizes long conversations
- Helps users quickly understand chats
- Improves productivity and UX

### 🔹 Typing Indicators ✍️
- Shows when a user is typing
- Real-time updates via WebSockets

### 🔹 Read Receipts ✔✔
- Track message delivery and seen status
- Enhances communication clarity

### 🔹 User Authentication 🔐
- Login & Register functionality
- Secure user management

### 🔹 REST + WebSocket Architecture
- REST APIs → fetch data (history, rooms)
- WebSockets → real-time updates

---

## 🛠️ Tech Stack

- **Backend:** Java, Spring Boot  
- **Real-Time:** WebSockets (STOMP, SockJS)  
- **Database:** MySQL  
- **ORM:** Spring Data JPA  
- **Security:** JWT Authentication  
- **Frontend:** HTML, CSS, JavaScript  

---

## 📂 Project Structure

```
com.example.chatapp
│── config        # WebSocket & Security configs
│── controller    # REST + WebSocket controllers
│── model         # Entity classes (User, Message, Room)
│── repository    # JPA repositories
│── service       # Business logic
│── security      # JWT authentication
```

---

## ⚙️ How It Works

1. User logs in / registers
2. Connects to WebSocket (`/ws`)
3. Sends message → `/app/sendMessage`
4. Server saves message in MySQL
5. Broadcasts message → `/topic/room/{roomName}`
6. All users in that room receive it instantly

---

## ▶️ How to Run

### 1. Clone the repository
```bash
git clone https://github.com/your-username/your-repo-name.git
cd your-repo-name
```

### 2. Setup MySQL
```sql
CREATE DATABASE chatdb;
```

Update `application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=yourpassword
```

### 3. Run the application
```bash
mvn spring-boot:run
```

### 4. Open in browser
```
http://localhost:8080
```

---

## 📸 Screenshots

> Add screenshots here (chat UI, AI summary, etc.)

---

## 📈 Future Improvements

- 📎 File & image sharing  
- 🔔 Push notifications  
- 🌐 Multi-language chat  
- 🎥 Voice / Video calling  

---

## 🙌 Acknowledgements

Inspired by modern chat applications like WhatsApp and Discord.

---

## 👩‍💻 Author

**Your Name**  
- GitHub: https://github.com/Bhumika-SN 
- LinkedIn: https://www.linkedin.com/in/bhumika-s-n-9152a6295/ 

---

## ⭐ If you like this project
Give it a star ⭐ and feel free to contribute!
