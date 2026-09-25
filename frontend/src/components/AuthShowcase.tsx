import frisLogoWhite from "../assets/fris-logo-white.png";
import {
  ActionsIcon,
  BoardIcon,
  CommitteeIcon,
  EvaluationsIcon,
  FindingsIcon,
  RecommendationsIcon,
  ScoringIcon,
} from "./icons";

const MODULES = [
  { icon: BoardIcon, label: "Board Setup", bg: "#e3c374" },
  { icon: EvaluationsIcon, label: "Evaluations", bg: "#7fb1e0" },
  { icon: CommitteeIcon, label: "Directors", bg: "#8fd0ae" },
  { icon: ScoringIcon, label: "BGEI Scoring", bg: "#e59a8f" },
  { icon: FindingsIcon, label: "Findings", bg: "#e3a3d6" },
  { icon: RecommendationsIcon, label: "Recommendations", bg: "#9fc2f2" },
  { icon: ActionsIcon, label: "Action Register", bg: "#8fd0ae" },
] as const;

export function AuthShowcase() {
  return (
    <div className="auth-showcase">
      <div className="auth-showcase-brand">
        <img src={frisLogoWhite} alt="First Registrars" />
        <span>BE-GEMS</span>
      </div>

      <div>
        <p className="auth-showcase-eyebrow">Board Evaluation &amp; Governance Effectiveness</p>
        <h1>
          Governance that's <em>evidenced</em>, not assumed.
        </h1>
      </div>

      <p className="auth-showcase-lede">
        One system for board and director evaluations, NCCG-aligned scoring, and the findings,
        recommendations and corrective actions that turn results into real governance improvement.
      </p>

      <div className="module-grid">
        {MODULES.map(({ icon: Icon, label, bg }) => (
          <div className="module-tile" key={label}>
            <span className="module-tile-icon" style={{ background: bg }}>
              <Icon width={17} height={17} />
            </span>
            <span>{label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
