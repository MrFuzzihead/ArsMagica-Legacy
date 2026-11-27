package am2.mixins.early;

import static am2.mixins.MixinTestSupport.inject;
import static am2.mixins.MixinTestSupport.mixinTarget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Random;

import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.junit.Test;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bytecode guards for the common {@code @Fix} groups: ethereal filtering, no-clip/water behaviour, redstone inlays,
 * liquid essence and the ethereal inventory-click guard.
 */
public class EtherealMixinTest {

    @Test
    public void worldEntityQueriesFilterEtherealPlayersAtReturn() throws Exception {
        assertEquals(World.class.getName(), mixinTarget(WorldEntityQueryMixin.class));

        assertInject(
            WorldEntityQueryMixin.class,
            "arsmagica2$filterEtherealEntities",
            "selectEntitiesWithinAABB",
            "RETURN",
            Class.class,
            AxisAlignedBB.class,
            IEntitySelector.class,
            CallbackInfoReturnable.class);
        assertInject(
            WorldEntityQueryMixin.class,
            "arsmagica2$filterEtherealEntitiesExcluding",
            "getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/command/IEntitySelector;)Ljava/util/List;",
            "RETURN",
            CallbackInfoReturnable.class);
        assertInject(
            WorldEntityQueryMixin.class,
            "arsmagica2$filterEtherealClosestPlayer",
            "getClosestPlayer",
            "RETURN",
            CallbackInfoReturnable.class);
    }

    @Test
    public void worldRedstoneInlayAnswersThePowerQuery() throws Exception {
        assertEquals(World.class.getName(), mixinTarget(WorldRedstoneMixin.class));
        assertInject(
            WorldRedstoneMixin.class,
            "arsmagica2$redstoneInlay",
            "isBlockIndirectlyGettingPowered",
            "HEAD",
            int.class,
            int.class,
            int.class,
            CallbackInfoReturnable.class);
    }

    @Test
    public void entityEtherealAndWaterFixesAreRegistered() throws Exception {
        assertEquals(Entity.class.getName(), mixinTarget(EntityMixin.class));

        assertInject(
            EntityMixin.class,
            "arsmagica2$noClipOverridesOpaqueBlock",
            "isEntityInsideOpaqueBlock",
            "RETURN",
            CallbackInfoReturnable.class);
        assertInject(
            EntityMixin.class,
            "arsmagica2$etherealWaterMovement",
            "handleWaterMovement",
            "HEAD",
            CallbackInfoReturnable.class);
        assertInject(
            EntityMixin.class,
            "arsmagica2$etherealCollision",
            "applyEntityCollision",
            "HEAD",
            Entity.class,
            CallbackInfo.class);
        assertInject(
            EntityMixin.class,
            "arsmagica2$waterAffinityMaterial",
            "isInsideOfMaterial",
            "HEAD",
            Material.class,
            CallbackInfoReturnable.class);
        assertInject(EntityMixin.class, "arsmagica2$waterAffinity", "isInWater", "HEAD", CallbackInfoReturnable.class);
    }

    @Test
    public void entityClientMixinHandlesTrueSight() throws Exception {
        assertEquals(Entity.class.getName(), mixinTarget(EntityClientMixin.class));
        assertInject(
            EntityClientMixin.class,
            "arsmagica2$trueSightSeesInvisible",
            "isInvisibleToPlayer",
            "HEAD",
            EntityPlayer.class,
            CallbackInfoReturnable.class);
    }

    @Test
    public void entityLivingBaseEtherealFixesAreRegistered() throws Exception {
        assertEquals(EntityLivingBase.class.getName(), mixinTarget(EntityLivingBaseMixin.class));

        assertInject(
            EntityLivingBaseMixin.class,
            "arsmagica2$etherealCanBePushed",
            "canBePushed",
            "HEAD",
            CallbackInfoReturnable.class);
        assertInject(
            EntityLivingBaseMixin.class,
            "arsmagica2$etherealCanBeCollidedWith",
            "canBeCollidedWith",
            "HEAD",
            CallbackInfoReturnable.class);
        assertInject(
            EntityLivingBaseMixin.class,
            "arsmagica2$etherealOnLadder",
            "isOnLadder",
            "HEAD",
            CallbackInfoReturnable.class);
    }

    @Test
    public void blockLiquidSkipsCurrentForWaterAffinity() throws Exception {
        assertEquals(BlockLiquid.class.getName(), mixinTarget(BlockLiquidMixin.class));
        assertInject(
            BlockLiquidMixin.class,
            "arsmagica2$waterAffinityVelocity",
            "velocityToAddToEntity",
            "HEAD",
            World.class,
            int.class,
            int.class,
            int.class,
            Entity.class,
            Vec3.class,
            CallbackInfo.class);
    }

    @Test
    public void blockDynamicLiquidReactsToEssence() throws Exception {
        assertEquals(BlockDynamicLiquid.class.getName(), mixinTarget(BlockDynamicLiquidMixin.class));
        assertInject(
            BlockDynamicLiquidMixin.class,
            "arsmagica2$lavaEssenceReaction",
            "updateTick",
            "HEAD",
            World.class,
            int.class,
            int.class,
            int.class,
            Random.class,
            CallbackInfo.class);
    }

    @Test
    public void blockLiquidInvokerUsesTheVanillaFizzHelper() throws Exception {
        assertEquals(BlockLiquid.class.getName(), mixinTarget(BlockLiquidInvoker.class));

        java.lang.reflect.Method method = BlockLiquidInvoker.class
            .getDeclaredMethod("am2$fizz", World.class, int.class, int.class, int.class);
        Invoker invoker = method.getAnnotation(Invoker.class);
        assertNotNull("@Invoker is missing", invoker);
        assertEquals("func_149799_m", invoker.value());
    }

    @Test
    public void netHandlerProtectsEtherealClicks() throws Exception {
        assertEquals(NetHandlerPlayServer.class.getName(), mixinTarget(NetHandlerPlayServerMixin.class));

        Field field = NetHandlerPlayServerMixin.class.getDeclaredField("playerEntity");
        assertNotNull("@Shadow is missing on playerEntity", field.getAnnotation(Shadow.class));
        assertEquals(EntityPlayerMP.class, field.getType());

        assertInject(
            NetHandlerPlayServerMixin.class,
            "arsmagica2$etherealClickProtection",
            "processClickWindow",
            "HEAD",
            C0EPacketClickWindow.class,
            CallbackInfo.class);
    }

    private static void assertInject(Class<?> mixinClass, String handlerName, String targetMethod, String atValue,
        Class<?>... parameterTypes) throws Exception {
        Inject inject = inject(mixinClass, handlerName, parameterTypes);
        assertEquals(1, inject.require());
        assertEquals(1, inject.method().length);
        assertEquals(targetMethod, inject.method()[0]);
        assertEquals(1, inject.at().length);
        assertEquals(atValue, inject.at()[0].value());
    }
}
