import "./Sidebar.css";
import { FaFolder, FaStar, FaTrash, FaUsers, FaRobot } from "react-icons/fa";
import { useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import API from "../services/api";

function Sidebar() {
  const navigate = useNavigate();
  const [sharedCount, setSharedCount] = useState(0);
  const [storage, setStorage] = useState({
    used: 0,
    limit: 1
  });


  const loadStorage = async () => {

    try {

      const response = await API.get("/files/storage");
      console.log("STORAGE RESPONSE:", response.data);
      setStorage(response.data);

    }
    catch (error) {

      console.log("STORAGE ERROR:", error);

    }

  };


  useEffect(() => {

    loadStorage();

  }, []);
  const loadSharedCount = async () => {
    try {
      const response = await API.get("/files/shared/count");
      setSharedCount(response.data);
    } catch (error) {
      console.log(error);
    }
  };

  useEffect(() => {
    loadSharedCount();
    const interval = setInterval(() => {
      loadSharedCount();
    }, 3000);

    return () => clearInterval(interval);
  }, []);

  return (

    <div className="sidebar">

      <h3>MY DRIVE</h3>


      <li onClick={() => navigate("/dashboard")}>
        <FaFolder /> My Drive
      </li>


      <li onClick={() => navigate("/starred")}>
        <FaStar /> Starred
      </li>


      <li onClick={() => navigate("/trash")}>
        <FaTrash /> Trash
      </li>


      <li onClick={() => navigate("/shared")}>
        <FaUsers /> Shared

        {sharedCount > 0 && (
          <span className="shared-badge">
            {sharedCount}
          </span>
        )}

      </li>
      <li onClick={() => navigate("/ai")}>
        🧠 AI Assistant
      </li>



      {/* STORAGE BOX */}

      <div className="storage-box">

        <h2>Storage</h2>

        <div className="storage-bar">

          <div
            className="storage-used"
            style={{
              width:
                `${Math.min(
                  (storage.used / storage.limit) * 100,
                  100
                )}%`
            }}
          ></div>

        </div>


        <p>
          {(storage.used / (1024 * 1024)).toFixed(1)} MB /
          {(storage.limit / (1024 * 1024 * 1024)).toFixed(1)} GB
        </p>


      </div>

    </div>

  );
}

export default Sidebar;
