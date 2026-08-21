import { useState } from "react";
import API from "../services/api";
import "./ConvertFileModal.css";

function ConvertFileModal({ file, onClose, loadFiles }) {

    const [format, setFormat] = useState("");
    const [converting, setConverting] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    if (!file) {
        return null;
    }

    const extension = file.fileName
        ?.split(".")
        .pop()
        ?.toLowerCase();

    const getFormats = () => {

        if (
            ["jpg", "jpeg", "png"].includes(extension)
        ) {
            return ["pdf", "docx"];
        }

        if (extension === "webp") {
            return ["pdf"];
        }

        if (extension === "pdf") {
            return ["jpg", "png"];
        }

        return [];
    };

    const formats = getFormats();

    const handleConvert = async () => {

        console.log("================================");
        console.log("🔥 CONVERT BUTTON CLICKED");
        console.log("File:", file);
        console.log("File ID:", file.id);
        console.log("Extension:", extension);
        console.log("Selected format:", format);
        console.log("Available formats:", formats);
        console.log("================================");

        if (!format) {
            setError("Please select a format first.");
            return;
        }

        try {

            setConverting(true);
            setError("");
            setMessage("");

            console.log(
                "🚀 Sending conversion request..."
            );

            const response = await API.post(
                `/files/${file.id}/convert`,
                null,
                {
                    params: {
                        format: format
                    }
                }
            );

            console.log(
                "✅ CONVERSION RESPONSE:",
                response
            );

            setMessage(
                "File converted successfully!"
            );

            if (loadFiles) {
                await loadFiles();
            }

            setTimeout(() => {
                onClose();
            }, 1500);

        } catch (err) {

            console.error(
                "❌ CONVERSION ERROR:",
                err
            );

            console.error(
                "Response:",
                err.response
            );

            console.error(
                "Response data:",
                err.response?.data
            );

            setError(
                err.response?.data?.message ||
                "Unable to convert the file."
            );

        } finally {

            setConverting(false);

        }
    };

    return (

        <div
            className="convert-overlay"
            onClick={() => {
                if (!converting) {
                    onClose();
                }
            }}
        >

            <div
                className="convert-modal"
                onClick={(e) => e.stopPropagation()}
            >

                {/* HEADER */}

                <div className="convert-header">

                    <div className="convert-icon">
                        🔄
                    </div>

                    <div>

                        <h2>
                            Convert File
                        </h2>

                        <p>
                            {file.fileName}
                        </p>

                    </div>

                </div>


                {/* FORMAT */}

                <label>
                    Convert to
                </label>

                <select
                    value={format}
                    onChange={(e) => {
                        console.log(
                            "Selected:",
                            e.target.value
                        );

                        setFormat(e.target.value);
                        setError("");
                    }}
                    disabled={converting}
                >

                    <option value="">
                        Select format
                    </option>

                    {formats.map((item) => (

                        <option
                            key={item}
                            value={item}
                        >
                            {item.toUpperCase()}
                        </option>

                    ))}

                </select>


                {/* NO FORMAT */}

                {formats.length === 0 && (

                    <p className="convert-info">

                        Conversion for this file type
                        is not available yet.

                    </p>

                )}


                {/* SUCCESS */}

                {message && (

                    <div className="convert-success">
                        ✓ {message}
                    </div>

                )}


                {/* ERROR */}

                {error && (

                    <div className="convert-error">
                        ⚠️ {error}
                    </div>

                )}


                {/* BUTTONS */}

                <div className="convert-actions">

                    <button
                        type="button"
                        className="convert-cancel"
                        onClick={onClose}
                        disabled={converting}
                    >
                        Cancel
                    </button>


                    <button
                        type="button"
                        className="convert-button"
                        onClick={handleConvert}
                        disabled={
                            converting ||
                            !format ||
                            formats.length === 0
                        }
                    >

                        {converting
                            ? "Converting..."
                            : "🔄 Convert"
                        }

                    </button>

                </div>

            </div>

        </div>
    );
}

export default ConvertFileModal;