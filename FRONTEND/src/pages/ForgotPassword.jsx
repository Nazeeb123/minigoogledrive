import { useState } from "react";
import { Link } from "react-router-dom";
import API from "../services/api";
import "./AuthRecovery.css";

export default function ForgotPassword() {
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const submit = async (event) => {
        event.preventDefault();
        setError("");
        if (!/^[A-Za-z0-9._%+-]+@gmail\.com$/.test(email.trim())) {
            setError("Please enter a valid @gmail.com address.");
            return;
        }
        try {
            setLoading(true);
            const response = await API.post("/forgot-password", { email: email.trim() });
            setMessage(response.data.message);
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Unable to request a reset link. Try again shortly.");
        } finally {
            setLoading(false);
        }
    };

    return <main className="recovery-page">
        <form className="recovery-card" onSubmit={submit}>
            <p className="eyebrow">Account recovery</p>
            <h1>Reset your password</h1>
            <p>Enter the Gmail address for your account and we’ll send a secure reset link.</p>
            <label htmlFor="recovery-email">Gmail address</label>
            <input id="recovery-email" type="email" autoComplete="email" placeholder="name@gmail.com" value={email} onChange={(e) => setEmail(e.target.value)} />
            {error && <p className="form-error" role="alert">{error}</p>}
            {message && <p className="form-success" role="status">{message}</p>}
            <button disabled={loading}>{loading ? "Sending link..." : "Send reset link"}</button>
            <Link className="back-link" to="/login">Back to sign in</Link>
        </form>
    </main>;
}
