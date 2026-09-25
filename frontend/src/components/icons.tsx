import type { SVGProps } from "react";

type IconProps = SVGProps<SVGSVGElement>;

function base(props: IconProps) {
  return {
    width: 20,
    height: 20,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.75,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    ...props,
  };
}

export function DashboardIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect x="3.5" y="3.5" width="7.5" height="7.5" rx="1.5" />
      <rect x="13" y="3.5" width="7.5" height="4.5" rx="1.5" />
      <rect x="13" y="10.5" width="7.5" height="10" rx="1.5" />
      <rect x="3.5" y="13.5" width="7.5" height="7" rx="1.5" />
    </svg>
  );
}

export function BoardIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect x="3.5" y="4.5" width="17" height="14" rx="2" />
      <path d="M3.5 9.5h17" />
      <circle cx="7.5" cy="7" r="0.9" fill="currentColor" stroke="none" />
      <circle cx="10.5" cy="7" r="0.9" fill="currentColor" stroke="none" />
    </svg>
  );
}

export function EvaluationsIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M8 3.5h8a1.5 1.5 0 0 1 1.5 1.5v14a1.5 1.5 0 0 1-1.5 1.5H8A1.5 1.5 0 0 1 6.5 19V5A1.5 1.5 0 0 1 8 3.5Z" />
      <path d="M9.5 3.5V6h5V3.5" />
      <path d="m9.5 12.5 1.8 1.8L15 10.5" />
    </svg>
  );
}

export function MyEvaluationsIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="8" r="3.2" />
      <path d="M5.5 20c0-3.6 2.9-6.2 6.5-6.2s6.5 2.6 6.5 6.2" />
    </svg>
  );
}

export function ActionsIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="8.5" />
      <path d="m8.5 12.3 2.4 2.4 4.6-5.2" />
    </svg>
  );
}

export function FindingsIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 3.5 21 19H3L12 3.5Z" />
      <path d="M12 10v3.6" />
      <circle cx="12" cy="16.3" r="0.9" fill="currentColor" stroke="none" />
    </svg>
  );
}

export function RecommendationsIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 3.5a5.5 5.5 0 0 0-3 10.1c.6.4 1 .9 1 1.6v.8h4v-.8c0-.7.4-1.2 1-1.6A5.5 5.5 0 0 0 12 3.5Z" />
      <path d="M10 19h4" />
      <path d="M10.5 21h3" />
    </svg>
  );
}

export function ScoringIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M4 20V10.5" />
      <path d="M10 20V4" />
      <path d="M16 20v-7" />
      <path d="M20 20v-3.5" />
    </svg>
  );
}

export function CommitteeIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="8.5" cy="8" r="2.6" />
      <circle cx="16.5" cy="8" r="2.6" />
      <path d="M3.5 19c0-2.9 2.3-5 5-5s5 2.1 5 5" />
      <path d="M12.5 14.3c.7-.2 1.4-.3 2-.3 2.7 0 5 2.1 5 5" />
    </svg>
  );
}

export function LogoutIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M9 4.5H6a1.5 1.5 0 0 0-1.5 1.5v12A1.5 1.5 0 0 0 6 19.5h3" />
      <path d="M14.5 16 19 12l-4.5-4" />
      <path d="M19 12H9" />
    </svg>
  );
}

export function OverdueIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="13" r="8" />
      <path d="M12 9.3V13l2.4 1.8" />
      <path d="M9 3.5h6" />
    </svg>
  );
}

export function ChevronRightIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m9 5 7 7-7 7" />
    </svg>
  );
}

export function DirectorsIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="9" cy="8" r="3" />
      <path d="M3.5 20c0-3.6 2.5-6.2 5.5-6.2s5.5 2.6 5.5 6.2" />
      <path d="M16 5.2c1.4.4 2.5 1.7 2.5 3.3 0 1.6-1.1 2.9-2.5 3.3" />
      <path d="M16.5 13.8c2.2.6 4 2.7 4 5.2" />
    </svg>
  );
}
