import { useNavigate } from "react-router-dom";
import { ROLE_LABELS } from "../constants/roles";
import { useAuth } from "../context/AuthContext";

export function TopBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleSignOut() {
    logout();
    navigate("/login");
  }

  if (!user) return null;

  return (
    <div className="topbar">
      <div>
        <strong>
          {user.firstName} {user.lastName}
        </strong>
        <span className="topbar-role"> &middot; {ROLE_LABELS[user.role]}</span>
      </div>
      <button type="button" className="secondary small" onClick={handleSignOut}>
        Sign out
      </button>
    </div>
  );
}
