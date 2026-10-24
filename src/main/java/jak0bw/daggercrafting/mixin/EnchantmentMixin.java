package jak0bw.daggercrafting.mixin;

import jak0bw.daggercrafting.item.DaggerItem;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets selected vanilla (and throwing_speed) enchantments apply to daggers even when
 * vanilla {@code supported_items} tags do not list them.
 */
@Mixin(Enchantment.class)
public class EnchantmentMixin {

	private static void shouldAllowEnchantment(
		Enchantment self,
		ItemStack stack,
		CallbackInfoReturnable<Boolean> cir,
		String[] validEnchantments
	) {
		if (!(stack.getItem() instanceof DaggerItem)) {
			return;
		}
		String enchantmentPath = enchantmentPath(self);
		for (String valid : validEnchantments) {
			if (enchantmentPath.contains(valid)) {
				cir.setReturnValue(true);
				break;
			}
		}
	}

	/**
	 * Uses the translation key path ({@code sharpness}) rather than English {@code toString()}.
	 */
	private static String enchantmentPath(Enchantment self) {
		if (self.description().getContents() instanceof TranslatableContents translatable) {
			String key = translatable.getKey();
			int lastDot = key.lastIndexOf('.');
			return (lastDot >= 0 ? key.substring(lastDot + 1) : key).toLowerCase();
		}
		return self.description().getString().toLowerCase();
	}

	@Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
	private void allowEnchantmentOnAnvil(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		shouldAllowEnchantment(
			(Enchantment) (Object) this,
			stack,
			cir,
			new String[] {
				"flame",
				"fire_aspect",
				"throwing_speed",
				"loyalty",
				"unbreaking",
				"sharpness",
				"smite",
				"bane_of_arthropods",
				"mending"
			}
		);
	}

	@Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
	private void allowEnchantmentOnEnchantingTable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		shouldAllowEnchantment(
			(Enchantment) (Object) this,
			stack,
			cir,
			new String[] {
				"flame",
				"fire_aspect",
				"throwing_speed",
				"loyalty",
				"unbreaking",
				"sharpness",
				"smite",
				"bane_of_arthropods"
			}
		);
	}
}
