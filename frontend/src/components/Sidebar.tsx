import { NavLink } from "react-router-dom";
import { ROLE_LABELS } from "../constants/roles";
import { useAuth } from "../context/AuthContext";
import frisLogoWhite from "../assets/fris-logo-white.png";
import { ActionsIcon, BoardIcon, DashboardIcon, EvaluationsIcon, MyEvaluationsIcon } from "./icons";
import { initials } from "../utils/initials";

export function Sidebar() {
  const { user } = useAuth();

  return (
    <nav className="sidebar">
      <div className="sidebar-brand">
        <img src={frisLogoWhite} alt="First Registrars" className="sidebar-brand-logo" />
        <div className="sidebar-product-name">BE-GEMS</div>
        <div className="sidebar-product-tagline">Governance Effectiveness</div>
      </div>
      <div className="sidebar-org">{user?.organizationName}</div>
      <ul className="sidebar-links">
        <li>
          <NavLink to="/dashboard">
            <DashboardIcon /> Dashboard
          </NavLink>
        </li>
        <li>
          <NavLink to="/board-setup">
            <BoardIcon /> Board Setup
          </NavLink>
        </li>
        <li>
          <NavLink to="/evaluations">
            <EvaluationsIcon /> Evaluations
          </NavLink>
        </li>
        <li>
          <NavLink to="/my-evaluations">
            <MyEvaluationsIcon /> My Evaluations
          </NavLink>
        </li>
        <li>
          <NavLink to="/actions">
            <ActionsIcon /> Action Register
          </NavLink>
        </li>
      </ul>
      {user && (
        <div className="sidebar-user">
          <span className="avatar">{initials(user.firstName, user.lastName)}</span>
          <div className="sidebar-user-info">
            <div className="sidebar-user-name">
              {user.firstName} {user.lastName}
            </div>
            <div className="sidebar-user-role">{ROLE_LABELS[user.role]}</div>
          </div>
        </div>
      )}
      <div className="sidebar-footer">First Registrars &amp; Investor Services</div>
    </nav>
  );
}
