package thut.essentials.commands.rtp;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.ThutEssentials;
import thut.essentials.commands.misc.Spawn;
import thut.essentials.commands.misc.Spawn.PlayerMover;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.PlayerDataHandler;

public class RTP extends BaseCommand
{
    private static final String PERMRTPOTHER = "thutessentials.rtp.other";

    public RTP()
    {
        super("rtp", 0);
        PermissionAPI.registerNode(PERMRTPOTHER, DefaultPermissionLevel.OP, "Is allowed to cast RTP on someone else?");
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player;
        if (args.length == 1)
        {
            if (sender instanceof PlayerEntity) if (!PermissionAPI.hasPermission((PlayerEntity) sender, PERMRTPOTHER))
                throw new CommandException("You do not have permission to RTP someone else.");
            player = getPlayer(server, sender, args[0]);
        }
        else
        {
            player = getPlayerBySender(sender);
        }
        BlockPos position;

        int delay = ConfigManager.INSTANCE.rtpReuseDelay;
        String timeTag = "rtp_time";
        long rtpTime = PlayerDataHandler.getCustomDataTag(player).getLong(timeTag);
        if ((delay <= 0 && rtpTime != 0) || server.getEntityWorld().getGameTime() < rtpTime)
            throw new CommandException("You cannot RTP again yet.");

        PlayerDataHandler.getCustomDataTag(player).putLong(timeTag,
                server.getEntityWorld().getGameTime() + delay);
        int n = 100;
        while ((position = checkSpot(player)) == null && n-- > 0)
            ;
        if (position != null) PlayerMover.setMove(player, ThutEssentials.instance.config.rtpActivateDelay,
                player.dimension, position, null, Spawn.INTERUPTED);
        else sender.sendMessage(new StringTextComponent("No spot found."));
    }

    private boolean isValid(BlockState ground, BlockState lower, BlockState upper)
    {
        return ground.isBlockNormalCube() && validMaterial(lower.getMaterial()) && validMaterial(upper.getMaterial());
    }

    private boolean validMaterial(Material mat)
    {
        return !mat.isSolid() && !mat.isLiquid();
    }

    // Calculate the next position to check.
    private BlockPos calculatePos()
    {
        Random rand = new Random();
        int x = (int) (Math.signum(rand.nextGaussian()) * rand.nextInt(ConfigManager.INSTANCE.rtpdistance));
        int z = (int) (Math.signum(rand.nextGaussian()) * rand.nextInt(ConfigManager.INSTANCE.rtpdistance));
        int y = 252;
        return new BlockPos(x, y, z);
    }

    private BlockPos checkSpot(PlayerEntity player)
    {
        BlockPos position = calculatePos();
        BlockState ground = player.getEntityWorld().getBlockState(position);
        BlockState lower = player.getEntityWorld().getBlockState(position.up());
        BlockState upper = player.getEntityWorld().getBlockState(position.up(2));
        // If the block we're standing on is air or liquid or if the two blocks
        // we take up are liquid then return null, check failed.
        if (isValid(ground, lower, upper)) { return position; }
        if (ground.getMaterial().isLiquid()) return null;
        while (!isValid(ground, lower, upper) && position.getY() > 0)
        {
            // Make a new position of y+1
            position = position.down();
            ground = player.getEntityWorld().getBlockState(position);
            lower = player.getEntityWorld().getBlockState(position.up());
            upper = player.getEntityWorld().getBlockState(position.up(2));
            if (ground.getMaterial().isLiquid()) return null;
        }
        return position.up(2);

    }

}
