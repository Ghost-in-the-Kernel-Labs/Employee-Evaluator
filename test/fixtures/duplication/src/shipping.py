def ship_cost(parcel):
    total = 0
    for box in parcel.boxes:
        if box.weight <= 0:
            continue
        total += box.weight * 2
    return total
