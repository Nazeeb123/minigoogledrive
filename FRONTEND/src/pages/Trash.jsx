import Sidebar from "../components/SideBar";
import FileCard from "../components/FileCard";
import { useEffect, useState } from "react";
import API from "../services/api";
import "./Trash.css";

function Trash() {

    const [trashedFiles, setTrashedFiles] = useState([]);


    const loadTrashedFiles = async () => {
        try {
            const response = await API.get("/files/trash");
            setTrashedFiles(response.data);
        }
        catch (error) {
            console.log(error);
        }
    };


    const handleRestore = async (id) => {
        try {
            await API.put(`/files/restore/${id}`);

            setTrashedFiles(
                trashedFiles.filter(file => file.id !== id)
            );
        }
        catch (error) {
            console.log(error);
        }
    };


    const handleDeletePermanent = async (id) => {
        try {
            await API.delete(`/files/permanent/${id}`);

            setTrashedFiles(
                trashedFiles.filter(file => file.id !== id)
            );
        }
        catch (error) {
            console.log(error);
        }
    };


    useEffect(() => {
        loadTrashedFiles();
    }, []);



    return (

        <div style={{display:"flex"}}>

            <Sidebar />


            <div className="trash-container">

                <h3>Trash</h3>


                <div className="files-grid">

                    {
                        trashedFiles.length === 0 ? (

                            <p className="empty-trash">No files in Trash</p>

                        ) : (

                            trashedFiles.map(file => (

                                <FileCard
                                    key={file.id}
                                    file={file}
                                    loadFiles={loadTrashedFiles}
                                    isTrash={true}
                                    onRestore={handleRestore}
                                    onDeletePermanent={handleDeletePermanent}
                                />

                            ))

                        )
                    }

                </div>


            </div>


        </div>

    );
}

export default Trash;