package thut.essentials.commands.land;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandManager.LandTeam.Rank;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.BaseCommand;

public class EditRanks extends BaseCommand
{

    public EditRanks()
    {
        super("teamranks", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        LandTeam landTeam = LandManager.getTeam(player);
        if (!landTeam.isAdmin(player)) throw new CommandException("Only Team Admins may manage ranks");
        String type = args[0];
        String rankName;
        EntityPlayer target;
        Rank rank;
        String perm;
        boolean added;
        switch (type)
        {
        case "addRank":
            rankName = args[1];
            rank = landTeam.rankMap.get(rankName);
            if (rank != null) throw new CommandException("Rank " + rankName + " already exists.");
            landTeam.rankMap.put(rankName, new Rank());
            player.addChatMessage(new TextComponentString("Added Rank " + rankName));
            LandSaveHandler.saveTeam(landTeam.teamName);
            break;
        case "setRank":
            rankName = args[1];
            target = getPlayer(server, sender, args[2]);
            rank = landTeam.rankMap.get(rankName);
            if (rank == null) throw new CommandException("Rank " + rankName + " does not exist.");
            rank.members.add(target.getUniqueID());
            landTeam.ranksMembers.put(target.getUniqueID(), rank);
            target.refreshDisplayName();
            player.addChatMessage(new TextComponentString("Added " + target.getName() + " to Rank " + rankName));
            LandSaveHandler.saveTeam(landTeam.teamName);
            break;
        case "setPerm":
            rankName = args[1];
            rank = landTeam.rankMap.get(rankName);
            if (rank == null) throw new CommandException("Rank " + rankName + " does not exist.");
            perm = args[2];
            added = rank.perms.add(perm);
            if (added) player.addChatMessage(new TextComponentString("Allowed " + perm));
            else player.addChatMessage(new TextComponentString("Already has " + perm));
            break;
        case "delPerm":
            rankName = args[1];
            rank = landTeam.rankMap.get(rankName);
            if (rank == null) throw new CommandException("Rank " + rankName + " does not exist.");
            perm = args[2];
            added = rank.perms.remove(perm);
            if (added) player.addChatMessage(new TextComponentString("Removed " + perm));
            else player.addChatMessage(new TextComponentString("Did not have " + perm));
            break;
        case "setPrefix":
            rankName = args[1];
            rank = landTeam.rankMap.get(rankName);
            if (rank == null) throw new CommandException("Rank " + rankName + " does not exist.");
            perm = args[2];
            rank.prefix = perm;
            if (perm.trim().isEmpty()) rank.prefix = null;
            added = rank.prefix != null;
            if (added) player.addChatMessage(new TextComponentString("Set Prefix to " + rank.prefix));
            else player.addChatMessage(new TextComponentString("Removed Rank Prefix"));
            break;
        case "listRanks":
            Set<String> ranks = landTeam.rankMap.keySet();
            player.addChatMessage(new TextComponentString("Ranks in your team:"));
            for (String s : ranks)
            {
                player.addChatMessage(new TextComponentString("  " + s));
            }
            break;
        case "listMembers":
            rankName = args[1];
            rank = landTeam.rankMap.get(rankName);
            if (rank == null) throw new CommandException("Rank " + rankName + " does not exist.");
            Collection<?> c = rank.members;
            player.addChatMessage(new TextComponentString("Members of " + rankName));
            for (Object o : c)
            {
                GameProfile profile = server.getMinecraftSessionService()
                        .fillProfileProperties(new GameProfile((UUID) o, null), true);
                sender.addChatMessage(new TextComponentString("  " + profile.getName()));
            }
            break;
        }
    }

}