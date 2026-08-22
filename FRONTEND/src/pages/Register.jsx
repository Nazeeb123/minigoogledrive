import { useState } from "react";
import API from "../services/api";
import { useNavigate, Link } from "react-router-dom";
import "./Register.css";
import logo from "../assets/logo.png";

function Register() {

    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const [success, setSuccess] = useState(false);
    const [loading, setLoading] = useState(false);

    const navigate = useNavigate();

    const handleRegister = async () => {

        if (!username || !email || !password) {
            return;
        }

        try {

            setLoading(true);

            await API.post("/register", {
                username,
                email,
                password
            });

            setSuccess(true);

            setUsername("");
            setEmail("");
            setPassword("");

            setTimeout(() => {
                navigate("/login");
            }, 1800);

        } catch (error) {

            console.log("REGISTER ERROR:", error);

        } finally {

            setLoading(false);

        }

    };


    return (

        <div className="register-page">

            <div className="register-card">

                {/* LOGO */}

                <div className="register-logo-circle">

                    <img
                        src={logo}
                        alt="Mini Google Drive"
                    />

                </div>


                <h1>
                    Create Account
                </h1>

                <p className="register-subtitle">
                    Create your Mini Google Drive account
                </p>


                {/* USERNAME */}

                <div className="register-input-group">

                    <label>
                        Username
                    </label>

                    <input
                        type="text"
                        placeholder="Enter your username"
                        value={username}
                        onChange={(e) =>
                            setUsername(e.target.value)
                        }
                    />

                </div>


                {/* EMAIL */}

                <div className="register-input-group">

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

                <div className="register-input-group">

                    <label>
                        Password
                    </label>

                    <input
                        type="password"
                        placeholder="Create a password"
                        value={password}
                        onChange={(e) =>
                            setPassword(e.target.value)
                        }
                    />

                </div>


                {/* REGISTER BUTTON */}

                <button
                    className="register-button"
                    onClick={handleRegister}
                    disabled={loading}
                >

                    {loading
                        ? "Creating account..."
                        : "Create Account"
                    }

                </button>


                {/* LOGIN LINK */}

                <p className="login-link">

                    Already have an account?

                    {" "}

                    <Link to="/login">
                        Login
                    </Link>

                </p>

            </div>


            {/* SUCCESS POPUP */}

            {success && (

                <div className="success-overlay">

                    <div className="success-popup">

                        <div className="success-icon">
                            ✓
                        </div>

                        <h2>
                            Registration Successful
                        </h2>

                        <p>
                            Your account has been created successfully.
                        </p>

                        <span>
                            Redirecting to login...
                        </span>

                    </div>

                </div>

            )}

        </div>

    );
}

export default Register;