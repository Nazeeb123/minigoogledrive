import { useEffect, useState } from "react";
import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";
import FileCard from "../components/FileCard";
import API from "../services/api";
import "./Dashboard.css";
import "./Shared.css";

function Shared() {
    console.log("SHARED PAGE OPENED");
    const [files, setFiles] = useState([]);
    const [search, setSearch] = useState("");
    const [shareFile, setShareFile] = useState(null);

    const loadFiles = async () => {
        try {
            const response = await API.get("/files/shared");

            console.log("SHARED RESPONSE:", response);
            console.log("SHARED DATA:", response.data);

            setFiles(response.data);

        } catch (error) {
            console.log(error);
        }
    };

    useEffect(() => {
        console.log("LOADING SHARED FILES");
        loadFiles();
    }, []);

    return (
        <div className="dashboard-container">

            <Sidebar />

            <div className="dashboard">

                <Navbar
                    search={search}
                    setSearch={setSearch}
                />

                <h2>📤 Shared With Me</h2>

                <div className="files-grid">

                    {files.length === 0 ? (
                        <p className="empty-state">
                            No files shared with you
                        </p>
                    ) : (
                        files.map(file => (
                            <FileCard
                                key={file.id}
                                file={file}
                                isRecent={false}
                                loadFiles={loadFiles}
                                setShareFile={setShareFile}
                                isShared={true}
                            />
                        ))
                    )}

                </div>

            </div>


        </div>
    );
}

export default Shared;