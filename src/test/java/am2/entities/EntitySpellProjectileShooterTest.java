package am2.entities;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.junit.Test;

public class EntitySpellProjectileShooterTest {

    @Test
    public void serverSideDummyCasterUsesTheDirectReference() throws Exception {
        EntitySpellProjectile projectile = projectile(false);
        EntityLivingBase direct = mock(EntityLivingBase.class);
        set(projectile, "directShooter", direct);

        assertSame(direct, invokeShooter(projectile));
        verify(projectile.worldObj, never()).getClosestPlayer(projectile.posX, projectile.posY, projectile.posZ, -1);
    }

    @Test
    public void serverSideMissingDirectReferenceDoesNotTargetANearbyPlayer() throws Exception {
        EntitySpellProjectile projectile = projectile(false);

        assertNull(invokeShooter(projectile));
        verify(projectile.worldObj, never()).getClosestPlayer(projectile.posX, projectile.posY, projectile.posZ, -1);
    }

    @Test
    public void clientSideMissingOwnerStillUsesTheVisualFallback() throws Exception {
        EntitySpellProjectile projectile = projectile(true);
        EntityPlayer closest = mock(EntityPlayer.class);
        when(projectile.worldObj.getClosestPlayer(projectile.posX, projectile.posY, projectile.posZ, -1))
            .thenReturn(closest);

        assertSame(closest, invokeShooter(projectile));
    }

    private static EntitySpellProjectile projectile(boolean remote) throws Exception {
        EntitySpellProjectile projectile = mock(EntitySpellProjectile.class);
        projectile.worldObj = mock(World.class);
        projectile.worldObj.isRemote = remote;
        DataWatcher watcher = mock(DataWatcher.class);
        when(watcher.getWatchableObjectInt(27)).thenReturn(-999);
        set(projectile, "dataWatcher", watcher);
        return projectile;
    }

    private static EntityLivingBase invokeShooter(EntitySpellProjectile projectile) throws Exception {
        Method method = EntitySpellProjectile.class.getDeclaredMethod("getShootingEntity");
        method.setAccessible(true);
        return (EntityLivingBase) method.invoke(projectile);
    }

    private static void set(Object owner, String name, Object value) throws Exception {
        Class<?> type = EntitySpellProjectile.class;
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(owner, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
