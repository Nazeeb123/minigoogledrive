import { useState } from "react";
import API from "../services/api";
import "./FileUpload.css";

function FileUpload({ refreshFiles }) {

    const MAX_FILE_SIZE = 100 * 1024 * 1024;

    const [file, setFile] = useState(null);
    const [fileName, setFileName] = useState("");
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [uploadMessage, setUploadMessage] = useState("");

    const uploadFile = async () => {

        if (!file) {
            alert("Please select a file");
            return;
        }

        if (file.size > MAX_FILE_SIZE) {
            alert("File size cannot exceed 100 MB");
            return;
        }

        const formData = new FormData();

        formData.append("file", file);

        if (fileName && fileName.trim()) {
            formData.append("fileName", fileName.trim());
        }

        try {

            setUploading(true);
            setUploadProgress(0);
            setUploadMessage("Uploading your file...");

            const response = await API.post(
                "/files/upload",
                formData,
                {
                    onUploadProgress: (progressEvent) => {

                        if (progressEvent.total) {

                            const percent = Math.round(
                                (progressEvent.loaded * 100) /
                                progressEvent.total
                            );

                            setUploadProgress(percent);

                            if (percent < 100) {
                                setUploadMessage(
                                    `Uploading your file... ${percent}%`
                                );
                            } else {
                                setUploadMessage(
                                    "Processing file... 100%"
                                );
                            }
                        }
                    }
                }
            );

            console.log("UPLOAD SUCCESS:", response.data);

            setUploadProgress(100);
            setUploadMessage("✅ File uploaded successfully!");

            setFile(null);
            setFileName("");

            // Refresh dashboard after successful upload
            await refreshFiles();

        } catch (error) {

            console.error(
                "UPLOAD ERROR:",
                error.response?.data || error
            );

            setUploadMessage("❌ File upload failed");

            alert(
                error.response?.data?.message ||
                "File upload failed"
            );

        } finally {

            setTimeout(() => {
                setUploading(false);
                setUploadProgress(0);
                setUploadMessage("");
            }, 2000);
        }
    };

    return (
        <div className="file-upload">

            <label className="choose-file">
                📁 Choose File

                <input
                    type="file"
                    accept=".pdf,.jpg,.jpeg,.png,.doc,.docx,.txt,.mp3,.wav,.ogg,.m4a,audio/*"
                    onChange={(e) => {
                        const selectedFile = e.target.files[0];

                        if (selectedFile && selectedFile.size > MAX_FILE_SIZE) {
                            alert("File size cannot exceed 100 MB");
                            e.target.value = "";
                            setFile(null);
                            return;
                        }

                        setFile(selectedFile);
                        setUploadMessage("");
                    }}
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

            <button
                onClick={uploadFile}
                disabled={uploading}
            >
                {uploading ? "Uploading..." : "⬆ Upload"}
            </button>


            {/* UPLOAD PROGRESS */}

            {uploading && (
                <div className="upload-progress">

                    <div className="upload-progress-message">
                        {uploadMessage}
                    </div>

                    <div className="progress-track">

                        <div
                            className="progress-bar"
                            style={{
                                width: `${uploadProgress}%`
                            }}
                        />

                    </div>

                    <div className="upload-percentage">
                        {uploadProgress}%
                    </div>

                </div>
            )}

        </div>
    );
}

export default FileUpload;