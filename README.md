# SnapShare 📁

A modern, full-stack file-sharing application built to securely transfer files between users. The project is designed to showcase skills in building a robust, scalable, and containerized microservice architecture.

---

### 🌟 Features

* **Multi-File Sharing:** Users can select and upload multiple files at once.
* **Secure OTP Sharing:** A unique, time-sensitive OTP is generated for every upload, ensuring only the intended recipient can access the files.
* **Scalable File Chunking:** Large files (over 100MB) are automatically split into smaller chunks and uploaded in a streaming fashion. This prevents server memory from being overwhelmed and ensures reliable transfers.
* **Automatic Cleanup:** All uploaded files and their metadata are automatically deleted from the server and database after a successful download, maintaining privacy and freeing up storage.
  
---
### 💻 Tech Stack

* **Frontend / Styling:** Angular modern, Tailwind CSS
* **Backend / Database:** Java; Spring Boot, PostgreSQL.
* **Deployment:** Github Actions, Docker, Render

---
### Demo video
https://github.com/user-attachments/assets/ec25bb1b-1e67-49cf-a9e0-b1c37554059a


