export function summary(rows: number[]): string {
  const total = rows.reduce((a, b) => a + b, 0);
  return `${rows.length} rows, ${total} total`;
}
