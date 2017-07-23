package thut.essentials.land;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraftforge.fml.common.FMLCommonHandler;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.Coordinate;

public class LandSaveHandler
{

    static ExclusionStrategy exclusion = new ExclusionStrategy()
    {
        @Override
        public boolean shouldSkipField(FieldAttributes f)
        {
            String name = f.getName();
            return name.equals("landMap") || name.equals("teamMap") || name.equals("playerTeams")
                    || name.equals("publicBlocks") || name.equals("ranksMembers");
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz)
        {
            return false;
        }
    };

    public static File getGlobalFolder()
    {
        String folder = FMLCommonHandler.instance().getMinecraftServerInstance().getFolderName();
        File file = FMLCommonHandler.instance().getSavesDirectory();
        File saveFolder = new File(file, folder);
        File teamsFolder = new File(saveFolder, "land");
        if (!teamsFolder.exists()) teamsFolder.mkdirs();
        return teamsFolder;
    }

    public static File getTeamFolder()
    {
        File teamFolder = new File(getGlobalFolder(), "teams");
        if (!teamFolder.exists()) teamFolder.mkdirs();
        return teamFolder;
    }

    public static void saveGlobalData()
    {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) return;
        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(exclusion).setPrettyPrinting().create();
        LandManager.getInstance().version = LandManager.VERSION;
        String json = gson.toJson(LandManager.getInstance());
        File teamsFile = new File(getGlobalFolder(), "landData.json");
        try
        {
            FileUtils.writeStringToFile(teamsFile, json, "UTF-8");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void loadGlobalData()
    {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) return;
        File teamsFile = new File(getGlobalFolder(), "landData.json");
        if (teamsFile.exists())
        {
            try
            {
                Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(exclusion).setPrettyPrinting()
                        .create();
                String json = FileUtils.readFileToString(teamsFile, "UTF-8");
                LandManager.instance = gson.fromJson(json, LandManager.class);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            if (LandManager.instance == null) LandManager.instance = new LandManager();
            loadTeams();
        }
        else
        {
            if (LandManager.instance == null) LandManager.instance = new LandManager();
            saveGlobalData();
        }
    }

    private static void loadTeams()
    {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) return;
        File folder = getTeamFolder();
        for (File file : folder.listFiles())
        {
            try
            {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = FileUtils.readFileToString(file, "UTF-8");
                LandTeam team = gson.fromJson(json, LandTeam.class);
                LandManager.getInstance().teamMap.put(team.teamName, team);
                team.init(FMLCommonHandler.instance().getMinecraftServerInstance());
                for (Coordinate land : team.land.land)
                    LandManager.getInstance().addTeamLand(team.teamName, land, false);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void saveTeam(String team)
    {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) return;
        File folder = getTeamFolder();
        File teamFile = new File(folder, team + ".json");
        LandTeam land;
        if ((land = LandManager.getInstance().getTeam(team, false)) != null)
        {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(land);
            try
            {
                FileUtils.writeStringToFile(teamFile, json, "UTF-8");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void deleteTeam(String team)
    {
        File folder = getTeamFolder();
        File teamFile = new File(folder, team + ".json");
        if (teamFile.exists()) teamFile.delete();
    }

}
