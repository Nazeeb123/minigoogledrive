# Mini Google Drive

### AI-Powered Cloud File Management Platform

Mini Google Drive is a full-stack cloud file management platform inspired by modern cloud storage systems. It provides secure file management, sharing, organization, and AI-powered document intelligence.

> **Note:** This repository contains proprietary source code. Please see the `LICENSE` file for permitted use.

---

## 🚀 Features

### 📁 File Management

* Upload and download files
* File organization using folders
* File search
* Recent files
* Starred files
* Trash and restore
* Permanent deletion
* File version management

### 🔐 Authentication & Security

* User registration and login
* JWT-based authentication
* Password hashing with BCrypt
* Protected API endpoints
* User-based file access

### 👥 File Sharing

* Share files with other users
* User-specific access control
* Shared files management
* Sharing notifications

### 🤖 AI-Powered Features

* Chat with uploaded documents
* Ask questions about files
* AI-powered file renaming
* Semantic file search
* Document embeddings
* Retrieval-Augmented Generation (RAG)

### 📄 Document Intelligence

Supports processing of documents such as PDFs and office files for search and AI-based question answering.

---

## 🏗️ Architecture

```text
                    ┌──────────────────┐
                    │   React Frontend │
                    └────────┬─────────┘
                             │
                        HTTP / REST
                             │
                    ┌────────▼─────────┐
                    │ Spring Boot API  │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
       ┌──────▼──────┐ ┌────▼─────┐ ┌──────▼──────┐
       │    MySQL    │ │  Storage │ │  AI / RAG   │
       │  Database   │ │  Files   │ │  Embeddings │
       └─────────────┘ └──────────┘ └─────────────┘
```

---

## 🛠️ Tech Stack

### Frontend

* React
* Vite
* JavaScript
* Axios

### Backend

* Java
* Spring Boot
* Spring Security
* JWT
* JPA / Hibernate

### Database

* MySQL

### AI

* LLM APIs
* Embeddings
* Retrieval-Augmented Generation (RAG)
* Semantic Search

### Tools & Services

* Git
* GitHub
* Postman
* Cloud File Storage

---

## 🔄 AI Document Question-Answering

The document question-answering system follows a retrieval-based architecture:

```text
User Question
      ↓
Question Embedding
      ↓
Semantic Retrieval
      ↓
Relevant Document Content
      ↓
Context + Question
      ↓
LLM
      ↓
Generated Answer
```

This allows users to ask questions about their uploaded documents instead of manually searching through them.

---

## 🔑 Security

The application uses JWT-based authentication to identify users and protect private API endpoints.

Passwords are securely hashed before being stored.

Access to files is controlled based on the authenticated user and sharing permissions.

---

## 📸 Project Preview

Screenshots and demonstrations can be added here.

---

## 🎯 Purpose

This project was built as a practical full-stack engineering project to understand:

* REST API development
* Spring Boot backend architecture
* Authentication and authorization
* Database design
* File management systems
* Cloud file storage
* AI integration
* Embeddings and semantic search
* RAG-based document question answering
* Full-stack application development

---

## 📜 License

Copyright (c) 2026 KMD Nazeeb Ul Zama

All rights reserved.

The source code is available for personal, educational, portfolio, and evaluation purposes only.

See the `LICENSE` file for the complete terms.
