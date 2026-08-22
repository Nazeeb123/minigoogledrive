import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import API from "../services/api";
import SearchResults from "../components/SearchResults";
import Sidebar from "../components/SideBar";
import Navbar from "../components/NavBar";

function SearchPage() {

    const [params] = useSearchParams();

    const query = params.get("query");

    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(true);


    useEffect(() => {

        if (query) {
            loadResults();
        } else {
            setResults([]);
            setLoading(false);
        }

    }, [query]);


    const loadResults = async () => {

        try {

            setLoading(true);

            const response = await API.get(
                `/files/semantic-search?query=${encodeURIComponent(query)}`
            );

            setResults(response.data);

        } catch (error) {

            console.log("Search error:", error);

            setResults([]);

        } finally {

            setLoading(false);

        }

    };


    return (

        <div className="dashboard-container">

            <Sidebar />

            <div className="dashboard">

                <Navbar />

                <h2>
                    Search Results for "{query}"
                </h2>


                {loading ? (

                    <p>Searching...</p>

                ) : results.length === 0 ? (

                    <p>No matching files found.</p>

                ) : (

                    <SearchResults
                        results={results}
                    />

                )}

            </div>

        </div>

    );
}

export default SearchPage;