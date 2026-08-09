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

        try {

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
                        response.headers["content-type"]
                        || "application/octet-stream"
                }
            );


            const url =
                window.URL.createObjectURL(blob);


            window.open(
                url,
                "_blank"
            );


        } catch (error) {

            console.log(
                "OPEN SEARCH FILE ERROR:",
                error
            );

            alert("Cannot open file");

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