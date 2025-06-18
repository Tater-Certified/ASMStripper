package com.github.tatercertified.stripper_test.mixin;

import com.github.tatercertified.asm_stripper.api.annotation.Strip;
import com.github.tatercertified.asm_stripper.api.annotation.Strippable;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Strippable
@Mixin(Mth.class)
public abstract class MathHelperMixin {
    @Strip
    @Shadow @Final public static float EPSILON;

    @Strip
    @Shadow
    public static int floorDiv(int i, int j) {
        return 0;
    }
}
