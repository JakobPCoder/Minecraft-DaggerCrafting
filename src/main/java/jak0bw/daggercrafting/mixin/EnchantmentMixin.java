package jak0bw.daggercrafting.mixin;

import java.util.Set;

import jak0bw.daggercrafting.item.DaggerItem;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Broadens which vanilla enchantments apply to {@link DaggerItem} stacks.
 *
 * Daggers are custom items, so vanilla {@code supported_items} tags never list them. Without this
 * mixin, anvil combines and enchanting-table rolls would reject loyalty, sharpness, and the rest of
 * the documented dagger set even though {@link jak0bw.daggercrafting.entity.DaggerEntity} and
 * {@link DaggerItem} already consume those effects at runtime.
 *
 * Matching uses exact registry ids parsed from translation keys of the form
 * {@code enchantment.<namespace>.<path>} (for example {@code enchantment.minecraft.loyalty} →
 * {@code minecraft:loyalty}). Substring matching is avoided so a third-party id that merely contains
 * {@code smite} cannot leak onto daggers. Custom enchantments such as throwing speed are not listed
 * here; their datapack {@code supported_items} already include {@code #daggercrafting:daggers}.
 */
@Mixin(Enchantment.class)
public class EnchantmentMixin {

	/**
	 * Vanilla combat and throw enchantments the enchanting table may offer on daggers.
	 *
	 * Mending is omitted so it stays treasure/anvil-only, matching vanilla table rules.
	 */
	private static final Set<Identifier> TABLE_IDS = Set.of(
		id("minecraft", "flame"),
		id("minecraft", "fire_aspect"),
		id("minecraft", "loyalty"),
		id("minecraft", "unbreaking"),
		id("minecraft", "sharpness"),
		id("minecraft", "smite"),
		id("minecraft", "bane_of_arthropods")
	);

	/**
	 * Anvil and book-apply set: table offers plus mending.
	 */
	private static final Set<Identifier> ANVIL_IDS = Set.of(
		id("minecraft", "flame"),
		id("minecraft", "fire_aspect"),
		id("minecraft", "loyalty"),
		id("minecraft", "unbreaking"),
		id("minecraft", "sharpness"),
		id("minecraft", "smite"),
		id("minecraft", "bane_of_arthropods"),
		id("minecraft", "mending")
	);

	/**
	 * Builds a registry id for the static allow-lists.
	 *
	 * @param namespace id namespace, always {@code minecraft} for this mixin
	 * @param path enchantment path such as {@code loyalty}
	 * @return identifier compared with {@link #enchantmentIdFromDescription}
	 */
	private static Identifier id(String namespace, String path) {
		return Identifier.fromNamespaceAndPath(namespace, path);
	}

	/**
	 * Shared gate for injector methods: only daggers are affected; other items keep vanilla results.
	 *
	 * When the enchantment description is not a matching translate key, this returns without setting
	 * {@code cir} so vanilla {@code canEnchant} / {@code isPrimaryItem} run. Unknown datapack
	 * enchantments therefore stay off daggers unless their own {@code supported_items} already include
	 * them.
	 *
	 * @param self the enchantment being tested (mixin target cast from {@code this})
	 * @param stack candidate item in anvil or enchanting table
	 * @param cir inject callback; set to {@code true} when the parsed id is in {@code allowedIds}
	 * @param allowedIds exact registry ids allowed for this hook
	 */
	private static void shouldAllowEnchantment(
		Enchantment self,
		ItemStack stack,
		CallbackInfoReturnable<Boolean> cir,
		Set<Identifier> allowedIds
	) {
		if (!(stack.getItem() instanceof DaggerItem)) {
			return;
		}
		Identifier enchantmentId = enchantmentIdFromDescription(self);
		if (enchantmentId != null && allowedIds.contains(enchantmentId)) {
			cir.setReturnValue(true);
		}
	}

	/**
	 * Parses {@code enchantment.<namespace>.<path>} from the enchantment description into a registry id.
	 *
	 * Vanilla and this mod always use that translate-key shape equal to the registry id. Other
	 * description types (literal text, unexpected keys) return {@code null} so they are not
	 * allow-listed.
	 *
	 * @param self enchantment whose {@link Enchantment#description()} is inspected
	 * @return parsed id, or {@code null} when the key is not {@code enchantment.ns.path}
	 */
	private static Identifier enchantmentIdFromDescription(Enchantment self) {
		if (!(self.description().getContents() instanceof TranslatableContents translatable)) {
			return null;
		}
		String key = translatable.getKey();
		final String prefix = "enchantment.";
		if (!key.startsWith(prefix)) {
			return null;
		}
		String remainder = key.substring(prefix.length());
		int namespaceEnd = remainder.indexOf('.');
		if (namespaceEnd <= 0 || namespaceEnd == remainder.length() - 1) {
			return null;
		}
		String namespace = remainder.substring(0, namespaceEnd);
		String path = remainder.substring(namespaceEnd + 1);
		return Identifier.tryParse(namespace + ":" + path);
	}

	/**
	 * Anvil (and book apply) path: {@link Enchantment#canEnchant(ItemStack)}.
	 *
	 * Includes {@code minecraft:mending}, which is intentionally omitted from the table hook below.
	 */
	@Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
	private void allowEnchantmentOnAnvil(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		shouldAllowEnchantment((Enchantment) (Object) this, stack, cir, ANVIL_IDS);
	}

	/**
	 * Enchanting table primary-item check: {@link Enchantment#isPrimaryItem(ItemStack)}.
	 *
	 * Same combat/throw set as anvil except mending.
	 */
	@Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
	private void allowEnchantmentOnEnchantingTable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		shouldAllowEnchantment((Enchantment) (Object) this, stack, cir, TABLE_IDS);
	}
}
