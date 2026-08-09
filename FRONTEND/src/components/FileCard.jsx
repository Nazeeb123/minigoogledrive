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

    const [menuOpen, setMenuOpen] = useState(false);

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

    const openFile = async (e) => {

        e?.stopPropagation();

        try {

            try {

                await API.get(
                    `/files/mark-viewed/${file.id}`
                );

            } catch (error) {

                console.log(
                    "Mark viewed failed:",
                    error
                );

            }

            const response =
                await API.get(
                    `/files/view/${file.id}`,
                    {
                        responseType: "blob"
                    }
                );

            const blob =
                new Blob(
                    [response.data],
                    {
                        type:
                            response.headers[
                            "content-type"
                            ]
                    }
                );

            const url =
                window.URL.createObjectURL(blob);

            window.open(
                url,
                "_blank"
            );

        }

        catch (error) {

            console.log(
                "OPEN FILE ERROR:",
                error
            );

            alert(
                "Cannot open file"
            );

        }

    };


    // =========================
    // DELETE
    // =========================

    const handleDelete = async () => {

        try {

            const response =
                await API.delete(
                    `/files/${file.id}`
                );

            console.log(
                response.data
            );

            alert(
                "File moved to Trash"
            );

            loadFiles();

        }

        catch (error) {

            console.log(error);

            alert(
                "Delete failed"
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

            alert(
                "Removed from recent"
            );

            loadFiles();

        }

        catch (error) {

            console.log(error);

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

            alert(
                "Star updated"
            );

            setMenuOpen(false);

            loadFiles();

        }

        catch (error) {

            console.log(error);

        }

    };


    // =========================
    // ADD TO MY DRIVE
    // =========================

    const addToMyDrive = async (id) => {

        try {

            await API.post(
                `/files/${id}/add-to-drive`
            );

            alert(
                "Added to My Drive"
            );

            loadFiles &&
                loadFiles();

        }

        catch (error) {

            console.log(error);

            alert(
                "Failed to add file"
            );

        }

    };


    return (

        <div className="file-card">


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
                            !menuOpen
                        );

                    }}
                >

                    <FaEllipsisV />

                </button>


                {menuOpen && (

                    <div className="file-menu">


                        {isTrash ? (

                            <>

                                <button
                                    onClick={() => {

                                        onRestore(
                                            file.id
                                        );

                                        setMenuOpen(
                                            false
                                        );

                                    }}
                                >
                                    ♻️ Restore
                                </button>


                                <button
                                    onClick={() => {

                                        onDeletePermanent(
                                            file.id
                                        );

                                        setMenuOpen(
                                            false
                                        );

                                    }}
                                >
                                    🗑 Delete Permanently
                                </button>

                            </>

                        ) : (

                            <>


                                {/* NORMAL RENAME */}

                                <button
                                    onClick={() => {

                                        setRenameMode(
                                            true
                                        );

                                        setMenuOpen(
                                            false
                                        );

                                    }}
                                >
                                    ✏️ Rename
                                </button>


                                {/* AI RENAME */}

                                <button
                                    onClick={
                                        handleAIRename
                                    }
                                >
                                    🧠 AI Rename
                                </button>


                                {/* CLEAR RECENT */}

                                {isRecent && (

                                    <button
                                        onClick={() => {

                                            removeFromRecent();

                                            setMenuOpen(
                                                false
                                            );

                                        }}
                                    >
                                        🕒 Clear Recent
                                    </button>

                                )}


                                {/* TRASH */}

                                <button
                                    onClick={() => {

                                        handleDelete();

                                        setMenuOpen(
                                            false
                                        );

                                    }}
                                >
                                    🗑 Move to Trash
                                </button>


                                {/* SHARE */}

                                <button
                                    onClick={() => {

                                        console.log(
                                            "SHARE CLICKED",
                                            file
                                        );

                                        setShareFile(
                                            file
                                        );

                                        setMenuOpen(
                                            false
                                        );

                                    }}
                                >
                                    🔗 Share
                                </button>


                                {/* STAR */}

                                <button
                                    onClick={
                                        handleStar
                                    }
                                >
                                    {file.starred
                                        ? "☆ Unstar"
                                        : "⭐ Star"}
                                </button>


                                {/* AI ASSISTANT */}

                                <button
                                    onClick={() => {

                                        navigate(
                                            `/ai?fileId=${file.id}`
                                        );

                                        setMenuOpen(
                                            false
                                        );

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
                FILE ICON
            ========================= */}

            <div
                className="file-icon"
                onClick={openFile}
                style={{
                    cursor: "pointer"
                }}
            >

                {icon}

            </div>


            {/* =========================
                FILE NAME
            ========================= */}

            <h4>
                {file.fileName}
            </h4>


            {/* =========================
                FILE SIZE
            ========================= */}

            <p>
                {(file.fileSize / 1024)
                    .toFixed(1)} KB
            </p>


            {/* =========================
                OPEN BUTTON
            ========================= */}

            <button
                className="open-btn"
                onClick={openFile}
            >
                👁 Open
            </button>


            {isShared && (

                <button
                    className="add-drive-btn"
                    onClick={() =>
                        addToMyDrive(
                            file.id
                        )
                    }
                >
                    📥 Add to My Drive
                </button>

            )}


            {/* =========================
                NORMAL RENAME BOX
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
                            setNewName(
                                e.target.value
                            )
                        }
                    />


                    <button
                        className="save-rename-btn"
                        onClick={
                            handleRename
                        }
                    >
                        ✓
                    </button>


                    <button
                        className="cancel-rename-btn"
                        onClick={() => {

                            setRenameMode(
                                false
                            );

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
                                            value={
                                                aiSuggestedName
                                            }
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
                                        onClick={
                                            confirmAIRename
                                        }
                                    >
                                        🧠 Rename
                                    </button>

                                )}

                        </div>

                    </div>

                </div>

            )}

        </div>

    );

}

export default FileCard;