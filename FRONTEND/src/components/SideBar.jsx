import "./SideBar.css";
import {
  FaFolder,
  FaStar,
  FaTrash,
  FaUsers,
  FaRobot,
  FaBars
} from "react-icons/fa";
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

  // =========================
  // SIDEBAR OPEN / CLOSE
  // =========================

  const [collapsed, setCollapsed] = useState(false);


  // =========================
  // LOAD STORAGE
  // =========================

  const loadStorage = async () => {
    try {

      const response = await API.get("/files/storage");

      console.log("STORAGE RESPONSE:", response.data);

      setStorage(response.data);

    } catch (error) {

      console.log("STORAGE ERROR:", error);

    }
  };


  useEffect(() => {

    loadStorage();

  }, []);


  // =========================
  // LOAD SHARED COUNT
  // =========================

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

    <div
      className={`sidebar ${collapsed ? "sidebar-collapsed" : ""
        }`}
    >

      {/* =========================
          SIDEBAR HEADER
      ========================= */}

      <div className="sidebar-header">

        <button
          className="sidebar-toggle"
          onClick={() =>
            setCollapsed(prev => !prev)
          }
          title={
            collapsed
              ? "Expand sidebar"
              : "Collapse sidebar"
          }
        >
          <FaBars />
        </button>


        {!collapsed && (
          <h3>MY DRIVE</h3>
        )}

      </div>


      {/* =========================
          MY DRIVE
      ========================= */}

      <li
        onClick={() => navigate("/dashboard")}
        title="My Drive"
      >
        <FaFolder />

        {!collapsed && (
          <span>My Drive</span>
        )}
      </li>


      {/* =========================
          STARRED
      ========================= */}

      <li
        onClick={() => navigate("/starred")}
        title="Starred"
      >
        <FaStar />

        {!collapsed && (
          <span>Starred</span>
        )}
      </li>


      {/* =========================
          TRASH
      ========================= */}

      <li
        onClick={() => navigate("/trash")}
        title="Trash"
      >
        <FaTrash />

        {!collapsed && (
          <span>Trash</span>
        )}
      </li>


      {/* =========================
          SHARED
      ========================= */}

      <li
        onClick={() => navigate("/shared")}
        title="Shared"
      >
        <FaUsers />

        {!collapsed && (
          <span>Shared</span>
        )}

        {sharedCount > 0 && (
          <span className="shared-badge">
            {sharedCount}
          </span>
        )}

      </li>


      {/* =========================
          AI ASSISTANT
      ========================= */}

      <li
        onClick={() => navigate("/ai")}
        title="AI Assistant"
      >
        <FaRobot />

        {!collapsed && (
          <span>AI Assistant</span>
        )}

      </li>


      {/* =========================
          STORAGE BOX
      ========================= */}

      {!collapsed && (

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

      )}

    </div>

  );
}

export default Sidebar;