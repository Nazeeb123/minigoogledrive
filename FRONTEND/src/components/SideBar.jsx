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

function Sidebar({
  collapsed,
  setCollapsed
}) {
  const navigate = useNavigate();

  const [sharedCount, setSharedCount] = useState(0);

  const [storage, setStorage] = useState({
    used: 0,
    limit: 2 * 1024 * 1024 * 1024,
    remaining: 2 * 1024 * 1024 * 1024,
    percentage: 0
  });
  // =========================
  // SIDEBAR OPEN / CLOSE
  // =========================




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




      {/* =========================
          MY DRIVE
      ========================= */}

      <div className="sidebar-header">
        <button
          className="sidebar-toggle"
          onClick={() => setCollapsed(prev => !prev)}
        >
          <FaBars />
        </button>

        {!collapsed && <h3>MY DRIVE</h3>}
      </div>

      <li onClick={() => navigate("/dashboard")}>
        <FaFolder />
        <span>My Drive</span>
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
        🧠

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
                width: `${Math.min(
                  storage.percentage ?? 0,
                  100
                )}%`
              }}
            ></div>

          </div>

          <p>
            {(storage.used / (1024 * 1024)).toFixed(1)} MB /{" "}
            {(storage.limit / (1024 * 1024 * 1024)).toFixed(1)} GB
          </p>

          <p>
            {storage.percentage?.toFixed(1) ?? "0.0"}% used
          </p>

          <p>
            {storage.remaining >= 1024 * 1024 * 1024
              ? `${(
                storage.remaining /
                (1024 * 1024 * 1024)
              ).toFixed(2)} GB remaining`
              : `${(
                storage.remaining /
                (1024 * 1024)
              ).toFixed(1)} MB remaining`}
          </p>

          {/* =========================
        STORAGE WARNING
    ========================= */}

          {storage.percentage >= 100 && (

            <p className="storage-warning storage-full">
              Storage is full. Remove files or get Premium.
            </p>

          )}

          {storage.percentage >= 80 &&
            storage.percentage < 100 && (

              <p className="storage-warning">
                Storage is getting full. Remove files or get Premium.
              </p>

            )}

        </div>

      )}
    </div>

  );
}

export default Sidebar;