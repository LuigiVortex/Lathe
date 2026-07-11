package com.lathe.mod;

public class LatheMod {
    public LatheMod() {
        System.out.println("Lathe Mod (in-game runtime) initialized.");
        // TODO: Implement in-game mod menu, API hooks, etc.
    }

    public static void initialize() {
        new LatheMod();
    }
}
