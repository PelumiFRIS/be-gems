import { useNavigate } from "react-router-dom";
import { ROLE_LABELS } from "../constants/roles";
import { useAuth } from "../context/AuthContext";
import { LogoutIcon } from "./icons";
import { initials } from "../utils/initials";

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
      <div className="topbar-identity">
        <span className="avatar">{initials(user.firstName, user.lastName)}</span>
        <div>
          <div className="topbar-name">
            {user.firstName} {user.lastName}
          </div>
          <div className="topbar-role">{ROLE_LABELS[user.role]}</div>
        </div>
      </div>
      <button type="button" className="secondary small" onClick={handleSignOut}>
        <LogoutIcon width={15} height={15} /> Sign out
      </button>
    </div>
  );
}
