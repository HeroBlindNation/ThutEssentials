package thut.essentials.commands.land.management;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;

public class Join extends BaseCommand
{

    public Join()
    {
        super("jointeam", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayerBySender(sender);
        String teamname = args[0];
        LandTeam teamtojoin = LandManager.getInstance().getTeam(teamname, false);
        LandTeam oldTeam = LandManager.getTeam(player);
        if (oldTeam == teamtojoin) throw new CommandException("You are already in that team!");
        if (teamtojoin != null)
        {
            boolean canJoinInvite = PermissionAPI.hasPermission(player, LandEventsHandler.PERMJOINTEAMINVITED);
            canJoinInvite = canJoinInvite && LandManager.getInstance().hasInvite(player.getUniqueID(), teamname);
            boolean canJoinNoInvite = PermissionAPI.hasPermission(player, LandEventsHandler.PERMJOINTEAMNOINVITE);
            canJoinInvite = canJoinInvite
                    || teamtojoin.teamName.equalsIgnoreCase(ConfigManager.INSTANCE.defaultTeamName);
            if (canJoinInvite && teamtojoin.member.size() == 0)
            {
                canJoinInvite = !LandManager.getInstance().getTeam(teamname, false).reserved;
            }
            if (canJoinInvite || canJoinNoInvite)
            {
                LandManager.getInstance().addToTeam(player.getUniqueID(), teamname);
                player.sendMessage(new TextComponentString("You joined Team " + teamname));
                return;
            }
        }
        else throw new CommandException("No team found by name " + teamname);
        sender.sendMessage(new TextComponentString("You do not have an invite for Team " + teamname));
    }

}
