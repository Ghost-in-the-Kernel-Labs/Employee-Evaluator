def price_order(order, discounts):
    total = 0
    for line in order.lines:
        if line.quantity <= 0:
            continue
        price = line.unit_price * line.quantity
        if line.sku in discounts:
            price = price - price * discounts[line.sku] / 100
        total += price
    if total > 1000:
        total = total * 0.95
    return round(total, 2)


def describe(order):
    return "order " + order.id
