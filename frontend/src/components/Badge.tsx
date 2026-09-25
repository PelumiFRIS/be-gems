interface BadgeProps {
  value: string;
}

export function Badge({ value }: BadgeProps) {
  return <span className={`badge badge-${value.toLowerCase()}`}>{value.replace(/_/g, " ")}</span>;
}
