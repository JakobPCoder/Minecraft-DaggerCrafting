package jak0bw.daggercrafting.item;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ItemTags {
    public static final TagKey<Item> REPAIRS_WOODEN_DAGGER = TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_wooden_dagger"));
    public static final TagKey<Item> REPAIRS_STONE_DAGGER = TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_stone_dagger"));
    public static final TagKey<Item> REPAIRS_IRON_DAGGER = TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_iron_dagger"));
    public static final TagKey<Item> REPAIRS_GOLDEN_DAGGER = TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_golden_dagger"));
    public static final TagKey<Item> REPAIRS_DIAMOND_DAGGER = TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_diamond_dagger"));
    public static final TagKey<Item> REPAIRS_NETHERITE_DAGGER = TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_netherite_dagger"));
}
