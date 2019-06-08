package thut.essentials.commands.land.management;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;

public class RemoveAdmin extends BaseCommand
{

    public RemoveAdmin()
    {
        super("removeteamadmin", 0);
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
        PlayerEntity user = getPlayerBySender(sender);
        GameProfile player = getProfile(server, args[0]);
        LandTeam teamA = LandManager.getTeam(user);
        LandTeam teamB = LandManager.getTeam(player.getId());
        if (teamA != teamB) throw new CommandException("You must be in the same team to do that.");
        String teamName = teamA.teamName;
        if (LandManager.getInstance().isAdmin(user.getUniqueID()))
        {
            LandManager.getInstance().removeAdmin(player.getId());
            sender.sendMessage(new StringTextComponent(player + " removed as an Admin for Team " + teamName));
        }
        else
        {
            throw new CommandException("You do not have permission to do that.");
        }
    }

}
