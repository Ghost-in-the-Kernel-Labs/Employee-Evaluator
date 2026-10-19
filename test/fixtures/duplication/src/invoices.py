"""Invoices: priced the way orders are."""


def price_invoice(invoice, rebates):
    amount = 0
    for item in invoice.items:
        if item.count <= 0:
            continue
        cost = item.unit_cost * item.count
        if item.code in rebates:
            cost = cost - cost * rebates[item.code] / 100
        amount += cost
    if amount > 2000:
        amount = amount * 0.9
    return round(amount, 4)
