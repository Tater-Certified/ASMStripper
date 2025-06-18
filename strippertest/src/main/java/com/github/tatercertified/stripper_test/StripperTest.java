package com.github.tatercertified.stripper_test;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StripperTest implements ModInitializer {
    Logger LOGGER = LoggerFactory.getLogger("Stripper Test");

    @Override
    public void onInitialize() {
        try {
            Mth.class.getMethod("floorDiv", int.class, int.class);
            LOGGER.info("Mixin Method Strip Failed");
        } catch (Exception e) {
            LOGGER.info("Mixin Method Strip Passed");
        }

        try {
            Mth.class.getField("EPSILON");
            LOGGER.info("Mixin Field Strip Failed");
        } catch (Exception e) {
            LOGGER.info("Mixin Field Strip Passed");
        }
    }
}
