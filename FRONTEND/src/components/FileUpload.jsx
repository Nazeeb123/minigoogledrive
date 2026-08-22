import { useState } from "react";
import API from "../services/api";
import "./FileUpload.css";

function FileUpload({ refreshFiles }) {

    const [file, setFile] = useState(null);
    const [fileName, setFileName] = useState("");
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);


    const uploadFile = async () => {

        if (!file) {
            alert("Please select a file");
            return;
        }

        const formData = new FormData();

        formData.append("file", file);
        formData.append("fileName", fileName);

        try {

            setUploading(true);
            setUploadProgress(0);

            await API.post(
                "/files/upload",
                formData,
                {
                    headers: {
                        "Content-Type": "multipart/form-data"
                    },

                    onUploadProgress: (progressEvent) => {

                        const percent = Math.round(
                            (progressEvent.loaded * 100) /
                            progressEvent.total
                        );

                        setUploadProgress(percent);

                    }

                }
            );

            setUploadProgress(100);

            alert("File uploaded successfully");

            refreshFiles();

        } catch (error) {

            console.log(error);

            alert("Upload failed");

        } finally {

            setTimeout(() => {

                setUploading(false);
                setUploadProgress(0);

            }, 1000);

        }

    };


    return (
        <div className="file-upload">

            <label className="choose-file">
                📁 Choose File
                <input
                    type="file"
                    onChange={(e) => setFile(e.target.files[0])}
                    hidden
                />
            </label>

            <span className="selected-file">
                {file ? file.name : "No file chosen"}
            </span>

            <input
                type="text"
                placeholder="Enter file name"
                value={fileName}
                onChange={(e) => setFileName(e.target.value)}
            />

            <button onClick={uploadFile}>
                ⬆ Upload
            </button>
            {uploading && (
                <div className="upload-popup">

                    <h4>Uploading...</h4>

                    <p>{file ? file.name : fileName}</p>

                    <div className="upload-bar">
                        <div
                            className="upload-fill"
                            style={{ width: `${uploadProgress}%` }}
                        />
                    </div>

                    <p>{uploadProgress}%</p>

                </div>
            )}

        </div>
    );
}

export default FileUpload;
