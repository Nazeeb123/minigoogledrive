import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import API from "../services/api";
import "./AuthRecovery.css";

export default function ResetPassword() {
    const [params] = useSearchParams();
    const navigate = useNavigate();
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const validPassword = password.length >= 8 && /[A-Z]/.test(password) && /[a-z]/.test(password) && /\d/.test(password);

    const submit = async (event) => {
        event.preventDefault();
        setError("");
        if (!params.get("token")) return setError("This reset link is missing or invalid.");
        if (!validPassword) return setError("Use 8+ characters with uppercase, lowercase, and a number.");
        if (password !== confirmPassword) return setError("Passwords do not match.");
        try {
            setLoading(true);
            await API.post("/reset-password", { token: params.get("token"), password });
            navigate("/login", { state: { passwordReset: true } });
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Unable to reset password. Request a new link.");
        } finally { setLoading(false); }
    };

    return <main className="recovery-page"><form className="recovery-card" onSubmit={submit}>
        <p className="eyebrow">Choose a new password</p>
        <h1>Set a new password</h1>
        <p>Use at least 8 characters, including uppercase, lowercase, and a number.</p>
        <label htmlFor="new-password">New password</label>
        <input id="new-password" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />
        <label htmlFor="confirm-password">Confirm password</label><input id="confirm-password" type="password" autoComplete="new-password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
        {error && <p className="form-error" role="alert">{error}</p>}
        <button disabled={loading}>{loading ? "Updating..." : "Update password"}</button><Link className="back-link" to="/login">Back to sign in</Link>
    </form></main>;
}
