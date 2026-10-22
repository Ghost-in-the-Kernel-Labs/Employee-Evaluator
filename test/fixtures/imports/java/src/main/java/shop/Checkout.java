package shop;

import java.util.List;

public class Checkout {
    List<String> lines(Cart cart) {
        return List.of(String.valueOf(cart.total()));
    }
}
