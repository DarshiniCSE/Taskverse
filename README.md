# TaskVerse — Your Smart Productivity Workspace

A modern, full-stack Task Management System designed for college project demonstrations. TaskVerse illustrates real-world **Object-Oriented Programming (OOP)** principles in Java and **Database Management System (DBMS)** concepts using MySQL Server and Apache Tomcat 10.1.

---

## 1. Project Technology Stack

- **Frontend:** HTML5, CSS3 (Midnight Dark Glowing Aesthetic), Vanilla JavaScript (ES6+ Fetch API)
- **Backend:** Java 26, Jakarta Servlets 6.0 (`jakarta.servlet.*`), JDBC
- **Database:** MySQL Server 8.0, MySQL Workbench 8.0
- **Web Server:** Apache Tomcat 10.1
- **Operating System:** Windows 11 / 10

---

## 2. Architecture & OOP Concepts for Viva

| OOP Concept | File | Implementation & Purpose |
| :--- | :--- | :--- |
| **Encapsulation** | `Task.java`, `Category.java`, `TaskStats.java` | All entity attributes are private, exposed only through validated getters and setters. |
| **Abstraction** | `TaskDAO.java` | An interface providing an abstract contract of data operations (`createTask`, `getAllTasks`, `updateTask`, etc.) without exposing internal SQL logic. |
| **Polymorphism** | `TaskDAOImpl.java` | Concrete implementation of `TaskDAO`. The servlets call `TaskDAO`, allowing loose coupling and interchangeability. |
| **Separation of Concerns (MVC)** | `servlet/`, `dao/`, `model/`, `webapp/` | Servlets act as Controllers, DAOs as Persistence, Models as Entities, and HTML/CSS/JS as View. |
| **Resource Safety** | `DBConnection.java`, `TaskDAOImpl.java` | Uses Java `try-with-resources` (`AutoCloseable`) ensuring Connections, Statements, and ResultSets are closed cleanly without memory leaks. |

---

## 3. Directory Layout

```
TaskVerse/
├── .gitignore                      # Excludes local db.properties and compiled bytecode
├── README.md                       # Complete documentation & college viva guide
├── build.ps1                       # Windows PowerShell build & deployment script
│
├── db/
│   ├── schema.sql                  # Database definition (taskverse_db, tables, indexes, sample data)
│   ├── db.properties.example       # Version-controlled configuration template
│   └── db.properties               # Local private MySQL credentials
│
├── src/
│   └── com/
│       └── taskverse/
│           ├── model/              # Task.java, Category.java, TaskStats.java
│           ├── dao/                # TaskDAO.java, TaskDAOImpl.java
│           ├── util/               # DBConnection.java
│           └── servlet/            # TaskServlet.java, DashboardServlet.java, HealthServlet.java
│
└── webapp/                         # Webroot deployed to Tomcat webapps/TaskVerse
    ├── index.html                  # Midnight theme dashboard
    ├── css/style.css               # Midnight dark glow aesthetic
    ├── js/app.js                   # Client-side controller and REST client
    └── WEB-INF/
        ├── web.xml                 # Jakarta servlet mappings
        ├── classes/                # Compiled .class files & runtime db.properties
        └── lib/                    # MySQL JDBC connector driver
```

---

## 4. Quick Setup & Execution Guide

### Step 1: Initialize Database in MySQL Workbench 8.0
1. Open **MySQL Workbench 8.0** and connect to your local MySQL Server (`localhost:3306`).
2. Go to **File -> Open SQL Script...** and select `db/schema.sql`.
3. Click the **Lightning Bolt (Execute)** button.
4. In the left **SCHEMAS** panel, right-click and click **Refresh All**. You will see `taskverse_db` with `categories` and `tasks`.

### Step 2: Configure Database Password
1. Open `db/db.properties` in VS Code or Notepad:
   ```properties
   db.driver=com.mysql.cj.jdbc.Driver
   db.url=jdbc:mysql://localhost:3306/taskverse_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
   db.user=root
   db.password=YOUR_MYSQL_PASSWORD
   ```
2. Save the file.

### Step 3: Compile and Deploy to Tomcat
Open PowerShell in the TaskVerse root directory and run:
```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

### Step 4: Start Tomcat Web Server
In PowerShell or Command Prompt, execute:
```cmd
cmd /c "C:\Users\darsh\Downloads\apache-tomcat-10.1.61-windows-x64\apache-tomcat-10.1.61\bin\startup.bat"
```

### Step 5: Open in Google Chrome
Navigate to:
```
http://localhost:8080/TaskVerse/
```

---

## 5. Verifying Persistent MySQL Storage
1. In the browser, click **"+ New Task"**, fill in the title, priority, category, and due date, then click **"Save Task"**.
2. Refresh the browser page (`F5`) to confirm the task persists from the database.
3. In **MySQL Workbench**, run:
   ```sql
   USE taskverse_db;
   SELECT * FROM tasks ORDER BY created_at DESC;
   ```
   You will see your newly created task record directly inside the database table!
