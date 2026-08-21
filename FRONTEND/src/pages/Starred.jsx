import { useEffect, useState } from "react";
import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";
import FileCard from "../components/FileCard";
import API from "../services/api";
import "./Dashboard.css";
import "./Starred.css";

function Starred() {

    const [files, setFiles] = useState([]);
    const [search, setSearch] = useState("");

    const loadStarredFiles = async () => {

        try {

            const response = await API.get("/files/starred");

            console.log(
                "STARRED RESPONSE:",
                response.data
            );

            setFiles(response.data);

        } catch (error) {

            console.log(
                "STARRED ERROR:",
                error
            );

        }

    };

    useEffect(() => {

        loadStarredFiles();

    }, []);

    const filteredFiles = files.filter((file) =>
        file.fileName
            .toLowerCase()
            .includes(search.toLowerCase())
    );

    return (

        <div className="dashboard-container">

            <Sidebar />

            <div className="dashboard">

                <Navbar
                    search={search}
                    setSearch={setSearch}
                />

                <h2>⭐ Starred</h2>

                <div className="files-grid">

                    {filteredFiles.length === 0 ? (

                        <p className="empty-state">
                            No starred files
                        </p>

                    ) : (

                        filteredFiles.map(file => (

                            <FileCard
                                key={file.id}
                                file={file}
                                isRecent={false}
                                loadFiles={loadStarredFiles}
                            />

                        ))

                    )}

                </div>

            </div>

        </div>

    );

}

export default Starred;