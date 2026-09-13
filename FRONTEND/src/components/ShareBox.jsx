import "./ShareBox.css";

import API from "../services/api";

import {
    FaEnvelope,
    FaLink,
    FaLinkedin,
    FaTimes
} from "react-icons/fa";

import { useState } from "react";


function ShareBox({ shareFile, setShareFile }) {

    const [email, setEmail] = useState("");
    const [sending, setSending] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState("");
    const [shareLink, setShareLink] = useState("");

    if (!shareFile) {
        return null;
    }


    // =========================
    // SEND BY EMAIL
    // =========================

    const shareEmail = async () => {

        if (!email.trim()) {

            setError("Please enter a recipient email address.");

            return;
        }

        try {

            setSending(true);
            setError("");

            await API.post(
                `/files/${shareFile.id}/share`,
                null,
                {
                    params: {
                        email: email.trim()
                    }
                }
            );

            setSuccess(true);

            setEmail("");

            setTimeout(() => {

                setSuccess(false);
                setShareFile(null);

            }, 2000);

        } catch (err) {

            console.log(
                "SHARE EMAIL ERROR:",
                err
            );

            setError(
                err.response?.data?.message ||
                "Unable to send the file. Please try again."
            );

        } finally {

            setSending(false);

        }

    };


    // =========================
    // COPY LINK
    // =========================

    const copyLink = async () => {

        try {

            const response = await API.post(
                `/files/${shareFile.id}/share-link`
            );

            const link = response.data;

            setShareLink(link);

            await navigator.clipboard.writeText(link);

            setSuccess(true);

            setTimeout(() => {

                setSuccess(false);

            }, 2000);

        } catch (err) {

            console.log(
                "COPY LINK ERROR:",
                err
            );

        }

    };


    // =========================
    // LINKEDIN
    // =========================

    const shareLinkedIn = () => {

        if (!shareLink) {
            setError("Copy the link first to generate a share link.");
            return;
        }

        const linkedInUrl =
            `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(shareLink)}`;

        window.open(
            linkedInUrl,
            "_blank",
            "width=700,height=600"
        );

    };


    return (

        <div className="share-modal-overlay">

            <div className="share-modal-content">


                {/* =========================
                    SUCCESS MESSAGE
                ========================= */}

                {success && (

                    <div className="share-success-popup">

                        <div className="share-success-icon">
                            ✓
                        </div>

                        <div>

                            <h3>
                                Successfully Shared
                            </h3>

                            <p>
                                Your file has been shared successfully.
                            </p>

                        </div>

                    </div>

                )}


                {/* =========================
                    HEADER
                ========================= */}

                <div className="share-header">

                    <h2>
                        Share File
                    </h2>

                    <p>
                        {shareFile.fileName}
                    </p>

                </div>


                {/* =========================
                    EMAIL
                ========================= */}

                <label>
                    Recipient Email
                </label>

                <input
                    type="email"
                    placeholder="Enter recipient email"
                    value={email}
                    onChange={(e) =>
                        setEmail(e.target.value)
                    }
                    disabled={sending}
                />


                {/* =========================
                    ERROR
                ========================= */}

                {error && (

                    <div className="share-error">

                        ⚠️ {error}

                    </div>

                )}


                {/* =========================
                    SEND BY EMAIL
                ========================= */}

                <button
                    className="share-btn email"
                    onClick={shareEmail}
                    disabled={sending}
                >

                    <FaEnvelope />

                    {sending
                        ? "Sending..."
                        : "Send by Email"
                    }

                </button>


                {/* =========================
                    COPY LINK
                ========================= */}

                


                {/* =========================
                    LINKEDIN
                ========================= */}


                {/* =========================
                    CANCEL
                ========================= */}

                <button
                    className="share-btn cancel"
                    onClick={() => {

                        if (!sending) {

                            setShareFile(null);

                        }

                    }}
                    disabled={sending}
                >

                    <FaTimes />

                    Cancel

                </button>


            </div>

        </div>

    );

}


export default ShareBox;