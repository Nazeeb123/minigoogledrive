
import { useState, useEffect } from "react";
import API from "../services/api";
import { useNavigate, Link } from "react-router-dom";
import "./Login.css";
import logo from "../assets/logo.png";

function Login() {

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    // ==============================
    // POPUP
    // ==============================

    const [popup, setPopup] = useState({
        show: false,
        message: "",
        type: ""
    });

    const showPopup = (message, type = "success") => {

        setPopup({
            show: true,
            message,
            type
        });

        setTimeout(() => {

            setPopup({
                show: false,
                message: "",
                type: ""
            });

        }, 3000);

    };


    const navigate = useNavigate();


    // ==============================
    // GOOGLE LOGIN
    // ==============================

    useEffect(() => {

        const initializeGoogle = () => {

            if (!window.google) {
                console.log("Google Identity Services not loaded yet");
                return;
            }

            window.google.accounts.id.initialize({

                client_id:
                    "97919262881-qf278cjpj709lodidkp7q1e5jea5ctb4.apps.googleusercontent.com",

                callback: handleGoogleLogin

            });

            window.google.accounts.id.renderButton(

                document.getElementById("google-login-button"),

                {
                    theme: "outline",
                    size: "large",
                    width: 350,
                    text: "signin_with",
                    shape: "rectangular"
                }

            );

        };


        const timer = setTimeout(
            initializeGoogle,
            500
        );


        return () => clearTimeout(timer);

    }, []);


    // ==============================
    // GOOGLE LOGIN
    // ==============================

    const handleGoogleLogin = async (response) => {

        console.log(
            "GOOGLE RESPONSE:",
            response
        );

        try {

            const result = await API.post(
                "/google-login",
                {
                    credential:
                        response.credential
                }
            );


            console.log(
                "GOOGLE LOGIN RESPONSE:",
                result.data
            );


            localStorage.setItem(
                "token",
                result.data
            );


            showPopup(
                "Google login successful",
                "success"
            );


            setTimeout(() => {

                navigate("/dashboard");

            }, 800);


        } catch (error) {

            console.log(
                "GOOGLE LOGIN ERROR:",
                error
            );


            showPopup(
                "Google login failed",
                "error"
            );

        }

    };


    // ==============================
    // NORMAL LOGIN
    // ==============================

    const handleLogin = async () => {

        if (!email || !password) {

            showPopup(
                "Please enter email and password",
                "error"
            );

            return;

        }


        console.log(
            "Email sent:",
            email
        );

        console.log(
            "Password sent:",
            password
        );


        try {

            const response = await API.post(
                "/login",
                {
                    email,
                    password
                }
            );


            console.log(
                "LOGIN RESPONSE:",
                response.data
            );


            localStorage.setItem(
                "token",
                response.data
            );


            localStorage.setItem(
                "email",
                email
            );


            showPopup(
                "Login successful",
                "success"
            );


            setTimeout(() => {

                navigate("/dashboard");

            }, 800);


        } catch (error) {

            console.log(error);


            showPopup(
                "Login failed",
                "error"
            );

        }

    };


    return (

        <div className="login-page">


            {/* ==============================
                POPUP
            ============================== */}

            {popup.show && (

                <div
                    className={`login-popup ${popup.type}`}
                >

                    <span className="popup-icon">

                        {popup.type === "success"
                            ? "✓"
                            : "✕"
                        }

                    </span>


                    <span>
                        {popup.message}
                    </span>

                </div>

            )}


            {/* ==============================
                LEFT BRANDING
            ============================== */}

            <div className="login-brand">

                <div className="brand-content">

                    <img
                        src={logo}
                        alt="Mini Google Drive"
                        className="login-logo"
                    />


                    <h1>
                        Mini Google Drive
                    </h1>


                    <p className="brand-tagline">
                        Secure. Simple. Powerful.
                    </p>


                    <p className="brand-description">

                        Store, manage and share your files
                        securely from anywhere.

                    </p>


                    <div className="security-badge">

                        🛡️ Secure Cloud Storage

                    </div>

                </div>

            </div>


            {/* ==============================
                RIGHT LOGIN
            ============================== */}

            <div className="login-section">

                <div className="login-card">


                    <div className="mobile-logo">

                        <img
                            src={logo}
                            alt="Mini Google Drive"
                        />

                    </div>


                    <h2>
                        Welcome back
                    </h2>


                    <p className="login-subtitle">

                        Sign in to continue to your drive

                    </p>


                    {/* EMAIL */}

                    <div className="input-group">

                        <label>
                            Email
                        </label>


                        <input
                            type="email"
                            placeholder="Enter your email"
                            value={email}
                            onChange={(e) =>
                                setEmail(e.target.value)
                            }
                        />

                    </div>


                    {/* PASSWORD */}

                    <div className="input-group">

                        <label>
                            Password
                        </label>


                        <input
                            type="password"
                            placeholder="Enter your password"
                            value={password}
                            onChange={(e) =>
                                setPassword(e.target.value)
                            }
                            onKeyDown={(e) => {

                                if (e.key === "Enter") {

                                    handleLogin();

                                }

                            }}
                        />

                    </div>


                    {/* NORMAL LOGIN */}

                    <button
                        className="login-button"
                        onClick={handleLogin}
                    >

                        Sign In

                    </button>


                    {/* DIVIDER */}

                    <div className="login-divider">

                        <span>
                            OR
                        </span>

                    </div>


                    {/* GOOGLE LOGIN */}

                    <div
                        id="google-login-button"
                        className="google-login-button"
                    >
                    </div>


                    {/* REGISTER */}

                    <div className="signup-section">

                        <span>
                            New to Mini Google Drive?
                        </span>


                        <Link
                            to="/register"
                            className="signup-button"
                        >

                            Create an account

                        </Link>

                    </div>


                    <div className="login-footer">

                        🔒 Your files are protected

                    </div>


                </div>

            </div>

        </div>

    );

}

export default Login;

