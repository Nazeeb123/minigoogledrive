import "./FolderCard.css";
import { FaEllipsisV } from "react-icons/fa";
import { deleteFolder } from "../services/folderService";
import { useState, useEffect, useRef } from "react";

function FolderCard({ folder, loadFolders }) {

    const [menuOpen, setMenuOpen] = useState(false);

    const menuRef = useRef(null);


    // ==========================================
    // CLOSE MENU WHEN CLICKING OUTSIDE
    // ==========================================

    useEffect(() => {

        const handleClickOutside = (event) => {

            if (
                menuRef.current &&
                !menuRef.current.contains(event.target)
            ) {

                setMenuOpen(false);

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


    // ==========================================
    // DELETE FOLDER
    // ==========================================

    const handleDelete = async (e) => {

        e?.stopPropagation();


        if (!window.confirm("Delete this folder?")) {
            return;
        }


        try {

            await deleteFolder(folder.id);

            setMenuOpen(false);

            loadFolders();

        }

        catch (error) {

            console.log(
                "DELETE FOLDER ERROR:",
                error
            );

            alert(
                "Failed to delete folder"
            );

        }

    };


    // ==========================================
    // RENAME
    // ==========================================

    const handleRename = (e) => {

        e.stopPropagation();

        setMenuOpen(false);

        // Rename function can be added later

        alert(
            "Folder rename coming soon"
        );

    };


    return (

        <div className="folder-card">


            {/* ==================================
                THREE DOT MENU
            ================================== */}

            <div
                className="folder-menu"
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

                        {/* RENAME */}

                        <button
                            onClick={handleRename}
                        >
                            ✏️ Rename
                        </button>


                        {/* DELETE */}

                        <button
                            onClick={handleDelete}
                        >
                            🗑 Move to Trash
                        </button>

                    </div>

                )}

            </div>


            {/* ==================================
                FOLDER ICON
            ================================== */}

            <div className="folder-icon">

                📁

            </div>


            {/* ==================================
                FOLDER NAME
            ================================== */}

            <h3>

                {folder.folderName}

            </h3>


        </div>

    );

}

export default FolderCard;