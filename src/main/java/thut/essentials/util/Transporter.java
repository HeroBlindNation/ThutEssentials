package thut.essentials.util;

import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class Transporter
{
    @SuppressWarnings("serial")
    public static class Vector3 extends Vector3f
    {

        public Vector3(double x, double y, double z)
        {
            super((float) x, (float) y, (float) z);
        }

        public Vector3(BlockPos moveTo)
        {
            this(moveTo.getX(), moveTo.getY(), moveTo.getZ());
        }

        public int intY()
        {
            return MathHelper.floor_double(y);
        }

        public int intX()
        {
            return MathHelper.floor_double(x);
        }

        public int intZ()
        {
            return MathHelper.floor_double(z);
        }

        public AxisAlignedBB getAABB()
        {
            return new AxisAlignedBB(x, y, z, x, y, z);
        }
    }

    private static Method copyDataFromOld;

    // From RFTools.
    public static class TTeleporter extends Teleporter
    {
        private final WorldServer worldServerInstance;
        private boolean           move = true;
        private double            x;
        private double            y;
        private double            z;

        public TTeleporter(WorldServer world, double x, double y, double z)
        {
            super(world);
            this.worldServerInstance = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public TTeleporter(WorldServer worldServerForDimension)
        {
            super(worldServerForDimension);
            this.worldServerInstance = worldServerForDimension;
            move = false;
        }

        @Override
        public void placeInPortal(Entity pEntity, float rotationYaw)
        {
            if (!move) return;
            this.worldServerInstance.getBlockState(new BlockPos((int) this.x, (int) this.y, (int) this.z));
            pEntity.setPosition(this.x, this.y, this.z);
            pEntity.motionX = 0.0f;
            pEntity.motionY = 0.0f;
            pEntity.motionZ = 0.0f;
        }

        @Override
        public void removeStalePortalLocations(long par1)
        {
        }

        @Override
        public boolean makePortal(Entity p_85188_1_)
        {
            return true;
        }
    }

    public static class ReMounter
    {
        final Entity theEntity;
        final Entity theMount;
        final int    dim;
        final long   time;

        public ReMounter(Entity entity, Entity mount, int dim)
        {
            theEntity = entity;
            theMount = mount;
            time = entity.worldObj.getTotalWorldTime();
            this.dim = dim;
        }

        @SubscribeEvent
        public void tick(TickEvent.ServerTickEvent evt)
        {
            if (evt.phase != TickEvent.Phase.END) return;
            if (theEntity.isDead) MinecraftForge.EVENT_BUS.unregister(this);
            if (theEntity.worldObj.getTotalWorldTime() >= time)
            {
                if (dim != theEntity.dimension)
                {
                    if (theEntity instanceof EntityPlayerMP)
                        theEntity.getServer().getPlayerList().transferPlayerToDimension((EntityPlayerMP) theEntity, dim,
                                new TTeleporter(theEntity.getServer().worldServerForDimension(dim)));
                    else
                    {
                        // Handle moving non players.
                    }
                }
                theEntity.setLocationAndAngles(theMount.posX, theMount.posY, theMount.posZ, theEntity.rotationYaw,
                        theEntity.rotationPitch);
                theEntity.startRiding(theMount);
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }
    }

    public static Entity teleportEntity(Entity entity, Vector3 t2, int dimension, boolean destBlocked)
    {
        if (entity.isRiding())
        {
            Entity mount = entity.getRidingEntity();
            mount = teleportEntity(mount, t2, dimension, false);
            return entity;
        }
        if (dimension != entity.dimension)
        {
            entity = transferToDimension(entity, t2, dimension);
            for (Entity e : entity.getRecursivePassengers())
            {
                transferToDimension(e, t2, dimension);
            }
        }
        int x = t2.intX() >> 4;
        int z = t2.intZ() >> 4;
        for (int i = x - 1; i <= x + 1; i++)
            for (int j = z - 1; j <= z + 1; j++)
            {
                entity.worldObj.getChunkFromChunkCoords(x, z);
            }
        entity.setPositionAndUpdate(t2.x, t2.y, t2.z);
        List<Entity> passengers = Lists.newArrayList(entity.getPassengers());
        for (Entity e : passengers)
        {
            e.dismountRidingEntity();
            e.setPositionAndUpdate(t2.x, t2.y, t2.z);
            MinecraftForge.EVENT_BUS.register(new ReMounter(e, entity, dimension));
        }
        WorldServer world = entity.getServer().worldServerForDimension(dimension);
        EntityTracker tracker = world.getEntityTracker();
        if (tracker.getTrackingPlayers(entity).getClass().getSimpleName().equals("EmptySet"))
        {
            tracker.trackEntity(entity);
            if (entity instanceof EntityPlayerMP)
            {
                EntityPlayerMP playerIn = (EntityPlayerMP) entity;
                tracker.updateVisibility(playerIn);
            }
        }
        return entity;
    }

    // From RFTools.
    private static Entity transferToDimension(Entity entity, Vector3 t2, int dimension)
    {
        int oldDimension = entity.worldObj.provider.getDimension();
        if (oldDimension == dimension) return entity;
        if (!(entity instanceof EntityPlayerMP)) { return changeDimension(entity, t2, dimension); }
        MinecraftServer server = entity.worldObj.getMinecraftServer();
        WorldServer worldServer = server.worldServerForDimension(dimension);
        Teleporter teleporter = new TTeleporter(worldServer, t2.x, t2.y, t2.z);
        EntityPlayerMP entityPlayerMP = (EntityPlayerMP) entity;
        entityPlayerMP.addExperienceLevel(0);
        worldServer.getMinecraftServer().getPlayerList().transferPlayerToDimension(entityPlayerMP, dimension,
                teleporter);
        if (oldDimension == 1)
        {
            // For some reason teleporting out of the end does weird things.
            worldServer.spawnEntityInWorld(entityPlayerMP);
            worldServer.updateEntityWithOptionalForce(entityPlayerMP, false);
        }
        return entityPlayerMP;
    }

    @Nullable
    // From Advanced Rocketry
    public static Entity changeDimension(Entity entityIn, Vector3 t2, int dimensionIn)
    {
        if (entityIn.dimension == dimensionIn) return entityIn;
        if (!entityIn.worldObj.isRemote && !entityIn.isDead)
        {
            List<Entity> passengers = entityIn.getPassengers();

            if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(entityIn, dimensionIn)) return null;
            entityIn.worldObj.theProfiler.startSection("changeDimension");
            MinecraftServer minecraftserver = entityIn.getServer();
            int i = entityIn.dimension;
            WorldServer worldserver = minecraftserver.worldServerForDimension(i);
            WorldServer worldserver1 = minecraftserver.worldServerForDimension(dimensionIn);
            entityIn.dimension = dimensionIn;

            if (i == 1 && dimensionIn == 1)
            {
                worldserver1 = minecraftserver.worldServerForDimension(0);
                entityIn.dimension = 0;
            }
            NBTTagCompound tag = new NBTTagCompound();
            entityIn.writeToNBT(tag);
            entityIn.worldObj.removeEntity(entityIn);
            entityIn.readFromNBT(tag);
            entityIn.isDead = false;
            entityIn.worldObj.theProfiler.startSection("reposition");

            double d0 = entityIn.posX;
            double d1 = entityIn.posZ;
            d0 = MathHelper.clamp_double(d0 * 8.0D, worldserver1.getWorldBorder().minX() + 16.0D,
                    worldserver1.getWorldBorder().maxX() - 16.0D);
            d1 = MathHelper.clamp_double(d1 * 8.0D, worldserver1.getWorldBorder().minZ() + 16.0D,
                    worldserver1.getWorldBorder().maxZ() - 16.0D);
            d0 = MathHelper.clamp_int((int) d0, -29999872, 29999872);
            d1 = MathHelper.clamp_int((int) d1, -29999872, 29999872);
            float f = entityIn.rotationYaw;
            entityIn.setLocationAndAngles(d0, entityIn.posY, d1, 90.0F, 0.0F);
            Teleporter teleporter = new TTeleporter(worldserver1, t2.x, t2.y, t2.z);
            teleporter.placeInExistingPortal(entityIn, f);
            worldserver.updateEntityWithOptionalForce(entityIn, false);
            entityIn.worldObj.theProfiler.endStartSection("reloading");
            Entity entity = CompatWrapper.createEntity(worldserver1, entityIn);
            if (entity != null)
            {
                if (copyDataFromOld == null)
                {
                    copyDataFromOld = ReflectionHelper.findMethod(Entity.class, entity,
                            new String[] { "a", "func_180432_n", "copyDataFromOld" }, Entity.class);
                }
                try
                {
                    copyDataFromOld.invoke(entity, entityIn);
                }
                catch (Exception e1)
                {
                    e1.printStackTrace();
                }
                entity.forceSpawn = true;
                worldserver1.spawnEntityInWorld(entity);
                worldserver1.updateEntityWithOptionalForce(entity, true);
                for (Entity e : passengers)
                {
                    // Fix that darn random crash?
                    worldserver.resetUpdateEntityTick();
                    worldserver1.resetUpdateEntityTick();
                    // Transfer the player if applicable
                    // Need to handle our own removal to avoid race condition
                    // where player is mounted on client on the old entity but
                    // is already mounted to the new one on server
                    MinecraftForge.EVENT_BUS.register(new ReMounter(e, entity, dimensionIn));
                }
            }
            entityIn.isDead = true;
            entityIn.worldObj.theProfiler.endSection();
            worldserver.resetUpdateEntityTick();
            worldserver1.resetUpdateEntityTick();
            entityIn.worldObj.theProfiler.endSection();
            return entity;
        }
        return null;
    }
}
