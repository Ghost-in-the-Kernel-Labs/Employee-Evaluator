package shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import shop.money.Price;
import static shop.money.Price.ZERO;

public class Cart {
    List<Price> items = new ArrayList<>();

    Price total() {
        return ZERO;
    }
}
