package thut.essentials.land;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.command.CommandException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.Coordinate;

public class LandManager
{

    public static class Invites
    {
        public Set<String> teams = Sets.newHashSet();
    }

    public static class LandTeam
    {
        public static final String EDITMESSAGES   = "editMessages";
        public static final String CLAIMPERM      = "claim";
        public static final String UNCLAIMPERM    = "unclaim";
        public static final String SETPREFIX      = "prefix";
        public static final String SETHOME        = "sethome";
        public static final String INVITE         = "invite";
        public static final String KICK           = "kick";

        public TeamLand            land           = new TeamLand();
        public String              teamName;
        public Set<UUID>           admin          = Sets.newHashSet();
        public Set<UUID>           member         = Sets.newHashSet();
        public Map<UUID, Rank>     ranksMembers   = Maps.newHashMap();
        public Map<String, Rank>   rankMap        = Maps.newHashMap();
        public Set<Coordinate>     anyUse         = Sets.newHashSet();
        public Coordinate          home;
        public String              exitMessage    = "";
        public String              enterMessage   = "";
        public String              denyMessage    = "";
        public String              prefix         = "";
        public boolean             reserved       = false;
        public boolean             players        = false;
        public boolean             noPlayerDamage = false;
        public boolean             noMobSpawn     = false;
        public boolean             friendlyFire   = true;
        public boolean             noExplosions   = false;

        // TODO figure out what I want to do with these two.
        public List<String>        allies         = Lists.newArrayList();
        public List<String>        enemies        = Lists.newArrayList();

        public LandTeam()
        {
        }

        public LandTeam(String name)
        {
            teamName = name;
        }

        public boolean isMember(Entity player)
        {
            return member.contains(player.getUniqueID());
        }

        public boolean isAdmin(Entity player)
        {
            return admin.contains(player.getUniqueID());
        }

        public boolean hasPerm(UUID player, String perm)
        {
            if (admin.contains(player)) return true;
            Rank rank = ranksMembers.get(player);
            if (rank == null) return false;
            return rank.perms.contains(perm);
        }

        public void setPerm(String rankName, String perm)
        {
            Rank rank = rankMap.get(rankName);
            if (rank != null) rank.perms.add(perm);
        }

        public void unsetPerm(String rankName, String perm)
        {
            Rank rank = rankMap.get(rankName);
            if (rank != null) rank.perms.remove(perm);
        }

        public void init(MinecraftServer server)
        {
            Set<UUID> members = Sets.newHashSet(member);
            if (!teamName.equals(ConfigManager.INSTANCE.defaultTeamName))
            {
                for (UUID id : members)
                    LandManager.getInstance().playerTeams.put(id, this);
                for (Coordinate c : anyUse)
                    LandManager.getInstance().publicBlocks.put(c, this);
            }
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof LandTeam) { return ((LandTeam) o).teamName.equals(teamName); }
            return false;
        }

        @Override
        public int hashCode()
        {
            return teamName.hashCode();
        }

