package com.oneshot;

import com.oneshot.modules.DiscordWebhook;
import com.oneshot.modules.HCIMScout;
import com.oneshot.utils.Constants;
import com.oneshot.utils.Icons;

import com.google.inject.Provides;

import net.runelite.api.clan.*;
import net.runelite.api.Client;
import net.runelite.api.events.*;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.GameState;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

import javax.inject.Inject;
import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;

@PluginDescriptor(
	name = Constants.PLUGIN_NAME
)

public class OneShotPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(OneShotPlugin.class);

    Dotenv dotenv = Dotenv.load();
    String recruiter = dotenv.get("WEBHOOK_RECRUITER");
    String pinkguard = dotenv.get("WEBHOOK_PINKGUARD");
    String frontman = dotenv.get("WEBHOOK_FRONTMAN");

    @Inject
    private HCIMScout hcimScout;

    @Inject
	private Client client;

    @Inject
    private OneShotConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private  ChatIconManager chatIconManager;

    @Inject
    private DrawManager drawManager;

    private NavigationButton navButton;
    private OneShotPanel panel;

    @Provides
    OneShotConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OneShotConfig.class);
    }

	@Override
	protected void startUp() throws Exception
	{
        log.debug("Startup");
        //hcimScout.init();
        panel = injector.getInstance(OneShotPanel.class);
        panel.init();

        navButton = NavigationButton.builder()
                .tooltip(Constants.PLUGIN_NAME)
                .icon(Icons.RED_HELM_IMAGE)
                .priority(Constants.DEFAULT_PRIORITY)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

	}

	@Override
	protected void shutDown() throws Exception
	{
        log.debug("Shutdown");
        //hcimScout.deinit();
        panel.deinit();
        clientToolbar.removeNavigation(navButton);
        panel = null;
        navButton = null;
	}

    @Subscribe
    public void onClanMemberJoined(ClanMemberJoined clanMemberJoined) throws IOException, InterruptedException {
        panel.refresh(getAllMembersInfo(), getMembersIcons(), getMembersDisplayName());
    }

    @Subscribe
    public void onClanMemberLeft(ClanMemberLeft clanMemberLeft) throws IOException, InterruptedException {
        panel.refresh(getAllMembersInfo(), getMembersIcons(), getMembersDisplayName());
    }

    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged clanChannelChanged)
    {
        if (clanChannelChanged.getClanChannel() != null) {
            panel.buildIntroPanel();
            String clanName = clanChannelChanged.getClanChannel().getName();
            if (isOneShotMember(clanName) && !clanChannelChanged.isGuest()) { //clan member
                String playerName = client.getLocalPlayer().getName();
                ClanSettings clanSettings = client.getClanSettings();
                assert clanSettings != null;
                ClanRank clanRank = Objects.requireNonNull(clanSettings.findMember(playerName)).getRank();
                ClanTitle clanTitle = Objects.requireNonNull(clanSettings.titleForRank(clanRank));
                log.debug("onClanChannelChanged: " + clanName + " | " + clanRank + " | " + clanTitle);
                panel.buildMainPanel(
                        isModerator(clanRank),
                        playerName,
                        clanTitle.getName(),
                        getRankIcon(clanTitle),
                        getAllMembersInfo(),
                        getMembersIcons(),
                        getMembersDisplayName()
                );
            } else if (isOneShotMember(clanName)) { // guest
                panel.changeIntroText1("You are currently a guest of One Shot", Color.RED);
                panel.changeIntroText2("Become a member today!", Color.RED);
            } else { //not in clan chat
                panel.changeIntroText1("You are not part of One Shot CC", Color.RED);
                panel.changeIntroText2("You don't have access, sorry", Color.RED);
            }
        }
        else
        {
            panel.buildIntroPanel();
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged varbitChanged)
    {
        if (varbitChanged.getVarbitId() == VarbitID.IRONMAN_HARDCORE_DEAD) {
            log.debug("again??");
            // TODO: works, needs logic
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) throws IOException {
        String chatMessage = event.getMessage();

        if (Objects.equals(chatMessage, "!Oneshot"))
        {
            log.debug("sending test");
            send_test();
        }
    }

    public void send_test() throws IOException {

        DiscordWebhook webhook = new DiscordWebhook(recruiter);
        webhook.setContent("Example of a more complete message");
        //webhook.setAvatarUrl("https://your.awesome/image.png");
        //webhook.setUsername("Custom Usernames!");
        //webhook.setTts(true);
        webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle("This is a title")
                .setDescription("This is a description")
                .setColor(Color.orange)
                .addField("1st Field", "Inline", true)
                .addField("2nd Field", "Inline", true)
                .addField("3rd Field", "No-Inline", false)
                //.setThumbnail("https://www.startpage.com/av/proxy-image?piurl=https%3A%2F%2Ftse3.mm.bing.net%2Fth%2Fid%2FOIP.xSKYQL7JlUvU_pwJQ-SYvQHaH9%3Fpid%3DApi&sp=1764141354T0d43d84f6dfdef6ec0729940c683d35427d7f6838db9e29e824684a447ddc0eb")
                .setFooter("footer here", "https://www.startpage.com/av/proxy-image?piurl=https%3A%2F%2Ftse3.mm.bing.net%2Fth%2Fid%2FOIP.rCnWNfwAmci58C7v70NvzwAAAA%3Fpid%3DApi&sp=1764141956Td9fb27be749e755b5a2fab8d9a2a6c8aa64640d038425589c8891fc9ad4327e5")
                .setImage("https://www.startpage.com/av/proxy-image?piurl=https%3A%2F%2Fi.ytimg.com%2Fvi%2FH6-8Kquboko%2Fmaxresdefault.jpg&sp=1764141714Ta7ccda0322031e24ca5d8368ce8d01f769acb7532db0c925a2786fecb297f6fe")
                .setAuthor("player here with clan icon", "", "https://oldschool.runescape.wiki/images/Clan_icon_-_Captain.png?b0561")
                //.setUrl("https://kryptongta.com")
                 );
//        webhook.addEmbed(new DiscordWebhook.EmbedObject()
//                .setDescription("Just another added embed object!"));
        webhook.execute(); //Handle exception

    }

    @Subscribe
    public void onGameTick(final GameTick event)
    {
        //hcimScout.gameTick();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN){
            panel.buildIntroPanel();
        }
    }

    public ImageIcon getRankIcon(ClanTitle clanTitle)
    {
        BufferedImage chatIcon = chatIconManager.getRankImage(clanTitle);
        assert chatIcon != null;
        return new ImageIcon(chatIcon.getScaledInstance(Constants.TEXT_ICON_SIZE, Constants.TEXT_ICON_SIZE, Image.SCALE_DEFAULT));
    }

    private static boolean isModerator(ClanRank clanRank)
    {
        return Arrays.asList(Constants.RANK_OWNER, Constants.RANK_DEPUTY_OWNER, Constants.RANK_ASTRAL,
                Constants.RANK_CAPTAIN).contains(clanRank);
    }

    private static boolean isOneShotMember(String clanName)
    {
        return Objects.equals(clanName, Constants.CLAN_NAME);
    }

    private ArrayList<OneShotMember> getAllMembersInfo() {

        ArrayList<OneShotMember> oneShotMembers = new ArrayList<OneShotMember>();
        ArrayList<Integer> tmpIndexList = new ArrayList<Integer>();

        // checks all members offline and online
        ClanSettings clanSettings = client.getClanSettings();
        assert clanSettings != null;
        java.util.List<ClanMember> clanMembers = clanSettings.getMembers();

        for (ClanMember clanMember : clanMembers)
        {
            ClanRank clanRank = clanMember.getRank();
            int index = clanRank.getRank();
            if (!tmpIndexList.contains(index))
            {
                tmpIndexList.add(index);
                ClanTitle clanTitle = clanSettings.titleForRank(clanRank);
                OneShotMember oneShotMember = new OneShotMember(index, clanTitle);
                oneShotMember.addTotal();
                oneShotMembers.add(oneShotMember);
            }
            else
            {
                for (OneShotMember oneShotMember : oneShotMembers)
                {
                    if (oneShotMember.index == index)
                    {
                        oneShotMember.addTotal();
                    }
                }
            }
        }

        // checks for online members only
        ClanChannel clanChannel = client.getClanChannel();
        assert clanChannel != null;
        List<ClanChannelMember> clanChannelMembers = clanChannel.getMembers();

        for (ClanChannelMember clanChannelMember : clanChannelMembers)
        {
            int index = clanChannelMember.getRank().getRank();
            for (OneShotMember oneShotMember : oneShotMembers)
            {
                if (oneShotMember.index == index)
                {
                    oneShotMember.addOnline();
                }
            }
        }

        oneShotMembers.sort(Comparator.comparing(OneShotMember::getIndex).reversed());

        return oneShotMembers;
    }

    private Map<String, ImageIcon> getMembersIcons()
    {
        Map<String, ImageIcon> Members = new HashMap<String, ImageIcon>();

        ClanSettings clanSettings = client.getClanSettings();
        assert clanSettings != null;
        java.util.List<ClanMember> clanMembers = clanSettings.getMembers();

        for (ClanMember clanMember : clanMembers)
        {
            String memberName = clanMember.getName();
//            log.debug(memberName.toLowerCase().replace(" ", " ") + ": " + convertToHexString(memberName.toLowerCase().replace(" ", " ").getBytes()));
            ImageIcon icon = getRankIcon(clanSettings.titleForRank(clanMember.getRank()));
            Members.put(memberName.toLowerCase().replace(" ", " "), icon);
        }
        return Members;
    }

    private Map<String, String> getMembersDisplayName()
    {
        Map<String, String> Members = new HashMap<String, String>();

        ClanSettings clanSettings = client.getClanSettings();
        assert clanSettings != null;
        java.util.List<ClanMember> clanMembers = clanSettings.getMembers();

        for (ClanMember clanMember : clanMembers)
        {
            String memberName = clanMember.getName();
//            log.debug(memberName.toLowerCase().replace(" ", " ") + ": " + convertToHexString(memberName.toLowerCase().replace(" ", " ").getBytes()));
            Members.put(memberName.toLowerCase().replace(" ", " "), memberName);
        }
        return Members;
    }

    public class OneShotMember {
        int index;
        ImageIcon icon;
        String name;
        int online = 0;
        int total = 0;

        public OneShotMember(int index, ClanTitle clanTitle)
        {
            this.index = index;
            this.icon = getRankIcon(clanTitle);
            this.name = clanTitle.getName();
        }

        public void addOnline()
        {
            this.online++;
        }

        public void addTotal()
        {
            this.total++;
        }

        public String toString()
        {
            return name + " | " + online + " | " + total;
        }

        public int getIndex()
        {
            return index;
        }

        public ImageIcon getIcon()
        {
            return icon;
        }

        public String getName()
        {
            return name;
        }

        public Object[] getRank()
        {
            return new Object[]{icon, name};
        }

        public int getOnline()
        {
            return online;
        }

        public int getTotal()
        {
            return total;
        }
    }

    public static String convertToHexString(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int nibble = (data[i] >>> 4) & 0x0F;
            int two_nibbles = 0;
            do {
                if ((0 <= nibble) && (nibble <= 9))
                    buf.append((char) ('0' + nibble));
                else
                    buf.append((char) ('a' + (nibble - 10)));

                nibble = data[i] & 0x0F;
            } while (two_nibbles++ < 1);
        }
        return buf.toString();
    }

}

