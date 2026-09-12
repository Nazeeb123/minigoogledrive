import { useNavigate } from "react-router-dom";
import API from "../services/api";
import "./SearchResults.css";
import React from "react";

function SearchResults({ results = [] }) {

    const navigate = useNavigate();


    // =====================================================
    // OPEN FILE
    // =====================================================

    const openFile = async (id) => {
        const previewWindow = window.open("", "_blank");

        if (!previewWindow) {
            alert("Please allow pop-ups to open files.");
            return;
        }

        previewWindow.document.title = "Opening file...";

        try {
            // Open file
            const response = await API.get(
                `/files/view/${id}`,
                {
                    responseType: "blob"
                }
            );

            const blob = new Blob(
                [response.data],
                {
                    type:
                        response.headers["content-type"] ||
                        "application/octet-stream"
                }
            );

            const url = window.URL.createObjectURL(blob);

            previewWindow.location.href = url;

            // Marking as viewed should not prevent the file from opening.
            API.get(`/files/mark-viewed/${id}`).catch((error) => {
                console.log("Mark viewed failed:", error);
            });

            setTimeout(() => {
                window.URL.revokeObjectURL(url);
            }, 60000);

        } catch (error) {

            console.log(
                "OPEN SEARCH FILE ERROR:",
                error
            );

            console.log(
                "SERVER ERROR:",
                error.response?.data
            );

            previewWindow.close();

            alert(
                error.response?.data?.message ||
                "Cannot open file"
            );
        }
    };

    // =====================================================
    // OPEN SEARCH RESULT
    // =====================================================

    const handleResultClick = (item) => {

        console.log(
            "SEARCH ITEM:",
            item
        );


        // Folder
        if (item.type === "FOLDER") {

            navigate(
                `/folder/${item.id}`
            );

            return;
        }


        // File
        openFile(item.id);

    };


    // =====================================================
    // NO RESULTS
    // =====================================================

    if (!results || results.length === 0) {

        return (

            <div className="search-results">

                <div className="no-search-results">

                    🔍 No results found

                </div>

            </div>

        );

    }


    // =====================================================
    // RESULTS
    // =====================================================

    return (

        <div className="search-results">

            <div className="search-results-title">

                🔍 Search Results

            </div>


            {results.map((item) => (

                <div

                    key={`${item.type}-${item.id}`}

                    className="search-item"

                    onClick={() =>
                        handleResultClick(item)
                    }

                >

                    {/* ICON */}

                    <span className="search-icon">

                        {item.type === "FOLDER"
                            ? "📁"
                            : "📄"}

                    </span>


                    {/* INFORMATION */}

                    <div className="search-item-info">

                        <h4>

                            {item.name}

                        </h4>


                        <p>

                            {item.type === "FOLDER"
                                ? "Folder"
                                : "File"}

                        </p>

                    </div>

                </div>

            ))}

        </div>

    );

}

export default SearchResults;