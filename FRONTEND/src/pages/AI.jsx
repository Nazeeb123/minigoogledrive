import { useEffect, useRef, useState } from "react";
import "./AI.css";

import { useSearchParams, useNavigate } from "react-router-dom";

import API from "../services/api";

// Markdown + LaTeX
import ReactMarkdown from "react-markdown";
import remarkMath from "remark-math";
import rehypeKatex from "rehype-katex";
import "katex/dist/katex.min.css";

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


    // =========================================================
    // LOAD ALL CHATS
    // =========================================================

    const loadChats = async () => {

        try {

            const response = await API.get("/ai/chats");

            setChats(response.data);

            if (
                response.data.length > 0 &&
                currentChatId === null
            ) {

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

            console.error(
                "LOAD CHATS ERROR:",
                error
            );

        }

    };


    // =========================================================
    // LOAD FILES
    // =========================================================

    const loadFiles = async () => {

        try {

            const response = await API.get("/files/my");

            setFiles(response.data);

            console.log(
                "📁 FILES LOADED:",
                response.data
            );

        } catch (error) {

            console.error(
                "❌ LOAD FILES ERROR:",
                error
            );

        }

    };


    // =========================================================
    // LOAD SELECTED FILE
    // =========================================================

    const loadSelectedFile = async () => {

        if (!fileId) {

            console.log(
                "❌ No fileId in URL"
            );

            setSelectedFile(null);

            return;
        }

        try {

            console.log(
                "🔍 Loading file ID:",
                fileId
            );

            const response = await API.get(
                "/files/my"
            );

            console.log(
                "📁 My files:",
                response.data
            );

            const file = response.data.find(
                (item) =>
                    String(item.id) === String(fileId)
            );

            if (file) {

                console.log(
                    "✅ FILE SELECTED:",
                    file
                );

                setSelectedFile(file);

            } else {

                console.log(
                    "❌ File not found. File ID:",
                    fileId
                );

                setSelectedFile(null);

            }

        } catch (error) {

            console.error(
                "❌ LOAD SELECTED FILE ERROR:",
                error
            );

        }

    };


    // =========================================================
    // LOAD CHAT MESSAGES
    // =========================================================

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

            console.error(
                "LOAD MESSAGES ERROR:",
                error
            );

        }

    };


    // =========================================================
    // INITIAL LOAD
    // =========================================================

    useEffect(() => {

        loadChats();

    }, []);


    useEffect(() => {

        loadSelectedFile();

    }, [fileId]);


    useEffect(() => {

        loadFiles();

    }, []);


    // =========================================================
    // AUTO SCROLL
    // =========================================================

    useEffect(() => {

        messagesEndRef.current?.scrollIntoView({
            behavior: "smooth"
        });

    }, [messages, loading]);


    // =========================================================
    // CREATE NEW CHAT
    // =========================================================

    const newChat = async () => {

        try {

            const response = await API.post(
                "/ai/chats",
                {
                    title: "New Chat"
                }
            );

            const newChatData = response.data;

            setChats((prev) => [
                newChatData,
                ...prev
            ]);

            setCurrentChatId(
                newChatData.id
            );

            setMessages([]);

            setQuestion("");

            setTimeout(() => {

                textareaRef.current?.focus();

            }, 100);

        } catch (error) {

            console.error(
                "CREATE CHAT ERROR:",
                error
            );

        }

    };


    // =========================================================
    // OPEN CHAT
    // =========================================================

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

            setMessages(
                formattedMessages
            );

            setTimeout(() => {

                textareaRef.current?.focus();

            }, 100);

        } catch (error) {

            console.error(
                "OPEN CHAT ERROR:",
                error
            );

        }

    };


    // =========================================================
    // ASK AI
    // =========================================================

    const askAI = async () => {

        const text = question.trim();

        if (!text || loading) {
            return;
        }

        let chatId = currentChatId;


        // =====================================================
        // CREATE CHAT IF NEEDED
        // =====================================================

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


        // =====================================================
        // SHOW USER MESSAGE
        // =====================================================

        setMessages((prev) => [

            ...prev,

            {
                role: "user",
                content: text
            }

        ]);

        setQuestion("");

        setLoading(true);


        // =====================================================
        // ASK AI
        // =====================================================

        try {

            let response;


            // =================================================
            // FILE AI
            // =================================================

            if (selectedFile) {

                response = await API.post(
                    "/ai/file-ask",
                    {
                        question: text,
                        fileId: selectedFile.id
                    }
                );

            }

            // =================================================
            // NORMAL AI
            // =================================================

            else {

                response = await API.post(
                    `/ai/chats/${chatId}/ask`,
                    {
                        question: text
                    }
                );

            }


            // =====================================================
            // SHOW AI RESPONSE
            // =====================================================

            setMessages((prev) => [

                ...prev,

                {
                    role: "ai",
                    content: response.data.answer
                }

            ]);


            // =====================================================
            // REFRESH CHAT LIST
            // =====================================================

            const chatsResponse = await API.get(
                "/ai/chats"
            );

            setChats(
                chatsResponse.data
            );

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


            if (
                error.response?.data?.message
            ) {

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


    // =========================================================
    // REGENERATE RESPONSE
    // =========================================================

    const regenerateResponse = async (index) => {

        if (loading) {
            return;
        }

        if (!currentChatId) {
            return;
        }

        const previousUserMessage =
            messages
                .slice(0, index)
                .reverse()
                .find(
                    (message) =>
                        message.role === "user"
                );

        if (!previousUserMessage) {
            return;
        }

        setLoading(true);

        try {

            const response = await API.post(
                `/ai/chats/${currentChatId}/ask`,
                {
                    question:
                        previousUserMessage.content
                }
            );

            setMessages((prev) => {

                const updated = [...prev];

                updated[index] = {
                    role: "ai",
                    content: response.data.answer
                };

                return updated;

            });

        } catch (error) {

            console.error(
                "REGENERATE ERROR:",
                error
            );

        } finally {

            setLoading(false);

        }

    };


    // =========================================================
    // COPY RESPONSE
    // =========================================================

    const copyResponse = async (content) => {

        try {

            await navigator.clipboard.writeText(
                content
            );

        } catch (error) {

            console.error(
                "COPY ERROR:",
                error
            );

        }

    };


    // =========================================================
    // ENTER KEY
    // =========================================================

    const handleKeyDown = (e) => {

        if (
            e.key === "Enter" &&
            !e.shiftKey
        ) {

            e.preventDefault();

            askAI();

        }

    };


    // =========================================================
    // DELETE CHAT
    // =========================================================

    const deleteChat = async (
        chatId,
        e
    ) => {

        e.stopPropagation();

        try {

            await API.delete(
                `/ai/chats/${chatId}`
            );

            const remainingChats =
                chats.filter(
                    (chat) =>
                        chat.id !== chatId
                );

            setChats(
                remainingChats
            );

            if (
                currentChatId === chatId
            ) {

                setCurrentChatId(null);

                setMessages([]);

            }

        } catch (error) {

            console.error(
                "DELETE CHAT ERROR:",
                error
            );

        }

    };


    // =========================================================
    // MARKDOWN COMPONENTS
    // =========================================================

    const markdownComponents = {

        /*
         * FIX:
         *
         * Previously code() returned:
         *
         * <pre>
         *     <code>
         *     </code>
         * </pre>
         *
         * ReactMarkdown can already place code inside
         * paragraph structures, which caused:
         *
         * <p>
         *     <pre>
         * </pre>
         * </p>
         *
         * That is invalid HTML.
         *
         * Now code() ONLY returns <code>.
         */

        code({
            inline,
            className,
            children,
            ...props
        }) {

            if (inline) {

                return (
                    <code
                        className="inline-code"
                        {...props}
                    >
                        {children}
                    </code>
                );

            }

            return (
                <code
                    className={className}
                    {...props}
                >
                    {children}
                </code>
            );

        },


        /*
         * Code blocks are handled separately.
         */

        pre({ children }) {

            return (
                <pre className="code-block">
                    {children}
                </pre>
            );

        },


        p({ children }) {

            return (
                <p className="markdown-paragraph">
                    {children}
                </p>
            );

        },


        h1({ children }) {

            return (
                <h1 className="markdown-h1">
                    {children}
                </h1>
            );

        },


        h2({ children }) {

            return (
                <h2 className="markdown-h2">
                    {children}
                </h2>
            );

        },


        h3({ children }) {

            return (
                <h3 className="markdown-h3">
                    {children}
                </h3>
            );

        },


        ul({ children }) {

            return (
                <ul className="markdown-ul">
                    {children}
                </ul>
            );

        },


        ol({ children }) {

            return (
                <ol className="markdown-ol">
                    {children}
                </ol>
            );

        },


        li({ children }) {

            return (
                <li className="markdown-li">
                    {children}
                </li>
            );

        },


        strong({ children }) {

            return (
                <strong className="markdown-strong">
                    {children}
                </strong>
            );

        },


        blockquote({ children }) {

            return (
                <blockquote className="markdown-blockquote">
                    {children}
                </blockquote>
            );

        },


        hr() {

            return (
                <hr className="markdown-hr" />
            );

        }

    };


    // =========================================================
    // RENDER
    // =========================================================

    return (

        <div className="ai-page">


            {/* =================================================
                SIDEBAR
            ================================================= */}

            <aside className="ai-sidebar">


                {/* NEW CHAT */}

                <button
                    className="new-chat-btn"
                    onClick={newChat}
                >

                    <span className="new-chat-icon">
                        ＋
                    </span>

                    New chat

                </button>


                {/* RECENT */}

                <div className="sidebar-section-title">
                    Recent
                </div>


                {/* CHAT LIST */}

                <div className="conversation-list">

                    {chats.length === 0 && (

                        <div className="empty-chats">
                            No conversations yet
                        </div>

                    )}


                    {chats.map((chat) => (

                        <div
                            key={chat.id}
                            className={
                                `conversation-item ${currentChatId === chat.id
                                    ? "active-chat"
                                    : ""
                                }`
                            }
                            onClick={() =>
                                openChat(chat.id)
                            }
                        >

                            <span>
                                💬
                            </span>


                            <span className="conversation-title">

                                {chat.title}

                            </span>


                            <button
                                className="delete-chat-btn"
                                onClick={(e) =>
                                    deleteChat(
                                        chat.id,
                                        e
                                    )
                                }
                                title="Delete chat"
                            >
                                🗑
                            </button>

                        </div>

                    ))}

                </div>


                {/* SIDEBAR BOTTOM */}

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


            {/* =================================================
                MAIN
            ================================================= */}

            <main className="ai-main">


                {/* =================================================
                    HEADER
                ================================================= */}

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
                                Powered by OpenRouter
                            </span>

                        </div>


                        <button
                            className="back-to-drive-btn"
                            onClick={() =>
                                navigate("/dashboard")
                            }
                        >
                            ← Back to My Drive
                        </button>

                    </div>

                </header>


                {/* =================================================
                    CHAT AREA
                ================================================= */}

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
                                        className={
                                            `message-row ${message.role === "user"
                                                ? "user-row"
                                                : "ai-row"
                                            }`
                                        }
                                        key={index}
                                    >


                                        {/* AVATAR */}

                                        <div
                                            className={
                                                `message-avatar ${message.role === "user"
                                                    ? "user-message-avatar"
                                                    : "ai-message-avatar"
                                                }`
                                            }
                                        >

                                            {message.role === "user"
                                                ? "U"
                                                : "🧠"}

                                        </div>


                                        {/* MESSAGE CONTENT */}

                                        <div className="message-content">


                                            <div className="message-name">

                                                {message.role === "user"
                                                    ? "You"
                                                    : "AI Assistant"}

                                            </div>


                                            <div className="message-text">


                                                {message.role === "user" ? (

                                                    <div>
                                                        {message.content}
                                                    </div>

                                                ) : (

                                                    <>

                                                        {/* AI RESPONSE */}

                                                        <ReactMarkdown
                                                            remarkPlugins={[
                                                                remarkMath
                                                            ]}
                                                            rehypePlugins={[
                                                                rehypeKatex
                                                            ]}
                                                            components={
                                                                markdownComponents
                                                            }
                                                        >
                                                            {message.content}
                                                        </ReactMarkdown>


                                                        {/* =================================================
                                                            COPY + REGENERATE
                                                        ================================================= */}

                                                        <div className="ai-message-actions">

                                                            <button
                                                                className="ai-action-btn"
                                                                onClick={() =>
                                                                    copyResponse(
                                                                        message.content
                                                                    )
                                                                }
                                                                title="Copy response"
                                                            >

                                                                <span>
                                                                    📋
                                                                </span>

                                                                <span>
                                                                    Copy
                                                                </span>

                                                            </button>


                                                            <button
                                                                className="ai-action-btn"
                                                                onClick={() =>
                                                                    regenerateResponse(
                                                                        index
                                                                    )
                                                                }
                                                                disabled={loading}
                                                                title="Regenerate response"
                                                            >

                                                                <span>
                                                                    ↻
                                                                </span>

                                                                <span>
                                                                    Regenerate
                                                                </span>

                                                            </button>

                                                        </div>

                                                    </>

                                                )}

                                            </div>

                                        </div>

                                    </div>

                                )
                            )}


                            {/* =================================================
                                TYPING
                            ================================================= */}

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


                            <div
                                ref={messagesEndRef}
                            />

                        </div>

                    )}

                </div>


                {/* =================================================
                    INPUT
                ================================================= */}

                <div className="input-section">


                    <div className="input-wrapper">


                        <textarea
                            ref={textareaRef}
                            value={question}
                            onChange={(e) =>
                                setQuestion(
                                    e.target.value
                                )
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