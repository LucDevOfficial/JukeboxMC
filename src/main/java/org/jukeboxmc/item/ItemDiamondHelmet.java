package org.jukeboxmc.item;

import org.jukeboxmc.item.behavior.ItemArmorBehavior;
import org.jukeboxmc.item.type.ArmorTierType;
import org.jukeboxmc.item.type.Durability;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class ItemDiamondHelmet extends ItemArmorBehavior implements Durability {

    public ItemDiamondHelmet() {
        super ( "minecraft:diamond_helmet" );
    }

    @Override
    public boolean useInAir( Player player, Vector clickVector ) {
        Item oldItem = player.getArmorInventory().getHelmet();
        player.getArmorInventory().setHelmet( this );
        player.getInventory().setItemInHand( oldItem );
        return true;
    }

    @Override
    public ArmorTierType getArmorTierType() {
        return ArmorTierType.DIAMOND;
    }

    @Override
    public int getArmorPoints() {
        return 3;
    }

    @Override
    public int getMaxDurability() {
        return 368;
    }

    @Override
    public int getMaxAmount() {
        return 1;
    }
}
