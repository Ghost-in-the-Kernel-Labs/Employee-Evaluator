export function sumOfPrimes(max: number): number {
  let total = 0;
  OUT: for (let i = 1; i <= max; ++i) {
    for (let j = 2; j < i; ++j) {
      if (i % j === 0) {
        continue OUT;
      }
    }
    total += i;
  }
  return total;
}

export function getWords(n: number): string {
  switch (n) {
    case 1:
      return "one";
    case 2:
      return "a couple";
    case 3:
      return "a few";
    default:
      return "lots";
  }
}

function grade(score: number): string {
  if (score > 90) {
    return "A";
  } else if (score > 80) {
    return "B";
  } else {
    return "C";
  }
}

const logic = (a: boolean, b: boolean, c: boolean, d: boolean): boolean => a && b && c || d && !(a || b);

export class Store {
  items: number[] = [];

  nested(): number {
    let n = 0;
    for (const x of this.items) {
      if (x > 0) {
        n += x > 10 ? 2 : 1;
      }
    }
    return n;
  }

  load = async (): Promise<void> => {
    try {
      await fetch("/x");
    } catch (e) {
      if (e === null) {
        return;
      }
    }
  };
}

function later(xs: number[]): number[] {
  return xs.map((x) => {
    if (x > 0) {
      return x;
    }
    return 0;
  });
}

function fact(n: number): number {
  return n <= 1 ? 1 : n * fact(n - 1);
}

function countdown(n: number): void {
  do {
    n--;
  } while (n > 0);
}
