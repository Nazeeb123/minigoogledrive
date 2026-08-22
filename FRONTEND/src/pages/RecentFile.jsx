import { useEffect, useState } from "react";
import Sidebar from "../components/SideBar";
import Navbar from "../components/NavBar";
import FileCard from "../components/FileCard";
import API from "../services/api";
import "./Dashboard.css";

function Recent() {

    const [files, setFiles] = useState([]);

    useEffect(() => {

        loadRecentFiles();

    }, []);

    const loadRecentFiles = async () => {

        try {

            const response = await API.get("/files/recent");

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

                <h1>🕒 Recent Files</h1>

                <div className="files-grid">

                    {files.map(file => (

                        <FileCard
                            key={file.id}
                            file={file}
                            loadFiles={loadRecentFiles}
                            setShareFile={setShareFile}
                        />

                    ))}

                </div>

            </div>

        </div>

    );

}

export default Recent;