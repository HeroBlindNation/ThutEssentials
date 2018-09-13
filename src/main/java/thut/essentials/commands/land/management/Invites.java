package thut.essentials.commands.land.management;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.land.LandManager;
import thut.essentials.util.BaseCommand;

public class Invites extends BaseCommand
{

    public Invites()
    {
        super("teaminvites", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayerBySender(sender);
        List<String> c = LandManager.getInstance().getInvites(player.getUniqueID());
        if (c.isEmpty())
        {
            sender.sendMessage(new TextComponentString("You have no team invites"));
            return;
        }
        sender.sendMessage(new TextComponentString("List of Team Invites, You can click one to join."));
        String links = "";
        String cmd = "joinTeam";
        String command = "/" + cmd + " " + c.get(0);
        String abilityJson = "{\"text\":\"" + c.get(0)
                + "\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + command + ""
                + "\"}}";
        links = abilityJson;
        for (int i = 1; i < c.size(); i++)
        {
            String command2 = "/" + cmd + " " + c.get(i);
            String abilityJson2 = "{\"text\":\"" + c.get(i)
                    + "\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + command2 + ""
                    + "\"}}";
            links = links + ",\"]\"" + ",\"[\"," + abilityJson2;
        }
        ITextComponent message = ITextComponent.Serializer.jsonToComponent("[\" [\"," + links + ",\"]\"]");
        sender.sendMessage(message);
    }

}
