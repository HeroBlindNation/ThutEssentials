package thut.essentials.commands.land;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.land.LandManager;
import thut.essentials.util.BaseCommand;

public class Create extends BaseCommand
{
    public Create()
    {
        super("createTeam", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        String teamname = args[0];
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        LandManager.getInstance().createTeam(player.getUniqueID(), teamname);
        player.addChatMessage(new TextComponentString("You created Team " + teamname));
        return;
    }
}
