import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import API from "../services/api";
import SearchResults from "../components/SearchResults";
import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";


function SearchPage(){

    const [params] = useSearchParams();

    const query = params.get("query");


    const [results,setResults] = useState([]);


    useEffect(()=>{

        loadResults();

    },[]);



    const loadResults = async()=>{

        try{

            const response =
            await API.get(
                `/files/search?query=${query}`
            );


            setResults(response.data);


        }
        catch(error){

            console.log(error);

        }

    };



    return(

        <div className="dashboard-container">


            <Sidebar />


            <div className="dashboard">


                <Navbar />


                <h2>
                    Search Results for "{query}"
                </h2>


                <SearchResults
                    results={results}
                />


            </div>


        </div>

    );

}


export default SearchPage;