package jak0bw.daggercrafting;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public class ModEnchantments {
	public static final RegistryKey<Enchantment> THROWING_SPEED = of("throwing_speed");

	private static RegistryKey<Enchantment> of(String path) {
		Identifier id = Identifier.of(DaggerCrafting.MOD_ID, path);
		return RegistryKey.of(RegistryKeys.ENCHANTMENT, id);
	}

	public static void registerModEnchantments() {
		DaggerCrafting.LOGGER.info("Registering Enchantments for " + DaggerCrafting.MOD_ID);
	}
} 