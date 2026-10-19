export function Badge(props: { count: number }) {
  return <span>{props.count > 0 ? props.count : "none"}</span>;
}