        public static class Rank
        {
            public Set<UUID>   members = Sets.newHashSet();
            public String      prefix;
            public Set<String> perms   = Sets.newHashSet();
        }
    }

    public static class TeamLand
    {
        public HashSet<Coordinate> land = Sets.newHashSet();

        public boolean addLand(Coordinate land)
        {
            return this.land.add(land);
        }

        public int countLand()
        {
            return land.size();
        }

        public boolean removeLand(Coordinate land)
        {
            return this.land.remove(land);
        }
    }

    static LandManager      instance;

    public static final int VERSION = 1;

    public static void clearInstance()
    {
        if (instance != null)
        {
            LandSaveHandler.saveGlobalData();
            for (String s : instance.teamMap.keySet())
                LandSaveHandler.saveTeam(s);
        }
        instance = null;
    }

    public static LandManager getInstance()
    {
        if (instance == null)
        {
            LandSaveHandler.loadGlobalData();
        }
        return instance;
    }

    public static LandTeam getTeam(Entity player)
    {
        LandTeam playerTeam = getInstance().playerTeams.get(player.getUniqueID());
        if (playerTeam == null)
        {
            for (LandTeam team : getInstance().teamMap.values())
            {
                if (team.isMember(player))
                {
                    getInstance().addToTeam(player.getUniqueID(), team.teamName);
                    playerTeam = team;
                    break;
                }
            }
            if (playerTeam == null && player instanceof EntityPlayer)
            {
                getInstance().addToTeam(player.getUniqueID(), ConfigManager.INSTANCE.defaultTeamName);
                playerTeam = getInstance().getTeam(ConfigManager.INSTANCE.defaultTeamName, false);
            }
        }
        return playerTeam;
    }

    public static LandTeam getDefaultTeam()
    {
        return getInstance().getTeam(ConfigManager.INSTANCE.defaultTeamName, true);
    }

    public static boolean owns(Entity player, Coordinate chunk)
    {
        return getTeam(player).equals(getInstance().getLandOwner(chunk));
    }

    protected HashMap<String, LandTeam>     teamMap      = Maps.newHashMap();
    protected HashMap<Coordinate, LandTeam> landMap      = Maps.newHashMap();
    protected HashMap<UUID, LandTeam>       playerTeams  = Maps.newHashMap();
    protected HashMap<UUID, Invites>        invites      = Maps.newHashMap();
    protected HashMap<Coordinate, LandTeam> publicBlocks = Maps.newHashMap();
    public int                              version      = VERSION;

    LandManager()
    {
    }

    public void renameTeam(String oldName, String newName) throws CommandException
    {
        if (teamMap.containsKey(newName)) throw new CommandException("Error, new team name already in use");
        LandTeam team = teamMap.remove(oldName);
        if (team == null) throw new CommandException("Error, specified team not found");
        teamMap.put(newName, team);
        for (Invites i : invites.values())
        {
            if (i.teams.remove(oldName))
            {
                i.teams.add(newName);
            }
        }
        team.teamName = newName;
        LandSaveHandler.saveTeam(newName);
        LandSaveHandler.deleteTeam(oldName);
    }

    public void removeTeam(String teamName)
    {
        LandTeam team = teamMap.remove(teamName);
        HashSet<Coordinate> land = Sets.newHashSet(landMap.keySet());
        for (Coordinate c : land)
        {
            if (landMap.get(c).equals(team))
            {
                landMap.remove(c);
            }
        }
        HashSet<UUID> ids = Sets.newHashSet(playerTeams.keySet());
        for (UUID id : ids)
        {
            if (playerTeams.get(id).equals(team))
            {
                playerTeams.remove(id);
            }
        }
        for (Invites i : invites.values())
        {
            i.teams.remove(teamName);
        }
        LandSaveHandler.deleteTeam(teamName);
    }

    public void addTeamLand(String team, Coordinate land, boolean sync)
    {
        LandTeam t = teamMap.get(team);
        if (t == null)
        {
            Thread.dumpStack();
            return;
        }
        t.land.addLand(land);
        landMap.put(land, t);
        for (LandTeam t1 : teamMap.values())
        {
            if (t != t1) t1.land.removeLand(land);
        }
        if (sync)
        {
            LandSaveHandler.saveTeam(team);
        }
    }

    public void addAdmin(UUID admin, String team)
    {
        LandTeam t = getTeam(team, true);
        t.admin.add(admin);
        LandSaveHandler.saveTeam(team);
    }

    public void addToTeam(UUID member, String team)
    {
        LandTeam t = getTeam(team, true);
        if (t.admin.isEmpty() && !t.teamName.equals(ConfigManager.INSTANCE.defaultTeamName))
        {
            t.admin.add(member);
        }
        if (playerTeams.containsKey(member))
        {
            LandTeam old = playerTeams.remove(member);
            old.member.remove(member);
            old.admin.remove(member);
            LandSaveHandler.saveTeam(old.teamName);
        }
        t.member.add(member);
        playerTeams.put(member, t);
        Invites invite = invites.get(member);
        if (invite != null)
        {
            invite.teams.remove(team);
        }
        LandSaveHandler.saveTeam(team);
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        try
        {
            EntityPlayer player = server.getPlayerList().getPlayerByUUID(member);
            if (player != null)
            {
                player.refreshDisplayName();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public int countLand(String team)
    {
        LandTeam t = teamMap.get(team);
        if (t != null) { return t.land.countLand(); }
        return 0;
    }

    public void createTeam(UUID member, String team) throws CommandException
    {
        if (teamMap.containsKey(team)) throw new CommandException(team + " already exists!");
        getTeam(team, true);
        addToTeam(member, team);
        addAdmin(member, team);
    }

    public List<String> getInvites(UUID member)
    {
        List<String> ret = new ArrayList<String>();
        Invites invite = invites.get(member);
        if (invite == null) return ret;
        return Lists.newArrayList(invite.teams);
    }

    public LandTeam getLandOwner(Coordinate land)
    {
        return landMap.get(land);
    }

    public LandTeam getTeam(String name, boolean create)
    {
        LandTeam team = teamMap.get(name);
        if (team == null && create)
        {
            team = new LandTeam(name);
            teamMap.put(name, team);
        }
        return team;
    }

    public List<Coordinate> getTeamLand(String team)
    {
        ArrayList<Coordinate> ret = new ArrayList<Coordinate>();
        LandTeam t = teamMap.get(team);
        if (t != null) ret.addAll(t.land.land);
        return ret;
    }

    public boolean hasInvite(UUID member, String team)
    {
        Invites invite = invites.get(member);
        if (invite != null) return invite.teams.contains(team);
        return false;
    }

    public boolean invite(UUID inviter, UUID invitee)
    {
        if (!isAdmin(inviter)) return false;
        String team = playerTeams.get(inviter).teamName;
        if (hasInvite(invitee, team)) return false;
        Invites invite = invites.get(invitee);
        if (invite == null)
        {
            invite = new Invites();
            invites.put(invitee, invite);
        }
        invite.teams.add(team);
        return true;
    }

    public boolean isAdmin(UUID member)
    {
        LandTeam team = playerTeams.get(member);
        if (team == null) return false;
        return team.admin.contains(member);
    }

    public boolean isOwned(Coordinate land)
    {
        return landMap.containsKey(land);
    }

    public boolean isPublic(Coordinate c)
    {
        return publicBlocks.containsKey(c);
    }

    public boolean isTeamLand(Coordinate chunk, String team)
    {
        LandTeam t = teamMap.get(team);
        if (t != null) return t.land.land.contains(chunk);
        return false;
    }

    public void removeAdmin(UUID member)
    {
        LandTeam t = playerTeams.get(member);
        if (t != null)
        {
            t.admin.remove(member);
        }
    }

    public void removeFromInvites(UUID member, String team)
    {
        Invites invite = invites.get(member);
        if (invite != null && invite.teams.contains(team))
        {
            invite.teams.remove(team);
            LandSaveHandler.saveGlobalData();
        }
    }

    public void removeFromTeam(UUID member)
    {
        LandTeam team = playerTeams.get(member);
        if (team != null)
        {
            team.admin.remove(member);
            team.member.remove(member);
            playerTeams.remove(member);
        }
    }

    public void removeTeamLand(String team, Coordinate land)
    {
        LandTeam t = teamMap.get(team);
        landMap.remove(land);
        if (t != null && t.land.removeLand(land))
        {
            LandSaveHandler.saveTeam(team);
        }
    }

    public void setPublic(Coordinate c, LandTeam owner)
    {
        publicBlocks.put(c, owner);
        owner.anyUse.add(c);
        LandSaveHandler.saveGlobalData();
    }

    public void unsetPublic(Coordinate c)
    {
        if (!publicBlocks.containsKey(c)) return;
        LandTeam team;
        (team = publicBlocks.remove(c)).anyUse.remove(c);
        LandSaveHandler.saveTeam(team.teamName);
        LandSaveHandler.saveGlobalData();
    }
}
