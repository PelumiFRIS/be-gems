import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import frisLogoWhite from "../assets/fris-logo-white.png";

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
          <NavLink to="/dashboard">Dashboard</NavLink>
        </li>
        <li>
          <NavLink to="/board-setup">Board Setup</NavLink>
        </li>
        <li>
          <NavLink to="/evaluations">Evaluations</NavLink>
        </li>
        <li>
          <NavLink to="/my-evaluations">My Evaluations</NavLink>
        </li>
        <li>
          <NavLink to="/actions">Action Register</NavLink>
        </li>
      </ul>
      <div className="sidebar-footer">First Registrars &amp; Investor Services</div>
    </nav>
  );
}
