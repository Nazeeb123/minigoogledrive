import "./Dashboard.css";
import { useEffect, useState, useRef } from "react";

import FileUpload from "../components/FileUpload";
import Sidebar from "../components/SideBar";
import Navbar from "../components/NavBar";
import API from "../services/api";
import FolderCard from "../components/FolderCard";
import FileCard from "../components/FileCard";

import {
    getFolders,
    createFolder
} from "../services/folderService";

import { useNavigate } from "react-router-dom";

import ShareBox from "../components/ShareBox";
import SearchResults from "../components/SearchResults";


function Dashboard() {

    const [files, setFiles] = useState([]);

    const [search, setSearch] = useState("");

    const [searchResults, setSearchResults] =
        useState([]);

    // IMPORTANT:
    // This ref contains both Navbar and SearchResults.
    const searchRef = useRef(null);

    const [folders, setFolders] =
        useState([]);

    const [showFolderModal, setShowFolderModal] =
        useState(false);

    const [folderName, setFolderName] =
        useState("");

    const [recentFiles, setRecentFiles] =
        useState([]);

    const [shareFile, setShareFile] =
        useState(null);

    const [userEmail, setUserEmail] =
        useState("");

    const navigate = useNavigate();

    const [loggingOut, setLoggingOut] =
        useState(false);

    const [uploadProgress, setUploadProgress] =
        useState(0);

    const [uploading, setUploading] =
        useState(false);

    const [notifications, setNotifications] =
        useState([]);

    const [unreadCount, setUnreadCount] =
        useState(0);

    const [showNotifications, setShowNotifications] =
        useState(false);

    const [popup, setPopup] = useState({
        show: false,
        message: "",
        type: ""
    });


    // =====================================================
    // POPUP
    // =====================================================

    const showPopup = (
        message,
        type = "success"
    ) => {

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


    // =====================================================
    // INITIAL LOAD
    // =====================================================

    useEffect(() => {

        setUserEmail(
            localStorage.getItem("email")
        );

        loadFiles();
        loadFolders();
        loadRecentFiles();
        loadNotifications();
        loadUnreadCount();

    }, []);


    // =====================================================
    // CLOSE SEARCH WHEN CLICKING OUTSIDE
    // =====================================================

    useEffect(() => {

        const handleClickOutside = (event) => {

            if (!searchRef.current) {
                return;
            }

            // If click is outside the complete search area
            if (
                !searchRef.current.contains(
                    event.target
                )
            ) {

                setSearch("");

                setSearchResults([]);

            }

        };


        document.addEventListener(
            "mousedown",
            handleClickOutside
        );


        return () => {

            document.removeEventListener(
                "mousedown",
                handleClickOutside
            );

        };

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

            const query =
                value.trim();


            // =================================================
            // NORMAL SEARCH
            // =================================================

            const normalResponse =
                await API.get(
                    `/files/search?query=${encodeURIComponent(
                        query
                    )}`
                );


            // =================================================
            // SEMANTIC SEARCH
            // =================================================

            const semanticResponse =
                await API.get(
                    `/files/semantic-search?query=${encodeURIComponent(
                        query
                    )}`
                );


            console.log(
                "NORMAL SEARCH:",
                normalResponse.data
            );

            console.log(
                "SEMANTIC SEARCH:",
                semanticResponse.data
            );


            // =================================================
            // COMBINE
            // =================================================

            const combinedResults = [

                ...normalResponse.data,

                ...semanticResponse.data

            ];


            // =================================================
            // REMOVE DUPLICATES
            // =================================================

            const uniqueResults =
                Array.from(

                    new Map(

                        combinedResults.map(
                            item => [

                                `${item.type}-${item.id}`,

                                item

                            ]
                        )

                    ).values()

                );


            setSearchResults(
                uniqueResults
            );


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

    // IMPORTANT:
    // ONLY ONE handleSearchEnter exists.
    const handleSearchEnter = (e) => {

        if (e.key !== "Enter") {
            return;
        }


        e.preventDefault();


        if (search.trim() === "") {
            return;
        }


        navigate(
            `/search?query=${encodeURIComponent(
                search.trim()
            )}`
        );

    };


    // =====================================================
    // LOAD FILES
    // =====================================================

    const loadFiles = async () => {

        try {

            const response =
                await API.get("/files/my");

            console.log(
                "MY FILES RESPONSE:",
                response.data
            );

            setFiles(
                response.data
            );

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

            setFolders(
                response.data
            );

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
                        responseType: "blob"
                    }
                );


            const url =
                window.URL.createObjectURL(
                    new Blob([
                        response.data
                    ])
                );


            const link =
                document.createElement("a");


            link.href = url;

            link.download = "download";


            document.body.appendChild(link);

            link.click();

            link.remove();


            window.URL.revokeObjectURL(
                url
            );


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

            alert(
                "File moved to Trash"
            );

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

            showPopup(
                "Star updated",
                "success"
            );

            loadFiles();

        } catch (error) {

            console.log(
                "STAR ERROR:",
                error
            );

            showPopup(
                "Failed to update star",
                "error"
            );

        }

    };


    // =====================================================
    // CREATE FOLDER
    // =====================================================

    const handleCreateFolder = async () => {

        if (
            folderName.trim() === ""
        ) {

            showPopup(
                "Enter folder name",
                "error"
            );

            return;

        }


        try {

            await createFolder(
                folderName
            );


            setFolderName("");

            setShowFolderModal(
                false
            );


            loadFolders();


            showPopup(
                "Folder created successfully",
                "success"
            );


        } catch (error) {

            console.log(
                "CREATE FOLDER ERROR:",
                error
            );


            showPopup(
                "Failed to create folder",
                "error"
            );

        }

    };


    // =====================================================
    // LOAD RECENT FILES
    // =====================================================

    const loadRecentFiles = async () => {

        try {

            const response =
                await API.get(
                    "/files/recent"
                );

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
    // REFRESH FILES
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

            {/* POPUP */}

            {popup.show && (

                <div
                    className={`dashboard-popup ${popup.type}`}
                >

                    <span className="popup-icon">

                        {popup.type === "success"
                            ? "✓"
                            : "✕"
                        }

                    </span>

                    <span>
                        {popup.message}
                    </span>

                </div>

            )}


            <Sidebar />


            {/* =================================================
                DASHBOARD
            ================================================= */}

            <div className="dashboard">


                {/* =================================================
                    SEARCH AREA

                    Navbar + SearchResults are inside this ref.

                    So clicking either the search bar OR the
                    results does NOT close the search.

                    Clicking anywhere else DOES close it.
                ================================================= */}

                <div
                    ref={searchRef}
                    className="search-area"
                >

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


                    {/* SEARCH RESULTS */}

                    {search && (

                        <SearchResults
                            results={
                                searchResults
                            }
                        />

                    )}

                </div>


                {/* =================================================
                    FILE UPLOAD
                ================================================= */}

                <FileUpload
                    refreshFiles={
                        loadFiles
                    }
                />


                {/* =================================================
                    WELCOME
                ================================================= */}

                <h1>
                    Welcome to Your Drive 👋
                </h1>


                {/* =================================================
                    MY DRIVE HEADER
                ================================================= */}

                <div className="drive-header">

                    <h2>
                        📁 My Drive
                    </h2>


                    <button
                        className="new-folder-btn"
                        onClick={() =>
                            setShowFolderModal(
                                true
                            )
                        }
                    >
                        + New Folder
                    </button>

                </div>


                {/* =================================================
                    FOLDERS
                ================================================= */}

                <div className="folders-grid">

                    {folders.map(
                        (folder) => (

                            <div
                                key={
                                    folder.id
                                }

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


                {/* =================================================
                    RECENT FILES
                ================================================= */}

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


            {/* =================================================
                CREATE FOLDER MODAL
            ================================================= */}

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


            {/* =================================================
                SHARE BOX
            ================================================= */}

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