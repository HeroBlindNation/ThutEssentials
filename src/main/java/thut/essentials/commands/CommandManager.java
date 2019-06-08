package thut.essentials.commands;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;

public class CommandManager
{
    public static class ClassFinder
    {

        private static final char   DOT               = '.';

        private static final char   SLASH             = '/';

        private static final String CLASS_SUFFIX      = ".class";

        private static final String BAD_PACKAGE_ERROR = "Unable to get resources from path '%s'. Are you sure the package '%s' exists?";

        public static List<Class<?>> find(String scannedPackage) throws UnsupportedEncodingException
        {
            String scannedPath = scannedPackage.replace(DOT, SLASH);
            URL scannedUrl = Thread.currentThread().getContextClassLoader().getResource(scannedPath);
            if (scannedUrl == null) { throw new IllegalArgumentException(
                    String.format(BAD_PACKAGE_ERROR, scannedPath, scannedPackage)); }
            File scannedDir = new File(
                    java.net.URLDecoder.decode(scannedUrl.getFile(), Charset.defaultCharset().name()));

            List<Class<?>> classes = new ArrayList<Class<?>>();
            if (scannedDir.exists()) for (File file : scannedDir.listFiles())
            {
                classes.addAll(findInFolder(file, scannedPackage));
            }
            else if (scannedDir.toString().contains("file:") && scannedDir.toString().contains(".jar"))
            {
                String name = scannedDir.toString();
                String pack = name.split("!")[1].replace(File.separatorChar, SLASH).substring(1) + SLASH;
                name = name.replace("file:", "");
                name = name.replaceAll("(.jar)(.*)", ".jar");
                scannedDir = new File(name);
                try
                {
                    ZipFile zip = new ZipFile(scannedDir);
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    int n = 0;
                    while (entries.hasMoreElements() && n < 10)
                    {
                        ZipEntry entry = entries.nextElement();
                        String s = entry.getName();
                        if (s.contains(pack) && s.endsWith(CLASS_SUFFIX))
                        {
                            try
                            {
                                classes.add(Class.forName(s.replace(CLASS_SUFFIX, "").replace(SLASH, DOT)));
                            }
                            catch (ClassNotFoundException ignore)
                            {
                            }
                        }
                    }
                    zip.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            return classes;
        }

        private static List<Class<?>> findInFolder(File file, String scannedPackage)
        {
            List<Class<?>> classes = new ArrayList<Class<?>>();
            String resource = scannedPackage + DOT + file.getName();
            if (file.isDirectory())
            {
                for (File child : file.listFiles())
                {
                    classes.addAll(findInFolder(child, resource));
                }
            }
            else if (resource.endsWith(CLASS_SUFFIX))
            {
                int endIndex = resource.length() - CLASS_SUFFIX.length();
                String className = resource.substring(0, endIndex);
                try
                {
                    classes.add(Class.forName(className));
                }
                catch (ClassNotFoundException ignore)
                {
                }
            }
            return classes;
        }

    }

    public static ITextComponent makeFormattedComponent(String text, TextFormatting colour, boolean bold)
    {
        return new StringTextComponent(text).setStyle(new Style().setBold(bold).setColor(colour));
    }

    public static ITextComponent makeFormattedComponent(String text, TextFormatting colour)
    {
        return new StringTextComponent(text).setStyle(new Style().setColor(colour));
    }

    public static ITextComponent makeFormattedCommandLink(String text, String command, TextFormatting colour,
            boolean bold)
    {
        return new StringTextComponent(text).setStyle(
                new Style().setBold(bold).setColor(colour).setClickEvent(new ClickEvent(Action.RUN_COMMAND, command)));
    }

    public static boolean isOp(ICommandSource sender, String bypasslimit)
    {
        if (sender instanceof PlayerEntity)
        {
            return PermissionAPI.hasPermission((PlayerEntity) sender, bypasslimit);
        }
        else if (sender instanceof TileEntityCommandBlock) { return true; }
        return sender.getName().equalsIgnoreCase("@") || sender.getName().equals("Server");
    }

    public static Map<String, List<String>> commands = Maps.newHashMap();

    static
    {
        commands.put("colour", Lists.newArrayList("colour", "color", "col"));
        commands.put("tpa", Lists.newArrayList("tpa"));
        commands.put("tptoggle", Lists.newArrayList("tptoggle"));
        commands.put("fly", Lists.newArrayList("fly"));
        commands.put("spawn", Lists.newArrayList("spawn"));
        commands.put("back", Lists.newArrayList("back"));

        commands.put("staff", Lists.newArrayList("staff", "sc"));

        commands.put("heal", Lists.newArrayList("heal"));
        commands.put("repair", Lists.newArrayList("repair"));

        // Warp related
        commands.put("setwarp", Lists.newArrayList("setwarp"));
        commands.put("delwarp", Lists.newArrayList("delwarp"));
        commands.put("warp", Lists.newArrayList("warp"));

        // Home related
        commands.put("sethome", Lists.newArrayList("sethome"));
        commands.put("delhome", Lists.newArrayList("delhome"));
        commands.put("home", Lists.newArrayList("home"));

        // Rules
        commands.put("rules", Lists.newArrayList("rules"));
        commands.put("addrule", Lists.newArrayList("addrule"));
        commands.put("delrule", Lists.newArrayList("delrule"));
    }

    List<Class<? extends BaseCommand>> commandClassList   = Lists.newArrayList();
    List<Class<? extends CommandBase>> otherClassCommands = Lists.newArrayList();

    List<BaseCommand>                  commandList        = Lists.newArrayList();

    @SuppressWarnings("unchecked")
    public CommandManager()
    {
        Set<String> blacklist = Sets.newHashSet(ConfigManager.INSTANCE.disabledCommands);
        List<Class<?>> foundClasses;

        for (String s : ConfigManager.INSTANCE.commands)
        {
            String[] args = s.split(":");
            List<String> aliases = Lists.newArrayList();
            for (String s1 : args)
                aliases.add(s1);
            LinkedHashSet<String> names = Sets.newLinkedHashSet(aliases);
            aliases = Lists.newArrayList(names);
            commands.put(args[0], aliases);
        }

        // Register commands.
        try
        {
            foundClasses = ClassFinder.find(CommandManager.class.getPackage().getName());
            List<String> classNames = Lists.newArrayList();
            for (Class<?> candidateClass : foundClasses)
            {
                if (CommandBase.class.isAssignableFrom(candidateClass) && candidateClass.getEnclosingClass() == null)
                {
                    classNames.add(candidateClass.getName());
                    // Skip blacklisted commands.
                    if (blacklist.contains(candidateClass.getName()))
                    {
                        if (ConfigManager.INSTANCE.comandDisableSpam)
                            System.out.println("Skipping Blacklisted " + candidateClass);
                        continue;
                    }
                    // Skip disabled land commands.
                    if (!ConfigManager.INSTANCE.landEnabled
                            && candidateClass.getName().startsWith("thut.essentials.commands.land"))
                    {
                        if (ConfigManager.INSTANCE.comandDisableSpam)
                            System.out.println("Skipping Disabled " + candidateClass);
                        continue;
                    }
                    try
                    {
                        if (BaseCommand.class.isAssignableFrom(candidateClass))
                            commandClassList.add((Class<? extends BaseCommand>) candidateClass);
                        else otherClassCommands.add((Class<? extends CommandBase>) candidateClass);
                    }
                    catch (Exception e)
                    {
                        if (ConfigManager.INSTANCE.comandDisableSpam)
                            System.out.println("Error with " + candidateClass);
                    }
                }
            }
            Collections.sort(classNames);
            ConfigManager.INSTANCE.updateField(ConfigManager.class.getDeclaredField("allThutEssentialsCommands"),
                    classNames.toArray(new String[0]));
            ConfigManager.INSTANCE.save();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void registerCommands(FMLServerStartingEvent event)
    {
        ConfigManager.INSTANCE.save();
        System.out.println(commandClassList);
        System.out.println(otherClassCommands);
        for (Class<? extends BaseCommand> clazz : commandClassList)
        {
            try
            {
                BaseCommand command = clazz.newInstance();
                if (command != null && command.getName() != null)
                {
                    commandList.add(command);
                    event.registerServerCommand(command);
                }
                else
                {
                    if (ConfigManager.INSTANCE.comandDisableSpam) System.err.println(command + " is not completed.");
                }
            }
            catch (Exception e)
            {
                if (ConfigManager.INSTANCE.comandDisableSpam) System.out.println("Error with " + clazz);
            }
        }
        for (Class<? extends CommandBase> clazz : otherClassCommands)
        {
            try
            {
                CommandBase command = clazz.newInstance();
                if (command != null && command.getName() != null)
                {
                    event.registerServerCommand(command);
                }
                else
                {
                    if (ConfigManager.INSTANCE.comandDisableSpam) System.err.println(command + " is not completed.");
                }
            }
            catch (Exception e)
            {
                if (ConfigManager.INSTANCE.comandDisableSpam) System.out.println("Error with " + clazz);
            }
        }
        ConfigManager.INSTANCE.commands = new String[commandList.size()];
        Collections.sort(commandList);
        for (int i = 0; i < commandList.size(); i++)
        {
            BaseCommand command = commandList.get(i);
            List<String> alii = commands.get(command.getName());
            String var = command.key;
            for (int j = 0; j < alii.size(); j++)
            {
                var = var + ":" + alii.get(j);
            }
            ConfigManager.INSTANCE.commands[i] = var;
        }
        try
        {
            ConfigManager.INSTANCE.updateField(ConfigManager.class.getDeclaredField("commands"),
                    ConfigManager.INSTANCE.commands);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        for (Class<? extends CommandBase> c : otherClassCommands)
        {
            if (ConfigManager.INSTANCE.comandDisableSpam)
                System.err.println(c + " is a commandbase instead of basecomand.");
        }
    }

    public void clear()
    {
        for (BaseCommand c : commandList)
        {
            c.destroy();
        }
    }
}
