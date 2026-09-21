package dev.xyat.kineticcore.internal.config;

import com.google.gson.JsonParser;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

/** Ensure high-precision JSON numeric tokens survive the network decoder unchanged. */
public final class ServerConfigDecimalWireRegression {
    public static void main(String[] args) throws Exception {
        Object precise = decode("10.0000000000000001");
        check(precise instanceof BigDecimal && ((BigDecimal) precise).compareTo(new BigDecimal("10.0000000000000001")) == 0,
                "network decoder rounded fractional number into an integer");
        Object boundary = decode("1.00000000000000001");
        check(boundary instanceof BigDecimal && ((BigDecimal) boundary).compareTo(BigDecimal.ONE) > 0,
                "network decoder rounded an out-of-range decimal down to upper bound");
        check(decode("42").equals(42L), "integral JSON tokens should keep long representation");
        System.out.println("PASS: 3 network numeric-preservation regression cases");
    }

    private static Object decode(String text) throws Exception {
        Method method = ServerConfigNetwork.class.getDeclaredMethod("decodeElement", com.google.gson.JsonElement.class);
        method.setAccessible(true);
        try {return method.invoke(null, JsonParser.parseString(text));}
        catch (InvocationTargetException error) {throw new AssertionError("failed to decode " + text, error.getCause());}
    }

    private static void check(boolean condition, String message) {if (!condition) throw new AssertionError(message);}
}
