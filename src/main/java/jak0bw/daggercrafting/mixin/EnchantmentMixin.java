package jak0bw.daggercrafting.mixin;

import jak0bw.daggercrafting.item.DaggerItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentMixin {

    private static void shouldAllowEnchantment(
            Enchantment self,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir,
            String[] validEnchantments,
            String logContext
    ) {
        if (!(stack.getItem() instanceof DaggerItem)) {
            return;
        }
        String enchantmentString = self.toString().toLowerCase();
        System.out.println("Checking enchantment: " + enchantmentString + " for dagger (" + logContext + ")");
        for (String valid : validEnchantments) {
            if (enchantmentString.contains(valid)) {
                System.out.println("ALLOWING " + valid.toUpperCase() + " ON DAGGER! (" + logContext + ")");
                cir.setReturnValue(true);
                break;
            }
        }
    }

    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    private void allowEnchantmentOnAnvil(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        shouldAllowEnchantment(
            (Enchantment)(Object)this,
            stack,
            cir,
            new String[]{"loyalty", "unbreaking", "sharpness", "smite", "bane_of_arthropods", "mending", "flame", "fire_aspect", "throwing_speed"},
            "anvil"
        );
    }

    @Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
    private void allowEnchantmentOnEnchantingTable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        shouldAllowEnchantment(
            (Enchantment)(Object)this,
            stack,
            cir,
            new String[]{"loyalty", "unbreaking", "sharpness", "smite", "bane_of_arthropods", "flame", "fire_aspect", "throwing_speed"},
            "enchanting_table"
        );
    }
} 