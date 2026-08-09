import "./Dashboard.css";
import { useEffect, useState } from "react";
import FileUpload from "../components/FileUpload";
import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";
import API from "../services/api";
import FolderCard from "../components/FolderCard";
import FileCard from "../components/FileCard";
import { getFolders, createFolder } from "../services/folderService";
import { useNavigate } from "react-router-dom";
import ShareBox from "../components/ShareBox";
import SearchResults from "../components/SearchResults";

function Dashboard() {

    const [files, setFiles] = useState([]);
    const [search, setSearch] = useState("");
    const [searchResults, setSearchResults] = useState([]);

    const [folders, setFolders] = useState([]);

    const [showFolderModal, setShowFolderModal] = useState(false);
    const [folderName, setFolderName] = useState("");

    const [recentFiles, setRecentFiles] = useState([]);

    const [shareFile, setShareFile] = useState(null);

    const [userEmail, setUserEmail] = useState("");

    const [uploadProgress, setUploadProgress] = useState(0);
    const [uploading, setUploading] = useState(false);

    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [showNotifications, setShowNotifications] = useState(false);

    const navigate = useNavigate();


    // =====================================================
    // INITIAL LOAD
    // =====================================================

    useEffect(() => {

        setUserEmail(localStorage.getItem("email"));

        loadFiles();
        loadFolders();
        loadRecentFiles();
        loadNotifications();
        loadUnreadCount();

    }, []);


    // =====================================================
    // SEARCH
    // =====================================================

    const handleSearch = async (value) => {

        setSearch(value);

        // Empty search
        if (value.trim() === "") {

            setSearchResults([]);

            return;
        }

        try {

            const response = await API.get(
                `/files/search?query=${encodeURIComponent(
                    value.trim()
                )}`
            );

            console.log(
                "SEARCH RESULTS:",
                response.data
            );

            setSearchResults(response.data);

        } catch (error) {

            console.error(
                "SEARCH ERROR:",
                error
            );

            setSearchResults([]);

        }

    };


    // =====================================================
    // SEARCH ENTER
    // =====================================================

    const handleSearchEnter = (e) => {

        if (e.key === "Enter") {

            e.preventDefault();

            if (search.trim() !== "") {

                navigate(
                    `/search?query=${encodeURIComponent(
                        search.trim()
                    )}`
                );

            }

        }

    };


    // =====================================================
    // LOAD FILES
    // =====================================================

    const loadFiles = async () => {

        try {

            const response =
                await API.get("/files/my");

            setFiles(response.data);

        } catch (error) {

            console.log(
                "LOAD FILES ERROR:",
                error
            );

        }

    };


    // =====================================================
    // LOAD FOLDERS
    // =====================================================

    const loadFolders = async () => {

        try {

            const response =
                await getFolders();

            setFolders(response.data);

        } catch (error) {

            console.log(
                "LOAD FOLDERS ERROR:",
                error
            );

        }

    };


    // =====================================================
    // DOWNLOAD FILE
    // =====================================================

    const downloadFile = async (id) => {

        try {

            const response =
                await API.get(
                    `/files/download/${id}`,
                    {
                        responseType: "blob",
                    }
                );

            const url =
                window.URL.createObjectURL(
                    new Blob([response.data])
                );

            const link =
                document.createElement("a");

            link.href = url;
            link.download = "download";

            document.body.appendChild(link);

            link.click();

            link.remove();

            window.URL.revokeObjectURL(url);

        } catch (error) {

            console.log(
                "DOWNLOAD ERROR:",
                error
            );

        }

    };


    // =====================================================
    // MOVE FILE TO TRASH
    // =====================================================

    const moveFileToTrash = async (id) => {

        try {

            await API.post(
                `/files/${id}/trash`
            );

            alert("File moved to Trash");

            loadFiles();
            loadRecentFiles();

        } catch (error) {

            console.log(
                "TRASH ERROR:",
                error
            );

        }

    };


    // =====================================================
    // STAR FILE
    // =====================================================

    const starFile = async (id) => {

        try {

            await API.put(
                `/files/${id}/star`
            );

            alert("Star updated");

            loadFiles();

        } catch (error) {

            console.log(
                "STAR ERROR:",
                error
            );

        }

    };


    // =====================================================
    // CREATE FOLDER
    // =====================================================

    const handleCreateFolder = async () => {

        if (folderName.trim() === "") {

            alert("Enter folder name");

            return;
        }

        try {

            await createFolder(
                folderName
            );

            setFolderName("");

            setShowFolderModal(false);

            loadFolders();

        } catch (error) {

            console.log(
                "CREATE FOLDER ERROR:",
                error
            );

        }

    };


    // =====================================================
    // LOAD RECENT FILES
    // =====================================================

    const loadRecentFiles = async () => {

        try {

            const response =
                await API.get("/files/recent");

            setRecentFiles(
                response.data
            );

        } catch (error) {

            console.log(
                "RECENT FILES ERROR:",
                error
            );

        }

    };


    // =====================================================
    // LOAD NOTIFICATIONS
    // =====================================================

    const loadNotifications = async () => {

        try {

            const response =
                await API.get(
                    "/notifications"
                );

            setNotifications(
                response.data
            );

        } catch (error) {

            console.log(
                "NOTIFICATION ERROR:",
                error
            );

        }

    };


    // =====================================================
    // LOAD UNREAD NOTIFICATIONS
    // =====================================================

    const loadUnreadCount = async () => {

        try {

            const response =
                await API.get(
                    "/notifications/unread-count"
                );

            setUnreadCount(
                response.data
            );

        } catch (error) {

            console.log(
                "UNREAD NOTIFICATION ERROR:",
                error
            );

        }

    };


    // =====================================================
    // MARK NOTIFICATION READ
    // =====================================================

    const markNotificationRead = async (id) => {

        try {

            await API.put(
                `/notifications/${id}/read`
            );

            loadNotifications();
            loadUnreadCount();

        } catch (error) {

            console.log(
                "MARK NOTIFICATION ERROR:",
                error
            );

        }

    };


    // =====================================================
    // LOAD FILES AFTER FILE ACTION
    // =====================================================

    const refreshAllFiles = () => {

        loadFiles();
        loadRecentFiles();

    };


    // =====================================================
    // UI
    // =====================================================

    return (

        <div className="dashboard-container">

            {/* =========================
                    SIDEBAR
                ========================= */}

            <Sidebar />


            {/* =========================
                    DASHBOARD
                ========================= */}

            <div className="dashboard">


                {/* =========================
                        NAVBAR
                    ========================= */}

                <Navbar

                    search={search}

                    setSearch={handleSearch}

                    onSearchEnter={
                        handleSearchEnter
                    }

                    notifications={
                        notifications
                    }

                    unreadCount={
                        unreadCount
                    }

                    showNotifications={
                        showNotifications
                    }

                    setShowNotifications={
                        setShowNotifications
                    }

                    markNotificationRead={
                        markNotificationRead
                    }

                />


                {/* =========================
                        FILE UPLOAD
                    ========================= */}

                <FileUpload
                    refreshFiles={
                        loadFiles
                    }
                />


                {/* =========================
                        SEARCH RESULTS
                    ========================= */}

                {search && (

                    <SearchResults
                        results={
                            searchResults
                        }
                    />

                )}


                {/* =========================
                        WELCOME
                    ========================= */}

                <h1>
                    Welcome to Your Drive 👋
                </h1>


                {/* =========================
                        MY DRIVE HEADER
                    ========================= */}

                <div className="drive-header">

                    <h2>
                        📁 My Drive
                    </h2>

                    <button
                        className="new-folder-btn"
                        onClick={() =>
                            setShowFolderModal(true)
                        }
                    >
                        + New Folder
                    </button>

                </div>


                {/* =========================
                        FOLDERS
                    ========================= */}

                <div className="folders-grid">

                    {folders.map(
                        (folder) => (

                            <div
                                key={folder.id}
                                onClick={() =>
                                    navigate(
                                        `/folder/${folder.id}`
                                    )
                                }
                            >

                                <FolderCard

                                    folder={
                                        folder
                                    }

                                    loadFolders={
                                        loadFolders
                                    }

                                />

                            </div>

                        )
                    )}

                </div>


                {/* =========================
                        RECENT FILES
                    ========================= */}

                <h2>
                    🕒 Recent Files
                </h2>

                <div className="files-grid">

                    {recentFiles.length === 0 ? (

                        <p>
                            No recent files
                        </p>

                    ) : (

                        recentFiles.map(
                            (file) => (

                                <FileCard

                                    key={
                                        file.id
                                    }

                                    file={
                                        file
                                    }

                                    isRecent={
                                        true
                                    }

                                    setShareFile={
                                        setShareFile
                                    }

                                    loadFiles={
                                        refreshAllFiles
                                    }

                                    onDelete={() =>
                                        moveFileToTrash(
                                            file.id
                                        )
                                    }

                                />

                            )
                        )

                    )}

                </div>


            </div>


            {/* =========================
                    CREATE FOLDER MODAL
                ========================= */}

            {showFolderModal && (

                <div className="modal">

                    <div className="modal-content">

                        <h2>
                            Create Folder
                        </h2>


                        <input

                            type="text"

                            placeholder="Folder name"

                            value={
                                folderName
                            }

                            onChange={(e) =>
                                setFolderName(
                                    e.target.value
                                )
                            }

                        />


                        <button
                            onClick={
                                handleCreateFolder
                            }
                        >
                            Create
                        </button>


                        <button
                            onClick={() =>
                                setShowFolderModal(
                                    false
                                )
                            }
                        >
                            Cancel
                        </button>

                    </div>

                </div>

            )}


            {/* =========================
                    SHARE BOX
                ========================= */}

            {shareFile && (

                <ShareBox

                    shareFile={
                        shareFile
                    }

                    setShareFile={
                        setShareFile
                    }

                />

            )}

        </div>

    );

}

export default Dashboard;