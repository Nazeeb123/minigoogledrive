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
    const [error, setError] = useState("");

    const passwordRules = {
        length: password.length >= 8,
        upper: /[A-Z]/.test(password),
        lower: /[a-z]/.test(password),
        number: /\d/.test(password)
    };
    const isValidPassword = Object.values(passwordRules).every(Boolean);
    const isGmail = /^[A-Za-z0-9._%+-]+@gmail\.com$/.test(email.trim());

    const navigate = useNavigate();

    const handleRegister = async () => {

        setError("");
        if (!username || !email || !password) {
            setError("Please complete every field.");
            return;
        }
        if (!isGmail) {
            setError("Please enter a valid @gmail.com address.");
            return;
        }
        if (!isValidPassword) {
            setError("Please meet all password requirements.");
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

            setError(error.response?.data?.message || "Unable to create your account. Please try again.");

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
                    <small className="field-hint">A Gmail address is required (name@gmail.com).</small>

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
                    <ul className="password-rules" aria-live="polite">
                        <li className={passwordRules.length ? "valid" : ""}>At least 8 characters</li>
                        <li className={passwordRules.upper ? "valid" : ""}>One uppercase letter</li>
                        <li className={passwordRules.lower ? "valid" : ""}>One lowercase letter</li>
                        <li className={passwordRules.number ? "valid" : ""}>One number</li>
                    </ul>

                </div>

                {error && <p className="form-error" role="alert">{error}</p>}


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
