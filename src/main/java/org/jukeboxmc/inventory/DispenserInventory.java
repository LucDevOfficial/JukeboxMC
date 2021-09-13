package org.jukeboxmc.inventory;

import org.jukeboxmc.blockentity.BlockEntityDispenser;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class DispenserInventory extends ContainerInventory {

    public DispenserInventory( InventoryHolder holder ) {
        super( holder, -1, 9 );
    }

    @Override
    public BlockEntityDispenser getInventoryHolder() {
        return (BlockEntityDispenser) this.holder;
    }

    @Override
    public InventoryType getInventoryType() {
        return InventoryType.DISPENSER;
    }

    @Override
    public WindowTypeId getWindowTypeId() {
        return WindowTypeId.DISPENSER;
    }
}