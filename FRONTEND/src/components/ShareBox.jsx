import API from "../services/api";
import {
    FaEnvelope,
    FaLink,
    FaWhatsapp,
    FaLinkedin,
    FaTimes
} from "react-icons/fa";

function ShareBox({ shareFile, setShareFile }) {

    console.log("SHAREBOX RECEIVED:", shareFile);
    const link =
        `http://localhost:5173/shared/${shareFile.id}`;


    const shareEmail = async () => {

        const email =
            document.getElementById("shareEmail").value;


        try {

            await API.post(
                `/files/${shareFile.id}/share`,
                null,
                {
                    params: {
                        email: email
                    }
                }
            );


            alert("Shared successfully");

            setShareFile(null);


        } catch (error) {

            console.log(error);

        }


    };



    return (

        <div className="modal">

            <div className="share-modal-content">

                <h2>
                    Share {shareFile.fileName}
                </h2>


                <input
                    id="shareEmail"
                    placeholder="Enter email"
                />


                <button className="share-btn email" onClick={shareEmail}>
                    <FaEnvelope />
                    Send Email
                </button>


                <button
                    className="share-btn link"
                    onClick={() => {
                        navigator.clipboard.writeText(link);
                        alert("Link copied");
                    }}
                >
                    <FaLink />
                    Copy Link
                </button>


                <button
                    className="share-btn whatsapp"
                    onClick={() => {
                        window.open(
                            `https://wa.me/?text=${link}`
                        );
                    }}
                >
                    <FaWhatsapp />
                    WhatsApp
                </button>


                <button
                    className="share-btn linkedin"
                    onClick={() => {
                        window.open(
                            `https://www.linkedin.com/sharing/share-offsite/?url=${link}`
                        );
                    }}
                >
                    <FaLinkedin />
                    LinkedIn
                </button>


                <button
                    className="share-btn cancel"
                    onClick={() => setShareFile(null)}
                >
                    <FaTimes />
                    Cancel
                </button>

            </div>

        </div>

    );

}

export default ShareBox;