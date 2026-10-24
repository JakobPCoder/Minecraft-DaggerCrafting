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
 * Broadens which enchantments vanilla considers valid on {@link DaggerItem} stacks.
 *
 * Daggers are custom {@link net.minecraft.world.item.Item}s, not swords or tridents, so datapack
 * {@code supported_items} tags never list them. Without this mixin, anvil combines and enchanting-table
 * rolls would reject loyalty, sharpness, {@link jak0bw.daggercrafting.ModEnchantments#THROWING_SPEED}, etc.
 * even though {@link jak0bw.daggercrafting.entity.DaggerEntity} and {@link DaggerItem} already consume
 * those effects at runtime.
 *
 * Matching is done by enchantment id fragment (e.g. {@code loyalty}) parsed from the translation key,
 * not by registry object identity, so renamed datapack enchantments still work when their lang key ends
 * with the expected path.
 */
@Mixin(Enchantment.class)
public class EnchantmentMixin {

	/**
	 * Shared gate for injector methods: only daggers are affected; other items keep vanilla results.
	 *
	 * @param self the enchantment being tested (mixin target cast from {@code this})
	 * @param stack candidate item in anvil or enchanting table
	 * @param cir inject callback; set to {@code true} when {@code validEnchantments} contains a substring of the enchant path
	 * @param validEnchantments allow-list for this hook (table vs anvil lists differ slightly)
	 */
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
	 * Stable id fragment for an {@link Enchantment}, used to match against allow-list strings.
	 *
	 * Prefers the last segment of the translation key ({@code enchantment.minecraft.sharpness} →
	 * {@code sharpness}) so locale changes do not break matching. Falls back to lowercased display
	 * string only when the description is not translatable.
	 */
	private static String enchantmentPath(Enchantment self) {
		if (self.description().getContents() instanceof TranslatableContents translatable) {
			String key = translatable.getKey();
			int lastDot = key.lastIndexOf('.');
			return (lastDot >= 0 ? key.substring(lastDot + 1) : key).toLowerCase();
		}
		return self.description().getString().toLowerCase();
	}

	/**
	 * Anvil (and book apply) path: {@link Enchantment#canEnchant(ItemStack)}.
	 *
	 * Includes {@code mending}, which is intentionally omitted from the table hook below.
	 */
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

	/**
	 * Enchanting table primary-item check: {@link Enchantment#isPrimaryItem(ItemStack)}.
	 *
	 * Same combat/throw set as anvil except mending (treasure enchant; table uses different rules).
	 */
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
