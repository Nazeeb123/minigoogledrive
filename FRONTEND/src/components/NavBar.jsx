import { useEffect, useRef, useState } from "react";
import axios from "axios";
import { FaSearch, FaBell } from "react-icons/fa";
import { useNavigate } from "react-router-dom";
import "./NavBar.css";

function Navbar({
  search,
  setSearch,
  onSearchEnter
}) {

  const email = localStorage.getItem("email");
  const token = localStorage.getItem("token");
  const navigate = useNavigate();

  const [loggingOut, setLoggingOut] = useState(false);

  const [showNotifications, setShowNotifications] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const notificationRef = useRef(null);

  // =====================================================
  // LOAD NOTIFICATIONS
  // =====================================================

  useEffect(() => {

    if (!token) return;

    fetchNotifications();
    fetchUnreadCount();

  }, [token]);


  // =====================================================
  // FETCH NOTIFICATIONS
  // =====================================================

  const fetchNotifications = async () => {

    try {

      const response = await axios.get(
        "http://localhost:8080/notifications",
        {
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      );

      setNotifications(response.data);

    } catch (error) {

      console.error(
        "Failed to fetch notifications:",
        error
      );

    }

  };


  // =====================================================
  // DELETE NOTIFICATION
  // =====================================================

  const deleteNotification = async (id) => {

    try {

      await axios.delete(
        `http://localhost:8080/notifications/${id}`,
        {
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      );

      const deletedNotification =
        notifications.find(
          notification =>
            notification.id === id
        );

      setNotifications(prev =>
        prev.filter(
          notification =>
            notification.id !== id
        )
      );

      if (
        deletedNotification &&
        !deletedNotification.read
      ) {

        setUnreadCount(prev =>
          Math.max(0, prev - 1)
        );

      }

    } catch (error) {

      console.error(
        "Failed to delete notification:",
        error
      );

    }

  };


  // =====================================================
  // FETCH UNREAD COUNT
  // =====================================================

  const fetchUnreadCount = async () => {

    try {

      const response = await axios.get(
        "http://localhost:8080/notifications/unread-count",
        {
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      );

      setUnreadCount(response.data);

    } catch (error) {

      console.error(
        "Failed to fetch unread count:",
        error
      );

    }

  };


  // =====================================================
  // MARK NOTIFICATION AS READ
  // =====================================================

  const markNotificationRead = async (id) => {

    try {

      await axios.put(
        `http://localhost:8080/notifications/${id}/read`,
        {},
        {
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      );

      setNotifications(prev =>
        prev.map(notification =>
          notification.id === id
            ? {
                ...notification,
                read: true
              }
            : notification
        )
      );

      setUnreadCount(prev =>
        Math.max(0, prev - 1)
      );

    } catch (error) {

      console.error(
        "Failed to mark notification as read:",
        error
      );

    }

  };


  // =====================================================
  // CLOSE NOTIFICATION DROPDOWN
  // =====================================================

  useEffect(() => {

    const handleClickOutside = (event) => {

      if (
        notificationRef.current &&
        !notificationRef.current.contains(
          event.target
        )
      ) {

        setShowNotifications(false);

      }

    };

    document.addEventListener(
      "mousedown",
      handleClickOutside
    );

    return () => {

      document.removeEventListener(
        "mousedown",
        handleClickOutside
      );

    };

  }, []);


  // =====================================================
  // UI
  // =====================================================

  return (

    <div className="navbar">

      {/* LOGOUT OVERLAY */}

      {loggingOut && (

        <div className="logout-overlay">

          <div className="logout-box">

            <div className="logout-spinner"></div>

            <h2>
              Logging out...
            </h2>

            <p>
              Please wait
            </p>

          </div>

        </div>

      )}


      {/* TITLE */}

      <h2>
        Mini Google Drive
      </h2>


      {/* =========================
          SEARCH
      ========================= */}

      <div className="search-container">

        <FaSearch className="search-icon" />

        <input
          className="search-box"
          type="text"
          placeholder="Search your files..."
          value={search}
          onChange={(e) =>
            setSearch(e.target.value)
          }
          onKeyDown={onSearchEnter}
        />

      </div>


      {/* =========================
          RIGHT SIDE
      ========================= */}

      <div className="navbar-right">

        <span>
          {email}
        </span>


        {/* =========================
            NOTIFICATIONS
        ========================= */}

        <div
          className="notification-container"
          ref={notificationRef}
        >

          <button
            className="notification-button"
            onClick={() => {

              setShowNotifications(
                prev => !prev
              );

              if (!showNotifications) {

                fetchNotifications();
                fetchUnreadCount();

              }

            }}
          >

            <FaBell />

            {unreadCount > 0 && (

              <span className="notification-badge">
                {unreadCount}
              </span>

            )}

          </button>


          {showNotifications && (

            <div className="notification-dropdown">

              <h3>
                Notifications
              </h3>


              {notifications.length === 0 ? (

                <p className="no-notifications">
                  No notifications
                </p>

              ) : (

                notifications.map(
                  (notification) => (

                    <div
                      key={notification.id}
                      className={`notification-item ${
                        notification.read
                          ? "read"
                          : "unread"
                      }`}
                    >

                      {/* NOTIFICATION CONTENT */}

                      <div
                        className="notification-main"
                        onClick={() => {

                          if (
                            !notification.read
                          ) {

                            markNotificationRead(
                              notification.id
                            );

                          }

                        }}
                      >

                        <div className="notification-icon">
                          🔔
                        </div>

                        <div className="notification-content">

                          <p>
                            {notification.message}
                          </p>

                          <small>
                            {new Date(
                              notification.createdAt
                            ).toLocaleString()}
                          </small>

                        </div>

                      </div>


                      {/* DELETE */}

                      <button
                        className="notification-delete-btn"
                        onClick={(e) => {

                          e.stopPropagation();

                          deleteNotification(
                            notification.id
                          );

                        }}
                        title="Delete notification"
                      >
                        🗑
                      </button>

                    </div>

                  )
                )

              )}

            </div>

          )}

        </div>


        {/* =========================
            LOGOUT
        ========================= */}

        <button
          onClick={() => {

            setLoggingOut(true);

            localStorage.clear();

            setTimeout(() => {

              navigate("/login");

            }, 2000);

          }}
        >
          Logout
        </button>

      </div>

    </div>

  );

}

export default Navbar;