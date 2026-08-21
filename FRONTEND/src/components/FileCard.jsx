import "./FileCard.css";

import {
    FaFilePdf,
    FaFileImage,
    FaFileVideo,
    FaFileAudio,
    FaFileWord,
    FaFileExcel,
    FaFilePowerpoint,
    FaFileArchive,
    FaFileAlt,
    FaEllipsisV
} from "react-icons/fa";

import { useState, useEffect, useRef } from "react";
import API from "../services/api";
import { useNavigate } from "react-router-dom";
import ConvertFileModal from "./ConvertFileModal";
import CompressFileModal from "./CompressFileModal";

function FileCard({
    file,
    loadFiles,
    isRecent,
    isShared,
    setShareFile,
    isTrash,
    onRestore,
    onDeletePermanent,
    onFileClick
}) {

    console.log("isShared =", isShared);

    const [newName, setNewName] = useState("");
    const [renameMode, setRenameMode] = useState(false);
    const [addedToDrive, setAddedToDrive] = useState(false);
    const [showEmailModal, setShowEmailModal] = useState(false);
    const [recipientEmail, setRecipientEmail] = useState("");
    const [emailSending, setEmailSending] = useState(false);
    const [emailSuccess, setEmailSuccess] = useState(false);
    const [showConvertModal, setShowConvertModal] = useState(false);

    const [menuOpen, setMenuOpen] = useState(false);
    const [popup, setPopup] = useState({
        show: false,
        message: "",
        type: ""
    });
    const [showCompressModal, setShowCompressModal] =
        useState(false);

    const showPopup = (message, type = "success") => {

        setPopup({
            show: true,
            message,
            type
        });

        setTimeout(() => {

            setPopup({
                show: false,
                message: "",
                type: ""
            });

        }, 3000);

    };

    // AI Rename states
    const [showAIRename, setShowAIRename] = useState(false);
    const [aiSuggestedName, setAiSuggestedName] = useState("");
    const [aiLoading, setAiLoading] = useState(false);
    const [aiError, setAiError] = useState("");

    const menuRef = useRef();

    const navigate = useNavigate();

    const extension =
        file.fileName.split(".").pop().toLowerCase();


    // =========================
    // CLOSE MENU
    // =========================

    useEffect(() => {

        const closeMenu = (event) => {

            if (
                menuRef.current &&
                !menuRef.current.contains(event.target)
            ) {
                setMenuOpen(false);
            }

        };

        document.addEventListener(
            "mousedown",
            closeMenu
        );

        return () => {

            document.removeEventListener(
                "mousedown",
                closeMenu
            );

        };

    }, []);


    // =========================
    // FILE ICON
    // =========================

    let icon =
        <FaFileAlt className="file-type-icon default" />;

    if (
        ["jpg", "jpeg", "png", "gif", "webp"]
            .includes(extension)
    ) {
        icon =
            <FaFileImage className="file-type-icon image" />;
    }

    else if (extension === "pdf") {

        icon =
            <FaFilePdf className="file-type-icon pdf" />;

    }

    else if (
        ["mp4", "avi", "mov", "mkv"]
            .includes(extension)
    ) {

        icon =
            <FaFileVideo className="file-type-icon video" />;

    }

    else if (
        ["mp3", "wav"]
            .includes(extension)
    ) {

        icon =
            <FaFileAudio className="file-type-icon audio" />;

    }

    else if (
        ["doc", "docx"]
            .includes(extension)
    ) {

        icon =
            <FaFileWord className="file-type-icon word" />;

    }

    else if (
        ["xls", "xlsx"]
            .includes(extension)
    ) {

        icon =
            <FaFileExcel className="file-type-icon excel" />;

    }

    else if (
        ["ppt", "pptx"]
            .includes(extension)
    ) {

        icon =
            <FaFilePowerpoint className="file-type-icon ppt" />;

    }

    else if (
        ["zip", "rar"]
            .includes(extension)
    ) {

        icon =
            <FaFileArchive className="file-type-icon zip" />;

    }


    // =========================
    // OPEN FILE
    // =========================

    const openFile = async (id) => {

        try {

            // Mark file as viewed
            try {

                await API.get(
                    `/files/mark-viewed/${id}`
                );

            } catch (error) {

                console.log(
                    "Mark viewed failed:",
                    error
                );

            }

            // Open file
            const response = await API.get(
                `/files/view/${id}`,
                {
                    responseType: "blob"
                }
            );

            const blob = new Blob(
                [response.data],
                {
                    type:
                        response.headers["content-type"]
                        || "application/octet-stream"
                }
            );

            const url =
                window.URL.createObjectURL(blob);

            window.open(
                url,
                "_blank"
            );

        } catch (error) {

            console.log(
                "OPEN SEARCH FILE ERROR:",
                error
            );

            alert("Cannot open file");

        }

    };


    // =========================
    // DELETE
    // =========================


    const handleDelete = async () => {

        try {

            if (isShared) {

                await API.delete(
                    `/files/${file.id}/remove-from-shared`
                );

                showPopup(
                    "Removed from Shared With Me",
                    "success"
                );

            } else {

                await API.delete(
                    `/files/${file.id}`
                );

                showPopup(
                    "File moved to Trash",
                    "success"
                );

            }

            setMenuOpen(false);

            if (loadFiles) {
                loadFiles();
            }

        } catch (error) {

            console.log(error);

            showPopup(
                "Delete failed",
                "error"
            );

        }

    };

    // =========================
    // NORMAL RENAME
    // =========================

    const handleRename = async () => {

        if (!newName.trim()) {
            return;
        }

        try {

            await API.put(
                `/files/rename/${file.id}`,
                null,
                {
                    params: {
                        newName: newName
                    }
                }
            );

            setRenameMode(false);
            setNewName("");

            loadFiles();

        }

        catch (error) {

            console.log(error);

        }

    };


    // =========================
    // AI RENAME
    // =========================

    const handleAIRename = async () => {

        setMenuOpen(false);

        setShowAIRename(true);

        setAiLoading(true);

        setAiSuggestedName("");

        setAiError("");

        try {

            const response =
                await API.post(
                    "/ai/rename",
                    {
                        fileId: file.id
                    }
                );

            console.log(
                "AI RENAME RESPONSE:",
                response.data
            );

            setAiSuggestedName(
                response.data.suggestedName
            );

        }

        catch (error) {

            console.log(
                "AI RENAME ERROR:",
                error
            );

            setAiError(
                "AI could not generate a filename."
            );

        }

        finally {

            setAiLoading(false);

        }

    };


    // =========================
    // CONFIRM AI RENAME
    // =========================

    const confirmAIRename = async () => {

        if (
            !aiSuggestedName ||
            !aiSuggestedName.trim()
        ) {
            return;
        }

        try {

            await API.put(
                `/files/rename/${file.id}`,
                null,
                {
                    params: {
                        newName:
                            aiSuggestedName.trim()
                    }
                }
            );

            setShowAIRename(false);

            setAiSuggestedName("");

            loadFiles();

        }

        catch (error) {

            console.log(
                "AI RENAME SAVE ERROR:",
                error
            );

            setAiError(
                "Could not rename the file."
            );

        }

    };


    // =========================
    // REMOVE FROM RECENT
    // =========================

    const removeFromRecent = async () => {

        try {

            await API.put(
                `/files/${file.id}/remove-recent`
            );

            showPopup(
                "Removed from recent",
                "success"
            );

            loadFiles();

        }

        catch (error) {

            console.log(error);

            showPopup(
                "Failed to remove from recent",
                "error"
            );

        }

    };


    // =========================
    // STAR
    // =========================

    const handleStar = async () => {

        try {

            await API.put(
                `/files/${file.id}/star`
            );

            showPopup(
                file.starred
                    ? "File unstarred"
                    : "File starred",
                "success"
            );

            setMenuOpen(false);

            loadFiles();

        }

        catch (error) {

            console.log(error);

            showPopup(
                "Failed to update star",
                "error"
            );

        }

    };


    // =========================
    // ADD TO MY DRIVE
    // =========================
    const addToMyDrive = async (id) => {

        try {

            const response = await API.post(
                `/files/${id}/add-to-drive`
            );

            if (response.data === "Already in My Drive") {

                showPopup(
                    "Already in My Drive",
                    "info"
                );

            } else {

                showPopup(
                    "Added to My Drive",
                    "success"
                );

                loadFiles &&
                    loadFiles();
            }

        } catch (error) {

            console.log(error);

            showPopup(
                "Failed to add file",
                "error"
            );

        }

    };
    // =========================
    // SEND FILE BY EMAIL
    // =========================

    // =========================
    // SEND FILE BY EMAIL
    // =========================

    const sendFileByEmail = async () => {

        if (!recipientEmail.trim()) {
            return;
        }

        try {

            setEmailSending(true);

            await API.post(
                `/files/${file.id}/send-email`,
                null,
                {
                    params: {
                        email: recipientEmail.trim()
                    }
                }
            );

            setEmailSending(false);
            setShowEmailModal(false);

            setEmailSuccess(true);

            setRecipientEmail("");

        } catch (error) {

            console.log(
                "SEND EMAIL ERROR:",
                error
            );

            setEmailSending(false);

            showPopup(
                error.response?.data?.message ||
                "Unable to send email. Please try again.",
                "error"
            );

        }

    };



    return (

        <div className="file-card">
            {/* =========================
    EMAIL SUCCESS POPUP
========================= */}

            {emailSuccess && (

                <div className="email-success-overlay">


                    <div className="email-success-modal">

                        <div className="email-success-icon">
                            ✓
                        </div>

                        <h2>
                            Email Sent Successfully
                        </h2>

                        <p>
                            <strong>{file.fileName}</strong>
                            <br />
                            has been sent successfully to
                            <br />
                            <span>{recipientEmail}</span>
                        </p>

                        <button
                            className="email-success-btn"
                            onClick={() =>
                                setEmailSuccess(false)
                            }
                        >
                            Done
                        </button>

                    </div>

                </div>

            )}
            {/* =========================
    SEND EMAIL MODAL
========================= */}

            {showEmailModal && (

                <div
                    className="email-modal-overlay"
                    onClick={() => {
                        if (!emailSending) {
                            setShowEmailModal(false);
                        }
                    }}
                >

                    <div
                        className="email-modal"
                        onClick={(e) => e.stopPropagation()}
                    >

                        <div className="email-modal-icon">
                            📧
                        </div>

                        <h2>
                            Send File
                        </h2>

                        <p className="email-modal-description">
                            Send <strong>{file.fileName}</strong> to another email address.
                        </p>

                        <label>
                            Recipient Email
                        </label>

                        <input
                            type="email"
                            placeholder="example@gmail.com"
                            value={recipientEmail}
                            onChange={(e) =>
                                setRecipientEmail(e.target.value)
                            }
                            disabled={emailSending}
                        />

                        <div className="email-modal-actions">

                            <button
                                className="email-cancel-btn"
                                onClick={() =>
                                    setShowEmailModal(false)
                                }
                                disabled={emailSending}
                            >
                                Cancel
                            </button>

                            <button
                                className="email-send-btn"
                                onClick={sendFileByEmail}
                                disabled={
                                    emailSending ||
                                    !recipientEmail.trim()
                                }
                            >
                                {emailSending
                                    ? "Sending..."
                                    : "📧 Send Email"
                                }
                            </button>

                        </div>

                    </div>

                </div>

            )}

            {/* =========================
            POPUP
        ========================= */}

            {popup.show && (

                <div
                    className={`file-popup ${popup.type}`}
                >

                    <span className="file-popup-icon">
                        {popup.type === "success" ? "✓" : "✕"}
                    </span>

                    <span>
                        {popup.message}
                    </span>

                </div>

            )}


            {/* =========================
            FILE ICON
        ========================= */}

            <div
                className="file-icon"
                onClick={() => openFile(file.id)}
            >
                {icon}
            </div>


            {/* =========================
            FILE NAME
        ========================= */}

            <div className="file-name-cell">

                <h4>
                    {file.fileName}
                </h4>

            </div>


            {/* =========================
            FILE SIZE
        ========================= */}

            <div className="file-size-cell">

                <p>
                    {file.fileSize
                        ? (file.fileSize / 1024).toFixed(1) + " KB"
                        : "—"
                    }
                </p>

            </div>


            {/* =========================
            DATE
        ========================= */}

            <div className="file-date-cell">

                <p>

                    {file.uploadDate
                        ? new Date(
                            file.uploadDate
                        ).toLocaleDateString(
                            "en-IN",
                            {
                                day: "2-digit",
                                month: "short",
                                year: "numeric"
                            }
                        )
                        : "—"
                    }

                </p>

            </div>


            {/* =========================
            LOCATION
        ========================= */}

            <div className="file-location-cell">

                <p>
                    📁 {file.location || "My Drive"}
                </p>

            </div>


            {/* =========================
            OPEN BUTTON
        ========================= */}

            <div className="file-open-cell">

                <button
                    className="open-btn"
                    onClick={() => openFile(file.id)}
                >
                    Open
                </button>

            </div>


            {/* =========================
            THREE DOT MENU
        ========================= */}

            <div
                className="menu-container"
                ref={menuRef}
            >

                <button
                    className="menu-btn"
                    onClick={(e) => {

                        e.stopPropagation();

                        setMenuOpen(
                            prev => !prev
                        );

                    }}
                >

                    <FaEllipsisV />

                </button>


                {menuOpen && (

                    <div
                        className="file-menu"
                        onClick={(e) =>
                            e.stopPropagation()
                        }
                    >

                        {isTrash ? (

                            <>

                                <button
                                    onClick={() => {

                                        onRestore(
                                            file.id
                                        );

                                        setMenuOpen(false);

                                    }}
                                >
                                    ♻️ Restore
                                </button>


                                <button
                                    onClick={() => {

                                        onDeletePermanent(
                                            file.id
                                        );

                                        setMenuOpen(false);

                                    }}
                                >
                                    🗑 Delete Permanently
                                </button>

                            </>

                        ) : (

                            <>

                                {/* RENAME */}

                                <button
                                    onClick={() => {

                                        setRenameMode(true);
                                        setMenuOpen(false);

                                    }}
                                >
                                    ✏️ Rename
                                </button>


                                {/* AI RENAME */}

                                <button
                                    onClick={handleAIRename}
                                >
                                    🧠 AI Rename
                                </button>


                                {/* CLEAR RECENT */}

                                {isRecent && (

                                    <button
                                        onClick={() => {

                                            removeFromRecent();
                                            setMenuOpen(false);

                                        }}
                                    >
                                        🕒 Clear Recent
                                    </button>

                                )}


                                {/* TRASH */}

                                <button
                                    onClick={() => {

                                        handleDelete();
                                        setMenuOpen(false);

                                    }}
                                >
                                    🗑 Move to Trash
                                </button>


                                {/* SHARE */}

                                <button
                                    onClick={() => {

                                        setShareFile(file);
                                        setMenuOpen(false);

                                    }}
                                >
                                    🔗 Share
                                </button>
                                {/* CONVERT FILE */}

                                {/* CONVERT FILE */}

                                <button
                                    onClick={() => {

                                        console.log("🔥 Convert File clicked");

                                        setMenuOpen(false);
                                        setShowConvertModal(true);

                                    }}
                                >
                                    🔄 Convert File
                                </button>
                                {/* COMPRESS FILE */}

                                <button
                                    onClick={() => {
                                        setMenuOpen(false);
                                        setShowCompressModal(true);
                                    }}
                                >
                                    📦 Compress File
                                </button>
                                {/* SEND BY EMAIL */}

                                <button
                                    onClick={() => {

                                        setMenuOpen(false);
                                        setShowEmailModal(true);

                                    }}
                                >
                                    📧 Send by Email
                                </button>

                                {/* STAR */}

                                <button
                                    onClick={handleStar}
                                >
                                    {file.starred
                                        ? "☆ Unstar"
                                        : "⭐ Star"
                                    }
                                </button>


                                {/* AI ASSISTANT */}

                                <button
                                    onClick={() => {

                                        navigate(
                                            `/ai?fileId=${file.id}`
                                        );

                                        setMenuOpen(false);

                                    }}
                                >
                                    🤖 AI Assistant
                                </button>

                            </>

                        )}


                    </div>

                )}

            </div>


            {/* =========================
            ADD TO MY DRIVE
        ========================= */}

            {isShared && (
                <button
                    className="add-drive-btn"
                    onClick={async () => {
                        if (addedToDrive) return;

                        await addToMyDrive(file.id);
                        setAddedToDrive(true);
                    }}
                    disabled={addedToDrive}
                >
                    {addedToDrive ? "✓ Added to My Drive" : "📥 Add to My Drive"}
                </button>
            )}

            {/* =========================
            NORMAL RENAME
        ========================= */}

            {renameMode && (

                <div
                    className="rename-box"
                    onClick={(e) =>
                        e.stopPropagation()
                    }
                >

                    <input
                        type="text"
                        placeholder="New name"
                        value={newName}
                        onChange={(e) =>
                            setNewName(e.target.value)
                        }
                    />


                    <button
                        className="save-rename-btn"
                        onClick={handleRename}
                    >
                        ✓
                    </button>


                    <button
                        className="cancel-rename-btn"
                        onClick={() => {

                            setRenameMode(false);
                            setNewName("");

                        }}
                    >
                        ✕
                    </button>

                </div>

            )}


            {/* =========================
            AI RENAME POPUP
        ========================= */}

            {showAIRename && (

                <div
                    className="ai-rename-overlay"
                    onClick={() => {

                        if (!aiLoading) {
                            setShowAIRename(false);
                        }

                    }}
                >

                    <div
                        className="ai-rename-modal"
                        onClick={(e) =>
                            e.stopPropagation()
                        }
                    >

                        <div className="ai-rename-header">

                            <div className="ai-rename-icon">
                                🧠
                            </div>

                            <div>

                                <h2>
                                    AI Rename
                                </h2>

                                <p>
                                    Let AI suggest a better filename
                                </p>

                            </div>

                        </div>


                        <div className="ai-rename-body">

                            <label>
                                Current name
                            </label>

                            <div className="ai-current-name">
                                📄 {file.fileName}
                            </div>


                            {aiLoading && (

                                <div className="ai-loading">

                                    <div className="ai-spinner"></div>

                                    <p>
                                        AI is analyzing your file...
                                    </p>

                                </div>

                            )}


                            {!aiLoading &&
                                !aiError &&
                                aiSuggestedName && (

                                    <>

                                        <label>
                                            AI suggestion
                                        </label>

                                        <input
                                            className="ai-suggested-input"
                                            value={aiSuggestedName}
                                            onChange={(e) =>
                                                setAiSuggestedName(
                                                    e.target.value
                                                )
                                            }
                                        />

                                        <p className="ai-hint">
                                            You can edit the suggested name before renaming.
                                        </p>

                                    </>

                                )}


                            {!aiLoading &&
                                aiError && (

                                    <div className="ai-error">
                                        ⚠️ {aiError}
                                    </div>

                                )}

                        </div>


                        <div className="ai-rename-actions">

                            <button
                                className="ai-cancel-btn"
                                onClick={() =>
                                    setShowAIRename(false)
                                }
                                disabled={aiLoading}
                            >
                                Cancel
                            </button>


                            {!aiLoading &&
                                !aiError &&
                                aiSuggestedName && (

                                    <button
                                        className="ai-confirm-btn"
                                        onClick={confirmAIRename}
                                    >
                                        🧠 Rename
                                    </button>

                                )}

                        </div>

                    </div>

                </div>

            )}
            {showConvertModal && (

                <ConvertFileModal
                    file={file}
                    onClose={() =>
                        setShowConvertModal(false)
                    }
                    loadFiles={loadFiles}
                />

            )}
            {showCompressModal && (

                <CompressFileModal
                    file={file}
                    onClose={() =>
                        setShowCompressModal(false)
                    }
                    loadFiles={loadFiles}
                />

            )}

        </div>
    );
}

export default FileCard;