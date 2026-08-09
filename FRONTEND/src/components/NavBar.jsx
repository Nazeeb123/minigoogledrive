import { useEffect, useRef, useState } from "react";
import axios from "axios";
import { FaSearch, FaBell } from "react-icons/fa";
import "./Navbar.css";

function Navbar({
  search,
  setSearch,
  onSearchEnter
}) {

  const email = localStorage.getItem("email");
  const token = localStorage.getItem("token");

  const [showNotifications, setShowNotifications] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const notificationRef = useRef(null);

  useEffect(() => {

    fetchNotifications();
    fetchUnreadCount();

  }, []);

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


  useEffect(() => {

    const handleClickOutside = (event) => {

      if (
        notificationRef.current &&
        !notificationRef.current.contains(event.target)
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


  return (

    <div className="navbar">

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
                  notification => (

                    <div
                      key={notification.id}
                      className={
                        `notification-item ${notification.read
                          ? "read"
                          : "unread"
                        }`
                      }
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
                          {
                            notification.message
                          }
                        </p>

                        <small>
                          {new Date(
                            notification.createdAt
                          ).toLocaleString()}
                        </small>

                      </div>

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

            localStorage.clear();
            window.location.reload();

          }}
        >
          Logout
        </button>

      </div>

    </div>

  );

}

export default Navbar;