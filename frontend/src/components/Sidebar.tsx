import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function Sidebar() {
  const { user } = useAuth();

  return (
    <nav className="sidebar">
      <div className="sidebar-logo">BE-GEMS</div>
      <div className="sidebar-org">{user?.organizationName}</div>
      <ul className="sidebar-links">
        <li>
          <NavLink to="/dashboard">Dashboard</NavLink>
        </li>
        <li>
          <NavLink to="/board-setup">Board Setup</NavLink>
        </li>
      </ul>
    </nav>
  );
}
