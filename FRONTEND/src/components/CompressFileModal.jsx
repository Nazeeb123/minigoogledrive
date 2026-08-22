import { useState } from "react";
import API from "../services/api";

function CompressFileModal({ file, onClose, loadFiles }) {

    const [targetSize, setTargetSize] = useState("");
    const [unit, setUnit] = useState("KB");
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const compressFile = async () => {

        if (!targetSize || Number(targetSize) <= 0) {
            setError("Enter a valid target size.");
            return;
        }

        try {

            setLoading(true);
            setError("");
            setMessage("");

            const targetBytes =
                unit === "MB"
                    ? Number(targetSize) * 1024 * 1024
                    : Number(targetSize) * 1024;

            const response = await API.post(
                `/files/${file.id}/compress`,
                null,
                {
                    params: {
                        targetSize: targetBytes
                    }
                }
            );

            setMessage(
                response.data.message ||
                "File compressed successfully."
            );

            if (loadFiles) {
                loadFiles();
            }

        } catch (err) {

            console.log(
                "COMPRESSION ERROR:",
                err
            );

            setError(
                err.response?.data?.message ||
                err.response?.data ||
                "Compression failed."
            );

        } finally {

            setLoading(false);

        }

    };

    return (

        <div
            className="compress-modal-overlay"
            onClick={() => {
                if (!loading) {
                    onClose();
                }
            }}
        >

            <div
                className="compress-modal"
                onClick={(e) =>
                    e.stopPropagation()
                }
            >

                <h2>
                    📦 Compress File
                </h2>

                <p>
                    {file.fileName}
                </p>

                <div className="current-size">

                    Current size:

                    <strong>
                        {" "}
                        {file.fileSize >= 1024 * 1024
                            ? (
                                file.fileSize /
                                (1024 * 1024)
                            ).toFixed(2) + " MB"
                            : (
                                file.fileSize /
                                1024
                            ).toFixed(1) + " KB"
                        }
                    </strong>

                </div>

                <label>
                    Target size
                </label>

                <div className="size-input">

                    <input
                        type="number"
                        min="1"
                        placeholder="Example: 100"
                        value={targetSize}
                        onChange={(e) =>
                            setTargetSize(e.target.value)
                        }
                    />

                    <select
                        value={unit}
                        onChange={(e) =>
                            setUnit(e.target.value)
                        }
                    >

                        <option value="KB">
                            KB
                        </option>

                        <option value="MB">
                            MB
                        </option>

                    </select>

                </div>

                {message && (

                    <div className="compress-success">
                        ✅ {message}
                    </div>

                )}

                {error && (

                    <div className="compress-error">
                        ❌ {error}
                    </div>

                )}

                <div className="compress-actions">

                    <button
                        onClick={onClose}
                        disabled={loading}
                    >
                        Cancel
                    </button>

                    <button
                        onClick={compressFile}
                        disabled={loading}
                    >
                        {loading
                            ? "Compressing..."
                            : "📦 Compress"
                        }
                    </button>

                </div>

            </div>

        </div>
    );
}

export default CompressFileModal;