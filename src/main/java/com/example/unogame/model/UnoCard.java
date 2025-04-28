package com.example.unogame.model;

public class UnoCard {
    public enum Color {
        RED, BLUE, GREEN, YELLOW, WILD;
    }

    public enum Value {
        ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE,
        SKIP, REVERSE, DRAW_TWO, WILD, WILD_DRAW_FOUR;

        private static final Value[] values = Value.values();
        public static Value getValue(int i) {
            return Value.values[i];
        }
    }

    private final Color color;
    private final Value value;

    public UnoCard(Color color, Value value) {
        this.color = color;
        this.value = value;
    }

    public Color getColor() {
        return color;
    }

    public Value getValue() {
        return value;
    }

    public String toString() {
        return color.name() + "_" + value.name();
    }

    public String toFileName() {
        switch (value) {
            case ZERO:
                case ONE:
                    case TWO:
                        case THREE:
                            case FOUR:
                                case FIVE:
                                    case SIX:
                                        case SEVEN:
                                            case EIGHT:
                                                case NINE:

                                                    return value.ordinal() + "_" + color.name().toLowerCase() + ".png";

            case SKIP:
                return "skip_" + color.name().toLowerCase() + ".png";

            case REVERSE:
                return "reserve_" + color.name().toLowerCase() + ".png";

            case DRAW_TWO:
                return "2_wild_draw_" + color.name().toLowerCase() + ".png";

            case WILD:
                return "wild.png";

            case WILD_DRAW_FOUR:
                return "4_wild_draw.png";

            default:
                throw new IllegalStateException("Valor desconocido: " + value);
        }
    }
}