import axios from "axios";
import { useEffect, useRef, useState } from "react";
import "./AI.css";
import { useSearchParams, useNavigate } from "react-router-dom";
import API from "../services/api";




function AI() {
    const [messages, setMessages] = useState([]);
    const [chats, setChats] = useState([]);
    const [currentChatId, setCurrentChatId] = useState(null);
    const [question, setQuestion] = useState("");
    const [loading, setLoading] = useState(false);
    const [files, setFiles] = useState([]);
    const [selectedFile, setSelectedFile] = useState(null);
    const [searchParams] = useSearchParams();
    const fileId = searchParams.get("fileId");
    const navigate = useNavigate();
    const messagesEndRef = useRef(null);
    const textareaRef = useRef(null);
    const [attachedFile, setAttachedFile] = useState(null);
   
   

    // =========================
    // LOAD ALL CHATS
    // =========================

    const loadChats = async () => {
        try {
            const response = await API.get("/ai/chats");
            setChats(response.data);

            if (response.data.length > 0 && currentChatId === null) {
                const latestChat = response.data[0];

                setCurrentChatId(latestChat.id);

                const messagesResponse = await API.get(
                    `/ai/chats/${latestChat.id}/messages`
                );

                const formattedMessages = [];

                messagesResponse.data.forEach((message) => {
                    formattedMessages.push({
                        role: "user",
                        content: message.question
                    });

                    formattedMessages.push({
                        role: "ai",
                        content: message.answer
                    });
                });

                setMessages(formattedMessages);
            }

        } catch (error) {
            console.error("LOAD CHATS ERROR:", error);
        }
    };
    const loadFiles = async () => {
        try {

            const response = await API.get("/files/my");

            setFiles(response.data);

            console.log("📁 FILES LOADED:", response.data);

        } catch (error) {

            console.error("❌ LOAD FILES ERROR:", error);

        }
    };
    // =========================
    // LOAD CHAT MESSAGES
    // =========================
    const loadSelectedFile = async () => {

        if (!fileId) {
            console.log("❌ No fileId in URL");
            return;
        }

        try {

            console.log("🔍 Loading file ID:", fileId);

            const response = await API.get("/files/my");

            console.log("📁 My files:", response.data);

            const file = response.data.find(
                (item) =>
                    String(item.id) === String(fileId)
            );

            if (file) {

                console.log("✅ FILE SELECTED:", file);

                setSelectedFile(file);

            } else {

                console.log(
                    "❌ File not found. File ID:",
                    fileId
                );

            }

        } catch (error) {

            console.error(
                "❌ LOAD SELECTED FILE ERROR:",
                error
            );

        }
    };
    const loadMessages = async (chatId) => {
        try {
            const response = await API.get(
                `/ai/chats/${chatId}/messages`
            );

            const formattedMessages = [];

            response.data.forEach((message) => {
                formattedMessages.push({
                    role: "user",
                    content: message.question
                });

                formattedMessages.push({
                    role: "ai",
                    content: message.answer
                });
            });

            setMessages(formattedMessages);
        } catch (error) {
            console.error("LOAD MESSAGES ERROR:", error);
        }
    };

    // =========================
    // INITIAL LOAD
    // =========================

    useEffect(() => {
        loadChats();
    }, []);
    useEffect(() => {
        loadSelectedFile();
    }, [fileId]);
    useEffect(() => {
        loadFiles();
    }, []);

    // =========================
    // AUTO SCROLL
    // =========================

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({
            behavior: "smooth"
        });
    }, [messages, loading]);

    // =========================
    // CREATE NEW CHAT
    // =========================

    const newChat = async () => {
        try {
            const response = await API.post(
                `/ai/chats`,
                {
                    title: "New Chat"
                }
            );

            const newChatData = response.data;

            setChats((prev) => [
                newChatData,
                ...prev
            ]);

            setCurrentChatId(newChatData.id);
            setMessages([]);
            setQuestion("");

            setTimeout(() => {
                textareaRef.current?.focus();
            }, 100);

        } catch (error) {
            console.error("CREATE CHAT ERROR:", error);
        }
    };

    // =========================
    // OPEN CHAT
    // =========================

    const openChat = async (chatId) => {
        try {
            setCurrentChatId(chatId);

            const response = await API.get(
                `/ai/chats/${chatId}/messages`
            );

            const formattedMessages = [];

            response.data.forEach((message) => {
                formattedMessages.push({
                    role: "user",
                    content: message.question
                });

                formattedMessages.push({
                    role: "ai",
                    content: message.answer
                });
            });

            // Only replace messages AFTER successfully getting them
            setMessages(formattedMessages);

            setTimeout(() => {
                textareaRef.current?.focus();
            }, 100);

        } catch (error) {
            console.error("OPEN CHAT ERROR:", error);
        }
    };

    // =========================
    // ASK AI
    // =========================


    const askAI = async () => {

        const text = question.trim();

        if (!text || loading) {
            return;
        }

        let chatId = currentChatId;


        // =========================================
        // CREATE CHAT IF THERE IS NO CHAT
        // =========================================

        if (!chatId) {

            try {

                const response = await API.post(
                    "/ai/chats",
                    {
                        title:
                            text.length > 30
                                ? text.substring(0, 30) + "..."
                                : text
                    }
                );

                chatId = response.data.id;

                setCurrentChatId(chatId);

                setChats((prev) => [
                    response.data,
                    ...prev
                ]);

            } catch (error) {

                console.error(
                    "CREATE CHAT ERROR:",
                    error
                );

                return;
            }
        }


        // =========================================
        // SHOW USER MESSAGE
        // =========================================

        setMessages((prev) => [
            ...prev,
            {
                role: "user",
                content: text
            }
        ]);

        setQuestion("");
        setLoading(true);


        // =========================================
        // ASK AI
        // =========================================

        try {

            let response;


            // FILE AI
            if (selectedFile) {

                response = await API.post(
                    "/ai/file-ask",
                    {
                        question: text,
                        fileId: selectedFile.id
                    }
                );

            }

            // NORMAL AI
            else {

                response = await API.post(
                    `/ai/chats/${chatId}/ask`,
                    {
                        question: text
                    }
                );
            }


            // =========================================
            // SHOW AI RESPONSE
            // =========================================

            setMessages((prev) => [
                ...prev,
                {
                    role: "ai",
                    content: response.data.answer
                }
            ]);


            // =========================================
            // REFRESH CHAT LIST
            // =========================================

            const chatsResponse = await API.get(
                "/ai/chats"
            );

            setChats(chatsResponse.data);


        } catch (error) {

            console.error(
                "AI ERROR:",
                error
            );

            console.error(
                "STATUS:",
                error.response?.status
            );

            console.error(
                "RESPONSE:",
                error.response?.data
            );


            let errorMessage =
                "Something went wrong. Please try again.";


            if (error.response?.data?.message) {

                errorMessage =
                    error.response.data.message;

            }


            setMessages((prev) => [
                ...prev,
                {
                    role: "ai",
                    content: errorMessage
                }
            ]);

        } finally {

            setLoading(false);

            setTimeout(() => {
                textareaRef.current?.focus();
            }, 100);

        }
    };


    // =========================
    // ENTER KEY
    // =========================

    const handleKeyDown = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            askAI();
        }
    };

    // =========================
    // DELETE CHAT
    // =========================

    const deleteChat = async (chatId, e) => {
        e.stopPropagation();

        try {
            await API.delete(
                `/ai/chats/${chatId}`
            );

            const remainingChats = chats.filter(
                (chat) => chat.id !== chatId
            );

            setChats(remainingChats);

            if (currentChatId === chatId) {
                setCurrentChatId(null);
                setMessages([]);
            }

        } catch (error) {
            console.error("DELETE CHAT ERROR:", error);
        }
    };

    return (
        <div className="ai-page">

            {/* =========================
                SIDEBAR
            ========================= */}

            <aside className="ai-sidebar">

                <button
                    className="new-chat-btn"
                    onClick={newChat}
                >
                    <span className="new-chat-icon">
                        ＋
                    </span>

                    New chat
                </button>

                <div className="sidebar-section-title">
                    Recent
                </div>

                <div className="conversation-list">

                    {chats.length === 0 && (
                        <div className="empty-chats">
                            No conversations yet
                        </div>
                    )}

                    {chats.map((chat) => (

                        <div
                            key={chat.id}
                            className={`conversation-item ${currentChatId === chat.id
                                ? "active-chat"
                                : ""
                                }`}
                            onClick={() =>
                                openChat(chat.id)
                            }
                        >

                            <span>💬</span>

                            <span className="conversation-title">
                                {chat.title}
                            </span>

                            <button
                                className="delete-chat-btn"
                                onClick={(e) => deleteChat(chat.id, e)}
                                title="Delete chat"
                            >
                                🗑
                            </button>

                        </div>

                    ))}

                </div>

                <div className="sidebar-bottom">

                    <div className="sidebar-user">

                        <div className="user-avatar">
                            AI
                        </div>

                        <div>

                            <div className="user-name">
                                Mini Google Drive
                            </div>

                            <div className="user-subtitle">
                                AI Assistant
                            </div>

                        </div>

                    </div>

                </div>

            </aside>


            {/* =========================
                MAIN CHAT
            ========================= */}

            <main className="ai-main">

                <header className="ai-header">


                    <div className="ai-header-title">

                        <div className="ai-logo">
                            🧠
                        </div>

                        <div>

                            <h2>
                                AI Assistant
                            </h2>
                            {selectedFile && (
                                <div className="ai-selected-file">
                                    📄 {selectedFile.fileName}
                                </div>
                            )}
                            <span>
                                Powered by Ollama
                            </span>

                        </div>
                        <button
                            className="back-to-drive-btn"
                            onClick={() => navigate("/dashboard")}
                        >
                            ← Back to My Drive
                        </button>

                    </div>

                </header>


                {/* =========================
                    CHAT AREA
                ========================= */}

                <div className="chat-area">

                    {messages.length === 0 ? (

                        <div className="welcome-screen">

                            <div className="welcome-logo">
                                🧠
                            </div>

                            <h1>
                                How can I help you?
                            </h1>

                            <p>
                                Ask anything about programming,
                                your files, or anything you want to learn.
                            </p>

                            <div className="suggestion-grid">

                                <button
                                    onClick={() =>
                                        setQuestion(
                                            "Explain Java in simple words"
                                        )
                                    }
                                >
                                    <strong>
                                        💡 Learn something
                                    </strong>

                                    <span>
                                        Explain Java in simple words
                                    </span>
                                </button>

                                <button
                                    onClick={() =>
                                        setQuestion(
                                            "Explain Spring Boot for beginners"
                                        )
                                    }
                                >
                                    <strong>
                                        ☕ Spring Boot
                                    </strong>

                                    <span>
                                        Explain Spring Boot for beginners
                                    </span>
                                </button>

                                <button
                                    onClick={() =>
                                        setQuestion(
                                            "Help me debug my Java code"
                                        )
                                    }
                                >
                                    <strong>
                                        🐛 Debug code
                                    </strong>

                                    <span>
                                        Help me debug my Java code
                                    </span>
                                </button>

                                <button
                                    onClick={() =>
                                        setQuestion(
                                            "Give me a project idea"
                                        )
                                    }
                                >
                                    <strong>
                                        🚀 Project ideas
                                    </strong>

                                    <span>
                                        Give me a project idea
                                    </span>
                                </button>

                            </div>

                        </div>

                    ) : (

                        <div className="messages-container">

                            {messages.map(
                                (message, index) => (

                                    <div
                                        className={`message-row ${message.role === "user"
                                            ? "user-row"
                                            : "ai-row"
                                            }`}
                                        key={index}
                                    >

                                        <div
                                            className={`message-avatar ${message.role === "user"
                                                ? "user-message-avatar"
                                                : "ai-message-avatar"
                                                }`}
                                        >
                                            {message.role === "user"
                                                ? "U"
                                                : "🧠"}
                                        </div>

                                        <div className="message-content">

                                            <div className="message-name">
                                                {message.role === "user"
                                                    ? "You"
                                                    : "AI Assistant"}
                                            </div>

                                            <div className="message-text">
                                                {message.content}
                                            </div>

                                        </div>

                                    </div>

                                )
                            )}

                            {loading && (

                                <div className="message-row ai-row">

                                    <div className="message-avatar ai-message-avatar">
                                        🧠
                                    </div>

                                    <div className="message-content">

                                        <div className="message-name">
                                            AI Assistant
                                        </div>

                                        <div className="typing-indicator">

                                            <span></span>
                                            <span></span>
                                            <span></span>

                                        </div>

                                    </div>

                                </div>

                            )}

                            <div ref={messagesEndRef} />

                        </div>

                    )}

                </div>


                {/* =========================
                    INPUT
                ========================= */}

                <div className="input-section">

                    <div className="input-wrapper">

                        <textarea
                            ref={textareaRef}
                            value={question}
                            onChange={(e) =>
                                setQuestion(e.target.value)
                            }
                            onKeyDown={handleKeyDown}
                            placeholder="Message AI Assistant..."
                            rows="1"
                        />

                        <button
                            className="send-button"
                            onClick={askAI}
                            disabled={
                                !question.trim() ||
                                loading
                            }
                        >
                            ➤
                        </button>

                    </div>

                    <div className="input-hint">
                        AI can make mistakes. Check important information.
                    </div>

                </div>

            </main>

        </div>
    );
}

export default AI;

