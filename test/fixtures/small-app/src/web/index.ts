import { fetchPrices } from "./api.generated";

export async function render(target: HTMLElement): Promise<void> {
  const prices = await fetchPrices();

  target.textContent = prices.join(", ");
}
