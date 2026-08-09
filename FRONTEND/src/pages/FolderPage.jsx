import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import API from "../services/api";
import Navbar from "../components/Navbar";
import Sidebar from "../components/Sidebar";
import FileCard from "../components/FileCard";
import "./Dashboard.css";

function FolderPage() {

    const { id } = useParams();

    const navigate = useNavigate();

    const [files, setFiles] = useState([]);

    useEffect(() => {

        loadFiles();

    }, []);

    const loadFiles = async () => {

        try {

            const response = await API.get(`/folders/${id}/files`);

            setFiles(response.data);

        } catch (error) {

            console.log(error);

        }

    };

    return (

        <div className="dashboard-container">

            <Sidebar />

            <div className="dashboard">

                <Navbar />

                <div className="drive-header">

                    <h2>📁 Folder</h2>

                    <button
                        className="new-folder-btn"
                        onClick={() => navigate("/dashboard")}
                    >
                        ← Back
                    </button>

                </div>

                <div className="toolbar">

                    <button className="new-folder-btn">
                        + Upload File
                    </button>

                </div>

                <div className="files-grid">

                    {files.map(file => (

                        <FileCard
                            key={file.id}
                            file={file}
                            setShareFile={setShareFile}
                            loadFiles={loadFiles}
                        />

                    ))}

                </div>

            </div>

        </div>

    );

}

export default FolderPage;