import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import API from "../services/api";
import Navbar from "../components/NavBar";
import Sidebar from "../components/SideBar";
import FileCard from "../components/FileCard";
import ShareBox from "../components/ShareBox";
import "./Dashboard.css";
import { useRef } from "react";



function Folder() {

    const { id } = useParams();

    const navigate = useNavigate();

    const [files, setFiles] = useState([]);

    const [shareFile, setShareFile] = useState(null);
    const [selectedFile, setSelectedFile] = useState(null);

    const fileInputRef = useRef();
    const [search, setSearch] = useState("");
    const [folderName, setFolderName] = useState("");


    useEffect(() => {

        loadFiles();

    }, []);



    const loadFiles = async () => {

        try {

            const response = await API.get(`/folders/${id}/files`);
            console.log(response.data);

            setFiles(response.data);
            if (response.data.length > 0 && response.data[0].folder) {
                setFolderName(response.data[0].folder.folderName);
            }

        } catch (error) {

            console.log(error);

        }

    };



    const uploadFile = async (event) => {

        const file = event.target.files[0];

        if (!file) return;


        const formData = new FormData();

        formData.append("file", file);
        formData.append("email", localStorage.getItem("email"));
        formData.append("folderId", id);
        formData.append("fileName", file.name);


        try {

            await API.post("/files/upload", formData, {

                headers: {
                    "Content-Type": "multipart/form-data",
                },

            });


            alert("File uploaded successfully");

            loadFiles();


        } catch (error) {

            console.log(error);

            alert("Upload failed");

        }

    };
    const filteredFiles = files.filter(file =>
        file.fileName.toLowerCase().includes(search.toLowerCase())
    );



    return (

        <div className="dashboard-container">


            <Sidebar />


            <div className="dashboard">


                <Navbar search={search}
                    setSearch={setSearch} />


                <div className="drive-header">


                    <h2>📁 {folderName || "Folder"}</h2>


                    <button
                        className="new-folder-btn"
                        onClick={() => navigate("/dashboard")}
                    >
                        ← Back
                    </button>


                </div>



                <div className="toolbar">


                    <button
                        className="new-folder-btn"
                        onClick={() => fileInputRef.current.click()}
                    >
                        + Upload File
                    </button>


                    <input
                        type="file"
                        ref={fileInputRef}
                        style={{ display: "none" }}
                        onChange={uploadFile}
                    />


                </div>




                <div className="files-grid">


                    {files.length === 0 ? (

                        <h3>No files in this folder</h3>


                    ) : (


                        filteredFiles.map(file => (


                            <FileCard

                                key={file.id}

                                file={file}

                                setShareFile={setShareFile}

                                loadFiles={loadFiles}
                                onFileClick={(file) => setSelectedFile(file)}
                            />


                        ))


                    )}


                </div>



            </div>



            {shareFile && (

                <ShareBox

                    shareFile={shareFile}

                    setShareFile={setShareFile}

                />

            )}



        </div>

    );

}


export default Folder;