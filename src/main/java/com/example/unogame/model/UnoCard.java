package com.example.unogame.model;

/**
 * Represents a single UNO card with a color and a value.
 * Provides logic to determine playability.
 *
 * @author
 *   Jhon Steven Angulo Nieves
 *   Braulio Robledo Delgado
 * @version 1.0
 */
public class UnoCard {

    /**
     * Enumeration of possible UNO card colors.
     */
    public enum Color {
        RED, BLUE, GREEN, YELLOW, WILD;
    }

    /**
     * Enumeration of possible UNO card values.
     */
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

    /**
     * Constructs a new UnoCard with the specified color and value.
     *
     * @param color the color of the card.
     * @param value the value/action of the card.
     */
    public UnoCard(Color color, Value value) {
        this.color = color;
        this.value = value;
    }


    /**
     * Returns the color of this card.
     *
     * @return the card's color.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the value of this card.
     *
     * @return the card's value.
     */
    public Value getValue() {
        return value;
    }

    /**
     * Returns a string representation of the card (e.g., "RED_FIVE").
     *
     * @return string combining color and value.
     */
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

