package org.jukeboxmc.entity.passive;

import org.jukeboxmc.entity.EntityLiving;
import org.jukeboxmc.entity.EntityType;

/**
 * @author Kaooot
 * @version 1.0
 */
public class EntityTadpole extends EntityLiving {

    @Override
    public String getName() {
        return "Tadpole";
    }

    @Override
    public float getWidth() {
        return 0.8f;
    }

    @Override
    public float getHeight() {
        return 0.6f;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TADPOLE;
    }
}