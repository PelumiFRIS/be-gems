interface BadgeProps {
  value: string;
  label?: string;
}

export function Badge({ value, label }: BadgeProps) {
  return <span className={`badge badge-${value.toLowerCase()}`}>{label ?? value.replace(/_/g, " ")}</span>;
}
