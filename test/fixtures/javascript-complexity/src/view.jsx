export function Badge(props) {
  return <span>{props.count > 0 ? props.count : "none"}</span>;
}
