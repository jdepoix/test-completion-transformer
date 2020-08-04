package test;

import static org.junit.Assert.*;

public class TestClass {
    public int myTestMethod(int a) {
        privateMethod(a);
        assertTrue(true);
        return a;
    }

    private int privateMethod(int a) {
        return a * a;
    }
}