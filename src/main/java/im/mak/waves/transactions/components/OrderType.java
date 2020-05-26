package im.mak.waves.transactions.components;

public enum OrderType {

    BUY("buy"), SELL("sell");

    private final String value;

    OrderType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

}
