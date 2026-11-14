package com.khoinhan.khoinhanjs;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.attribute.IInviteContainer;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.managers.RoleManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discord binding cho KubeJS (JDA 5.x).
 *
 * Config (configMap):
 *   token            (bat buoc)
 *   channels         map key -> channelId
 *   sanitizeMessages mac dinh true
 *   guildId          (tuy chon) server mac dinh cho slash command / role / kick / ban...
 *   enableMembers    (tuy chon, mac dinh false) bat intent GUILD_MEMBERS + cache member
 *                    (phai bat "Server Members Intent" trong Discord Developer Portal)
 *   status           (tuy chon) online | idle | dnd | invisible
 *   activity         (tuy chon) text hien thi duoi ten bot
 *
 * Luu y: moi callback chay tren thread cua JDA, KHONG phai server thread cua Minecraft.
 * Neu callback can thay doi world/player thi hay dua vao server.execute(...) trong script.
 */
public class DiscordBinding {
    private JDA jda;
    private final String token;
    private final String guildId;
    private final boolean enableMembers;
    private final Map<String, String> channels = new HashMap<>();
    private final boolean sanitizeMessages;

    private final List<Consumer<DiscordMessage>> messageCallbacks = new CopyOnWriteArrayList<>();
    private volatile Consumer<DiscordButton> buttonCallback;
    private volatile Consumer<DiscordSelect> selectCallback;
    private volatile Consumer<DiscordCommand> commandCallback;
    private volatile Consumer<DiscordAutocomplete> autocompleteCallback;
    private volatile Consumer<DiscordModal> modalCallback;
    private volatile Consumer<Map<String, Object>> reactionAddCallback;
    private volatile Consumer<Map<String, Object>> reactionRemoveCallback;
    private volatile Consumer<Map<String, Object>> messageDeleteCallback;
    private volatile Consumer<Map<String, Object>> messageEditCallback;
    private volatile Consumer<Map<String, Object>> memberJoinCallback;
    private volatile Consumer<Map<String, Object>> memberLeaveCallback;
    private volatile Consumer<Map<String, Object>> voiceCallback;

    @SuppressWarnings("unchecked")
    public DiscordBinding(Map<String, Object> configMap) {
        Object tokenObj = configMap.get("token");
        this.token = tokenObj == null ? null : String.valueOf(tokenObj);
        if (this.token == null || this.token.isBlank()) {
            throw new IllegalArgumentException("Discord token is required");
        }

        Object rawChannels = configMap.get("channels");
        if (rawChannels instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    this.channels.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
        }

        this.sanitizeMessages = !Boolean.FALSE.equals(configMap.get("sanitizeMessages"));
        this.guildId = configMap.get("guildId") == null ? null : String.valueOf(configMap.get("guildId"));
        this.enableMembers = Boolean.TRUE.equals(configMap.get("enableMembers"));

        initialize(configMap);
    }

    private void initialize(Map<String, Object> configMap) {
        try {
            EnumSet<GatewayIntent> intents = EnumSet.of(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT);
            if (enableMembers) intents.add(GatewayIntent.GUILD_MEMBERS);

            // Chan @everyone/@here/role/user ping tu noi dung do script gui di.
            if (sanitizeMessages) {
                MessageRequest.setDefaultMentions(EnumSet.of(
                        Message.MentionType.CHANNEL,
                        Message.MentionType.EMOJI,
                        Message.MentionType.SLASH_COMMAND));
            }

            JDABuilder builder = JDABuilder.createDefault(token)
                    .enableIntents(intents)
                    .addEventListeners(new MessageListener());

            if (enableMembers) {
                builder.setMemberCachePolicy(MemberCachePolicy.ALL)
                        .setChunkingFilter(ChunkingFilter.ALL);
            }

            OnlineStatus status = parseStatus(configMap.get("status"));
            if (status != null) builder.setStatus(status);
            if (configMap.get("activity") != null) {
                builder.setActivity(Activity.playing(String.valueOf(configMap.get("activity"))));
            }

            this.jda = builder.build();
            this.jda.awaitReady();
            KhoinhanJS.LOGGER.info("Discord bot connected successfully as " + jda.getSelfUser().getName());
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to initialize Discord bot: " + e.getMessage(), e);
            throw new RuntimeException("Discord bot initialization failed", e);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static OnlineStatus parseStatus(Object value) {
        if (value == null) return null;
        OnlineStatus status = OnlineStatus.fromKey(String.valueOf(value).toLowerCase());
        return status == OnlineStatus.UNKNOWN ? null : status;
    }

    private static Consumer<Throwable> logFailure(String what) {
        return err -> KhoinhanJS.LOGGER.error("Discord: failed to " + what + ": " + err.getMessage());
    }

    /** channelKey la key trong config.channels, hoac truc tiep la ID cua kenh. */
    private MessageChannel resolveChannel(String keyOrId) {
        if (keyOrId == null) return null;
        String id = channels.getOrDefault(keyOrId, keyOrId);
        if (!id.matches("\\d{15,25}")) {
            KhoinhanJS.LOGGER.warn("Channel key '" + keyOrId + "' not found in configuration");
            return null;
        }
        MessageChannel channel = jda.getChannelById(MessageChannel.class, id);
        if (channel == null) {
            KhoinhanJS.LOGGER.warn("Discord channel not found: " + id);
        }
        return channel;
    }

    private Guild getGuild() {
        if (guildId != null) return jda.getGuildById(guildId);
        List<Guild> guilds = jda.getGuilds();
        return guilds.isEmpty() ? null : guilds.get(0);
    }

    public String sanitizeMessage(String message) {
        return sanitizeMessage(message, 2000);
    }

    public String sanitizeMessage(String message, int maxLength) {
        if (message == null) return "";
        String sanitized = message
                .replace("@everyone", "@\u200Beveryone")
                .replace("@here", "@\u200Bhere")
                .replaceAll("<@&\\d+>", "@\u200Brole")
                .replaceAll("`{3,}", "`\u200B`\u200B`");

        Pattern userMentionPattern = Pattern.compile("<@!?(\\d+)>");
        Matcher matcher = userMentionPattern.matcher(sanitized);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String userId = matcher.group(1);
            String replacement = "@\u200Buser";
            try {
                var user = jda.getUserById(userId);
                if (user != null) {
                    replacement = "@\u200B" + user.getName();
                }
            } catch (Exception ignored) {}
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        sanitized = sb.toString();

        sanitized = sanitized.replaceAll("<#\\d+>", "#\u200Bchannel");
        return truncate(sanitized, maxLength);
    }

    private String prepareText(String message) {
        return sanitizeMessages ? sanitizeMessage(message) : truncate(message, 2000);
    }

    // ------------------------------------------------------------------
    // Embed / Button / Dropdown / Modal builders
    // ------------------------------------------------------------------

    private static Integer parseColor(Object color) {
        if (color instanceof Number n) return n.intValue();
        if (color instanceof String s) {
            try {
                String hex = s.trim().replace("#", "").replace("0x", "");
                return Integer.parseInt(hex, 16);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * Emoji co the la:
     *  - chuoi: "🛒" hoac "<:ten:id>" / "<a:ten:id>" (emoji server)
     *  - object: {name: "🛒"} hoac {name: "ten", id: "123...", animated: true} (dang cua Discord API)
     */
    static Emoji parseEmoji(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof Map<?, ?> map) {
                Object name = map.get("name");
                if (name == null) return null;
                Object id = map.get("id");
                if (id != null && !String.valueOf(id).isBlank()) {
                    return Emoji.fromCustom(String.valueOf(name), Long.parseLong(String.valueOf(id)),
                            Boolean.TRUE.equals(map.get("animated")));
                }
                return Emoji.fromUnicode(String.valueOf(name));
            }
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : Emoji.fromFormatted(text);
        } catch (Exception e) {
            KhoinhanJS.LOGGER.warn("Invalid Discord emoji: " + value);
            return null;
        }
    }

    /**
     * embedData: title, url, description, color (number hoac "#RRGGBB"), footer, footerIcon,
     * author, authorUrl, authorIcon, thumbnail, image, timestamp (true), fields:[{name,value,inline}]
     */
    MessageEmbed buildEmbed(Map<String, Object> embedData) {
        EmbedBuilder builder = new EmbedBuilder();
        if (embedData.get("title") != null) {
            String title = truncate(String.valueOf(embedData.get("title")), 256);
            String url = embedData.get("url") != null ? String.valueOf(embedData.get("url")) : null;
            builder.setTitle(title, url);
        }
        if (embedData.get("description") != null) {
            String desc = String.valueOf(embedData.get("description"));
            builder.setDescription(sanitizeMessages ? sanitizeMessage(desc, 4096) : truncate(desc, 4096));
        }
        Integer color = parseColor(embedData.get("color"));
        if (color != null) builder.setColor(color);

        if (embedData.get("footer") != null) {
            String icon = embedData.get("footerIcon") != null ? String.valueOf(embedData.get("footerIcon")) : null;
            builder.setFooter(truncate(String.valueOf(embedData.get("footer")), 2048), icon);
        }
        if (embedData.get("author") != null) {
            String url = embedData.get("authorUrl") != null ? String.valueOf(embedData.get("authorUrl")) : null;
            String icon = embedData.get("authorIcon") != null ? String.valueOf(embedData.get("authorIcon")) : null;
            builder.setAuthor(truncate(String.valueOf(embedData.get("author")), 256), url, icon);
        }
        if (embedData.get("thumbnail") != null) builder.setThumbnail(String.valueOf(embedData.get("thumbnail")));
        if (embedData.get("image") != null) builder.setImage(String.valueOf(embedData.get("image")));
        if (Boolean.TRUE.equals(embedData.get("timestamp"))) builder.setTimestamp(Instant.now());

        if (embedData.get("fields") instanceof List<?> fieldList) {
            int count = 0;
            for (Object item : fieldList) {
                if (count >= 25) break;
                if (item instanceof Map<?, ?> field) {
                    String value = String.valueOf(field.get("value"));
                    if (sanitizeMessages) value = sanitizeMessage(value, 1024);
                    builder.addField(
                            truncate(String.valueOf(field.get("name")), 256),
                            truncate(value, 1024),
                            Boolean.TRUE.equals(field.get("inline")));
                    count++;
                }
            }
        }
        return builder.build();
    }

    /**
     * Button: {id, label, style: primary|secondary|success|danger, disabled, emoji}
     * Link button: {url, label}
     */
    static Button buildButton(Map<String, Object> data) {
        if (data == null || data.get("label") == null) return null;
        String label = truncate(String.valueOf(data.get("label")), 80);
        Button button;

        if (data.get("url") != null) {
            button = Button.link(String.valueOf(data.get("url")), label);
        } else {
            if (data.get("id") == null) return null;
            String id = truncate(String.valueOf(data.get("id")), 100);
            String style = String.valueOf(data.getOrDefault("style", "primary")).toLowerCase();
            button = switch (style) {
                case "secondary" -> Button.secondary(id, label);
                case "success" -> Button.success(id, label);
                case "danger" -> Button.danger(id, label);
                default -> Button.primary(id, label);
            };
        }
        if (data.get("emoji") != null) {
            try {
                Emoji emoji = parseEmoji(data.get("emoji"));
                if (emoji != null) button = button.withEmoji(emoji);
            } catch (Exception ignored) {}
        }
        if (Boolean.TRUE.equals(data.get("disabled"))) {
            button = button.asDisabled();
        }
        return button;
    }

    /**
     * Dropdown (string select menu):
     * {type:"select", id, placeholder, min, max, disabled,
     *  options:[{label, value, description, emoji, default}]}   (toi da 25 option)
     */
    static StringSelectMenu buildSelect(Map<String, Object> data) {
        if (data == null || data.get("id") == null) return null;
        if (!(data.get("options") instanceof List<?> options) || options.isEmpty()) return null;

        StringSelectMenu.Builder builder = StringSelectMenu.create(truncate(String.valueOf(data.get("id")), 100));
        if (data.get("placeholder") != null) {
            builder.setPlaceholder(truncate(String.valueOf(data.get("placeholder")), 150));
        }

        int count = 0;
        for (Object item : options) {
            if (count >= 25) break;
            if (!(item instanceof Map<?, ?> o) || o.get("label") == null) continue;

            String label = truncate(String.valueOf(o.get("label")), 100);
            String value = truncate(String.valueOf(o.get("value") != null ? o.get("value") : o.get("label")), 100);
            SelectOption option = SelectOption.of(label, value);
            if (o.get("description") != null) {
                option = option.withDescription(truncate(String.valueOf(o.get("description")), 100));
            }
            if (o.get("emoji") != null) {
                try {
                    Emoji emoji = parseEmoji(o.get("emoji"));
                    if (emoji != null) option = option.withEmoji(emoji);
                } catch (Exception ignored) {}
            }
            if (Boolean.TRUE.equals(o.get("default"))) {
                option = option.withDefault(true);
            }
            builder.addOptions(option);
            count++;
        }
        if (count == 0) return null;

        int minValues = data.get("min") instanceof Number n ? n.intValue() : 1;
        int maxValues = data.get("max") instanceof Number n ? n.intValue() : 1;
        maxValues = Math.max(1, Math.min(maxValues, count));
        minValues = Math.max(0, Math.min(minValues, maxValues));
        builder.setRequiredRange(minValues, maxValues);

        if (Boolean.TRUE.equals(data.get("disabled"))) builder.setDisabled(true);
        return builder.build();
    }

    /**
     * Discord tu choi ca tin nhan neu 2 component co cung custom_id.
     * Neu script lo tao id trung, tu dong them hau to ":d2", ":d3"... (va ghi canh bao)
     * de tin van gui duoc. Nut link (url) khong co id nen bo qua.
     */
    private static Map<String, Object> ensureUniqueId(Map<String, Object> data, Set<String> usedIds) {
        if (data.get("id") == null || data.get("url") != null) return data;
        String id = truncate(String.valueOf(data.get("id")), 100);
        if (usedIds.add(id)) return data;

        int n = 2;
        String candidate;
        do {
            String suffix = ":d" + n++;
            candidate = truncate(id, 100 - suffix.length()) + suffix;
        } while (!usedIds.add(candidate));

        KhoinhanJS.LOGGER.warn("Duplicate Discord component id '" + id + "' renamed to '" + candidate + "'");
        Map<String, Object> copy = new LinkedHashMap<>(data);
        copy.put("id", candidate);
        return copy;
    }

    /**
     * Gop button + dropdown thanh cac hang (ActionRow).
     * - Button xep toi da 5 cai / hang.
     * - Moi dropdown chiem tron 1 hang.
     * - Toi da 5 hang.
     * Phan tu co type:"select" (hoac "dropdown") la dropdown, con lai la button.
     */
    static List<ActionRow> buildRows(List<Map<String, Object>> components) {
        List<ActionRow> rows = new ArrayList<>();
        if (components == null) return rows;
        List<Button> current = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();

        for (Map<String, Object> original : components) {
            if (rows.size() >= 5) break;
            if (original == null) continue;
            Map<String, Object> data = ensureUniqueId(original, usedIds);

            String type = String.valueOf(data.getOrDefault("type", "button")).toLowerCase();
            if (type.equals("select") || type.equals("dropdown")) {
                StringSelectMenu menu = buildSelect(data);
                if (menu == null) continue;
                if (!current.isEmpty()) {
                    rows.add(ActionRow.of(current));
                    current = new ArrayList<>();
                    if (rows.size() >= 5) break;
                }
                rows.add(ActionRow.of(menu));
            } else {
                Button button = buildButton(data);
                if (button == null) continue;
                current.add(button);
                if (current.size() == 5) {
                    rows.add(ActionRow.of(current));
                    current = new ArrayList<>();
                }
            }
        }
        if (!current.isEmpty() && rows.size() < 5) {
            rows.add(ActionRow.of(current));
        }
        return rows;
    }

    /**
     * Modal: {id, title, fields:[{id, label, style: short|paragraph, placeholder, value,
     *  required (mac dinh true), min, max}]}  (toi da 5 field)
     */
    static Modal buildModal(Map<String, Object> data) {
        if (data == null || data.get("id") == null || data.get("title") == null) return null;
        if (!(data.get("fields") instanceof List<?> fields)) return null;

        Modal.Builder builder = Modal.create(
                truncate(String.valueOf(data.get("id")), 100),
                truncate(String.valueOf(data.get("title")), 45));

        int count = 0;
        for (Object item : fields) {
            if (count >= 5) break;
            if (!(item instanceof Map<?, ?> f) || f.get("id") == null || f.get("label") == null) continue;

            TextInputStyle style = "paragraph".equalsIgnoreCase(String.valueOf(f.get("style")))
                    ? TextInputStyle.PARAGRAPH : TextInputStyle.SHORT;
            TextInput.Builder input = TextInput.create(
                    truncate(String.valueOf(f.get("id")), 100),
                    truncate(String.valueOf(f.get("label")), 45),
                    style);
            if (f.get("placeholder") != null) input.setPlaceholder(truncate(String.valueOf(f.get("placeholder")), 100));
            if (f.get("value") != null) input.setValue(truncate(String.valueOf(f.get("value")), 4000));
            input.setRequired(!Boolean.FALSE.equals(f.get("required")));
            if (f.get("min") instanceof Number n) input.setMinLength(n.intValue());
            if (f.get("max") instanceof Number n) input.setMaxLength(n.intValue());

            builder.addActionRow(input.build());
            count++;
        }
        if (count == 0) return null;
        return builder.build();
    }

    /**
     * Slash command: {name, description, adminOnly, guildOnly,
     *  options:[{name, description, type: string|integer|number|boolean|user|channel|role|mentionable,
     *            required, autocomplete, choices:[{name,value}]}]}
     */
    private static SlashCommandData buildCommand(Map<String, Object> data) {
        if (data == null || data.get("name") == null || data.get("description") == null) return null;
        String name = String.valueOf(data.get("name")).toLowerCase();
        if (!name.matches("[a-z0-9_-]{1,32}")) {
            KhoinhanJS.LOGGER.warn("Invalid slash command name: " + name);
            return null;
        }

        SlashCommandData command = Commands.slash(name, truncate(String.valueOf(data.get("description")), 100));
        if (Boolean.TRUE.equals(data.get("adminOnly"))) {
            command.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
        }
        if (Boolean.TRUE.equals(data.get("guildOnly"))) {
            command.setGuildOnly(true);
        }

        if (data.get("options") instanceof List<?> optionList) {
            command.addOptions(buildOptions(optionList));
        }

        if (data.get("subcommands") instanceof List<?> subList) {
            for (Object item : subList) {
                if (!(item instanceof Map<?, ?> sub) || sub.get("name") == null || sub.get("description") == null) continue;
                SubcommandData subcommand = new SubcommandData(
                        String.valueOf(sub.get("name")).toLowerCase(),
                        truncate(String.valueOf(sub.get("description")), 100));
                if (sub.get("options") instanceof List<?> subOptions) subcommand.addOptions(buildOptions(subOptions));
                command.addSubcommands(subcommand);
            }
        }
        return command;
    }

    private static void applyOptionLimits(OptionData option, OptionType type, Map<?, ?> o) {
        try {
            if (type == OptionType.INTEGER) {
                if (o.get("min") instanceof Number n) option.setMinValue(n.longValue());
                if (o.get("max") instanceof Number n) option.setMaxValue(n.longValue());
            } else if (type == OptionType.NUMBER) {
                if (o.get("min") instanceof Number n) option.setMinValue(n.doubleValue());
                if (o.get("max") instanceof Number n) option.setMaxValue(n.doubleValue());
            } else if (type == OptionType.STRING) {
                if (o.get("minLength") instanceof Number n) option.setMinLength(n.intValue());
                if (o.get("maxLength") instanceof Number n) option.setMaxLength(n.intValue());
            }
        } catch (Exception ignored) {}
    }

    private static List<OptionData> buildOptions(List<?> optionList) {
        List<OptionData> required = new ArrayList<>();
        List<OptionData> optional = new ArrayList<>();
        for (Object item : optionList) {
            if (!(item instanceof Map<?, ?> o) || o.get("name") == null || o.get("description") == null) continue;
            OptionType type = parseOptionType(String.valueOf(o.get("type") == null ? "string" : o.get("type")));
            boolean isRequired = Boolean.TRUE.equals(o.get("required"));
            boolean autocomplete = Boolean.TRUE.equals(o.get("autocomplete"))
                    && (type == OptionType.STRING || type == OptionType.INTEGER || type == OptionType.NUMBER);

            OptionData option = new OptionData(
                    type,
                    String.valueOf(o.get("name")).toLowerCase(),
                    truncate(String.valueOf(o.get("description")), 100),
                    isRequired,
                    autocomplete);

            if (!autocomplete && o.get("choices") instanceof List<?> choices) {
                int c = 0;
                for (Object choice : choices) {
                    if (c >= 25) break;
                    if (!(choice instanceof Map<?, ?> ch) || ch.get("name") == null || ch.get("value") == null) continue;
                    String choiceName = truncate(String.valueOf(ch.get("name")), 100);
                    try {
                        switch (type) {
                            case INTEGER -> option.addChoice(choiceName, ((Number) toNumber(ch.get("value"))).longValue());
                            case NUMBER -> option.addChoice(choiceName, ((Number) toNumber(ch.get("value"))).doubleValue());
                            default -> option.addChoice(choiceName, truncate(String.valueOf(ch.get("value")), 100));
                        }
                        c++;
                    } catch (Exception ignored) {}
                }
            }
            applyOptionLimits(option, type, o);
            (isRequired ? required : optional).add(option);
        }
        // Discord bat buoc option "required" phai dung truoc.
        List<OptionData> all = new ArrayList<>(required);
        all.addAll(optional);
        return all;
    }

    private static Number toNumber(Object value) {
        if (value instanceof Number n) return n;
        return Double.valueOf(String.valueOf(value));
    }

    private static OptionType parseOptionType(String type) {
        return switch (type.toLowerCase()) {
            case "integer", "int", "long" -> OptionType.INTEGER;
            case "number", "double", "float" -> OptionType.NUMBER;
            case "boolean", "bool" -> OptionType.BOOLEAN;
            case "user" -> OptionType.USER;
            case "channel" -> OptionType.CHANNEL;
            case "role" -> OptionType.ROLE;
            case "mentionable" -> OptionType.MENTIONABLE;
            case "attachment", "file" -> OptionType.ATTACHMENT;
            default -> OptionType.STRING;
        };
    }

    private static Object optionValue(OptionMapping m) {
        return switch (m.getType()) {
            case INTEGER -> m.getAsLong();
            case NUMBER -> m.getAsDouble();
            case BOOLEAN -> m.getAsBoolean();
            case USER -> m.getAsUser().getId();
            case CHANNEL -> m.getAsChannel().getId();
            case ROLE -> m.getAsRole().getId();
            case MENTIONABLE -> m.getAsMentionable().getId();
            case ATTACHMENT -> m.getAsAttachment().getUrl();
            default -> m.getAsString();
        };
    }

    // ------------------------------------------------------------------
    // Gui / sua / xoa tin nhan
    // ------------------------------------------------------------------

    public boolean sendMessage(String channelKey, String message) {
        return sendMessageWithComponents(channelKey, message, null, null);
    }

    public boolean sendMessageWithComponents(String channelKey, String message, List<Map<String, Object>> components) {
        return sendMessageWithComponents(channelKey, message, components, null);
    }

    /** onSent nhan messageId cua tin vua gui (co the null). */
    public boolean sendMessageWithComponents(String channelKey, String message,
                                             List<Map<String, Object>> components, Consumer<String> onSent) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;

            var action = channel.sendMessage(prepareText(message));
            if (components != null) action = action.setComponents(buildRows(components));
            action.queue(msg -> {
                if (onSent != null) {
                    try { onSent.accept(msg.getId()); } catch (Exception e) {
                        KhoinhanJS.LOGGER.error("Error in onSent callback: " + e.getMessage());
                    }
                }
            }, logFailure("send message"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to send Discord message: " + e.getMessage());
            return false;
        }
    }

    /** Tra loi (reply) mot tin nhan co san. */
    public boolean replyToMessage(String channelKey, String messageId, String text) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.sendMessage(prepareText(text))
                    .setMessageReference(messageId)
                    .mentionRepliedUser(false)
                    .queue(null, logFailure("reply to message"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to reply to Discord message: " + e.getMessage());
            return false;
        }
    }

    private static final java.net.http.HttpClient FILE_HTTP = java.net.http.HttpClient.newBuilder()
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static String fileNameFromUrl(String url) {
        String path = url;
        int cut = path.indexOf('?');
        if (cut >= 0) path = path.substring(0, cut);
        cut = path.indexOf('#');
        if (cut >= 0) path = path.substring(0, cut);
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isBlank() ? "file" : name;
    }

    private void notifySent(Consumer<String> onSent, Message msg) {
        if (onSent == null) return;
        try {
            onSent.accept(msg.getId());
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Error in onSent callback: " + e.getMessage());
        }
    }

    private void sendFileFallback(MessageChannel channel, String body, String url, String replyToMessageId, Consumer<String> onSent) {
        try {
            String content = truncate(body.isEmpty() ? url : body + "\n" + url, 2000);
            var action = channel.sendMessage(content);
            if (replyToMessageId != null && !replyToMessageId.isBlank()) {
                action = action.setMessageReference(replyToMessageId).mentionRepliedUser(false);
            }
            action.queue(msg -> notifySent(onSent, msg), logFailure("send file fallback"));
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to send Discord file fallback: " + e.getMessage());
        }
    }

    public boolean sendFileFromUrl(String channelKey, String text, String fileUrl, String replyToMessageId, Consumer<String> onSent) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null || fileUrl == null || fileUrl.isBlank()) return false;
            final String body = text == null ? "" : prepareText(text);
            final String url = fileUrl.trim();
            final long limit = channel instanceof GuildMessageChannel guildChannel
                    ? guildChannel.getGuild().getMaxFileSize()
                    : 10L * 1024 * 1024;
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "DiscordBot (https://khoinhan.local, 1.0)")
                    .GET()
                    .build();
            FILE_HTTP.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray()).whenComplete((response, error) -> {
                try {
                    byte[] data = error == null && response.statusCode() == 200 ? response.body() : null;
                    if (data == null || data.length == 0 || data.length > limit) {
                        sendFileFallback(channel, body, url, replyToMessageId, onSent);
                        return;
                    }
                    FileUpload upload = FileUpload.fromData(data, fileNameFromUrl(url));
                    var action = body.isEmpty() ? channel.sendFiles(upload) : channel.sendMessage(body).addFiles(upload);
                    if (replyToMessageId != null && !replyToMessageId.isBlank()) {
                        action = action.setMessageReference(replyToMessageId).mentionRepliedUser(false);
                    }
                    action.queue(msg -> notifySent(onSent, msg), err -> {
                        logFailure("send file").accept(err);
                        sendFileFallback(channel, body, url, replyToMessageId, onSent);
                    });
                } catch (Exception e) {
                    KhoinhanJS.LOGGER.error("Failed to upload Discord file: " + e.getMessage());
                    sendFileFallback(channel, body, url, replyToMessageId, onSent);
                }
            });
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to send Discord file: " + e.getMessage());
            return false;
        }
    }

    public boolean sendEmbed(String channelKey, Map<String, Object> embedData) {
        return sendEmbedWithComponents(channelKey, embedData, null, null);
    }

    /** Giu lai cho script cu. Dung chung voi sendEmbedWithComponents. */
    public boolean sendEmbedWithButtons(String channelKey, Map<String, Object> embedData, List<Map<String, Object>> buttons) {
        return sendEmbedWithComponents(channelKey, embedData, buttons, null);
    }

    public boolean sendEmbedWithComponents(String channelKey, Map<String, Object> embedData, List<Map<String, Object>> components) {
        return sendEmbedWithComponents(channelKey, embedData, components, null);
    }

    public boolean sendEmbedWithComponents(String channelKey, Map<String, Object> embedData,
                                           List<Map<String, Object>> components, Consumer<String> onSent) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;

            var action = channel.sendMessageEmbeds(buildEmbed(embedData));
            if (components != null) action = action.setComponents(buildRows(components));
            action.queue(msg -> {
                if (onSent != null) {
                    try { onSent.accept(msg.getId()); } catch (Exception e) {
                        KhoinhanJS.LOGGER.error("Error in onSent callback: " + e.getMessage());
                    }
                }
            }, logFailure("send embed"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to send Discord embed: " + e.getMessage());
            return false;
        }
    }

    public boolean editMessage(String channelKey, String messageId, String text) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.editMessageById(messageId, prepareText(text)).queue(null, logFailure("edit message"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to edit Discord message: " + e.getMessage());
            return false;
        }
    }

    /** components = null -> giu nguyen component cu; [] -> xoa het component. */
    public boolean editEmbed(String channelKey, String messageId, Map<String, Object> embedData,
                             List<Map<String, Object>> components) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            var action = channel.editMessageEmbedsById(messageId, buildEmbed(embedData));
            if (components != null) action = action.setComponents(buildRows(components));
            action.queue(null, logFailure("edit embed"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to edit Discord embed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteMessage(String channelKey, String messageId) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.deleteMessageById(messageId).queue(null, logFailure("delete message"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to delete Discord message: " + e.getMessage());
            return false;
        }
    }

    /** emoji: "👍" hoac "<:name:id>" cho emoji server. */
    public boolean addReaction(String channelKey, String messageId, String emoji) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.addReactionById(messageId, Emoji.fromFormatted(emoji)).queue(null, logFailure("add reaction"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to add Discord reaction: " + e.getMessage());
            return false;
        }
    }

    public boolean pinMessage(String channelKey, String messageId) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.pinMessageById(messageId).queue(null, logFailure("pin message"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to pin Discord message: " + e.getMessage());
            return false;
        }
    }

    /** Nhan tin rieng (DM). Se that bai (chi ghi log) neu nguoi dung chan DM tu server. */
    public boolean sendDM(String userId, String text) {
        try {
            String finalText = prepareText(text);
            jda.retrieveUserById(userId).queue(
                    user -> user.openPrivateChannel().queue(
                            channel -> channel.sendMessage(finalText).queue(null, logFailure("send DM")),
                            logFailure("open DM")),
                    logFailure("find user " + userId));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to send Discord DM: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Slash command
    // ------------------------------------------------------------------

    /**
     * Dang ky slash command. Neu co guildId trong config -> chi dang ky cho server do (co hieu luc ngay),
     * neu khong -> dang ky global (co the mat toi ~1 gio de hien).
     * Lenh nay GHI DE toan bo lenh cu cua bot o pham vi do.
     */
    public boolean registerCommands(List<Map<String, Object>> commandList) {
        try {
            List<SlashCommandData> data = new ArrayList<>();
            if (commandList != null) {
                for (Map<String, Object> c : commandList) {
                    SlashCommandData d = buildCommand(c);
                    if (d != null) data.add(d);
                }
            }
            if (guildId != null) {
                Guild guild = jda.getGuildById(guildId);
                if (guild == null) {
                    KhoinhanJS.LOGGER.warn("Discord guild not found: " + guildId);
                    return false;
                }
                guild.updateCommands().addCommands(data).queue(
                        ok -> KhoinhanJS.LOGGER.info("Registered " + data.size() + " guild slash commands"),
                        logFailure("register guild commands"));
            } else {
                jda.updateCommands().addCommands(data).queue(
                        ok -> KhoinhanJS.LOGGER.info("Registered " + data.size() + " global slash commands"),
                        logFailure("register global commands"));
            }
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to register Discord commands: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean clearCommands() {
        return registerCommands(Collections.emptyList());
    }

    // ------------------------------------------------------------------
    // Role / moderation (can quyen tuong ung cho bot trong server)
    // ------------------------------------------------------------------

    public boolean addRole(String userId, String roleId) {
        try {
            Guild guild = getGuild();
            Role role = guild == null ? null : guild.getRoleById(roleId);
            if (role == null) {
                KhoinhanJS.LOGGER.warn("Discord role/guild not found for addRole");
                return false;
            }
            guild.addRoleToMember(UserSnowflake.fromId(userId), role).queue(null, logFailure("add role"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to add Discord role: " + e.getMessage());
            return false;
        }
    }

    public boolean removeRole(String userId, String roleId) {
        try {
            Guild guild = getGuild();
            Role role = guild == null ? null : guild.getRoleById(roleId);
            if (role == null) {
                KhoinhanJS.LOGGER.warn("Discord role/guild not found for removeRole");
                return false;
            }
            guild.removeRoleFromMember(UserSnowflake.fromId(userId), role).queue(null, logFailure("remove role"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to remove Discord role: " + e.getMessage());
            return false;
        }
    }

    public boolean kick(String userId, String reason) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            guild.kick(UserSnowflake.fromId(userId)).reason(reason).queue(null, logFailure("kick"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to kick Discord user: " + e.getMessage());
            return false;
        }
    }

    /** deleteDays: xoa tin nhan cua nguoi do trong N ngay gan nhat (0-7). */
    public boolean ban(String userId, String reason, int deleteDays) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            int days = Math.max(0, Math.min(7, deleteDays));
            guild.ban(UserSnowflake.fromId(userId), days, TimeUnit.DAYS).reason(reason).queue(null, logFailure("ban"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to ban Discord user: " + e.getMessage());
            return false;
        }
    }

    public boolean timeout(String userId, int minutes, String reason) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            guild.timeoutFor(UserSnowflake.fromId(userId), Duration.ofMinutes(Math.max(1, minutes)))
                    .reason(reason).queue(null, logFailure("timeout"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to timeout Discord user: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Presence / thong tin bot
    // ------------------------------------------------------------------

    public void setActivity(String activity) {
        setActivity(activity, "playing");
    }

    /** type: playing | watching | listening | competing */
    public void setActivity(String activity, String type) {
        try {
            Activity a = switch (String.valueOf(type).toLowerCase()) {
                case "watching" -> Activity.watching(activity);
                case "listening" -> Activity.listening(activity);
                case "competing" -> Activity.competing(activity);
                default -> Activity.playing(activity);
            };
            jda.getPresence().setActivity(a);
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord activity: " + e.getMessage());
        }
    }

    /** status: online | idle | dnd | invisible */
    public void setStatus(String status) {
        OnlineStatus s = parseStatus(status);
        if (s == null) {
            KhoinhanJS.LOGGER.warn("Unknown Discord status: " + status);
            return;
        }
        jda.getPresence().setStatus(s);
    }

    public boolean isReady() { return jda != null && jda.getStatus() == JDA.Status.CONNECTED; }
    public long getPing() { return jda.getGatewayPing(); }
    public String getBotId() { return jda.getSelfUser().getId(); }
    public String getBotName() { return jda.getSelfUser().getName(); }

    public int getMemberCount() {
        Guild guild = getGuild();
        return guild == null ? 0 : guild.getMemberCount();
    }

    // ------------------------------------------------------------------

    private <T> void deliver(Consumer<T> callback, T value) {
        if (callback == null) return;
        try {
            callback.accept(value);
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Error in Discord callback: " + e.getMessage(), e);
        }
    }

    private void fire(Consumer<Map<String, Object>> handler, Map<String, Object> payload, String what) {
        if (handler == null) return;
        try {
            handler.accept(payload);
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Error in Discord " + what + " callback: " + e.getMessage(), e);
        }
    }

    private GuildChannel resolveGuildChannel(String keyOrId) {
        if (keyOrId == null) return null;
        String id = channels.getOrDefault(keyOrId, keyOrId);
        if (!id.matches("\\d{15,25}")) return null;
        return jda.getGuildChannelById(id);
    }

    private static EnumSet<Permission> parsePermissions(Object value) {
        EnumSet<Permission> set = EnumSet.noneOf(Permission.class);
        if (value == null) return set;
        Iterable<?> items = value instanceof Iterable<?> list ? list : Collections.singletonList(value);
        for (Object item : items) {
            try {
                set.add(Permission.valueOf(String.valueOf(item).toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return set;
    }

    private static Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("displayName", user.getEffectiveName());
        map.put("avatarUrl", user.getEffectiveAvatarUrl());
        map.put("isBot", user.isBot());
        map.put("createdAt", user.getTimeCreated().toString());
        return map;
    }

    private static Map<String, Object> memberToMap(Member member) {
        Map<String, Object> map = userToMap(member.getUser());
        map.put("nickname", member.getNickname());
        map.put("effectiveName", member.getEffectiveName());
        map.put("isOwner", member.isOwner());
        map.put("isAdmin", member.hasPermission(Permission.ADMINISTRATOR));
        map.put("joinedAt", member.getTimeJoined().toString());
        map.put("isTimedOut", member.isTimedOut());
        map.put("timeoutUntil", member.getTimeOutEnd() == null ? null : member.getTimeOutEnd().toString());
        map.put("roleIds", member.getRoles().stream().map(Role::getId).toArray(String[]::new));
        map.put("roles", member.getRoles().stream().map(Role::getName).toArray(String[]::new));
        var voice = member.getVoiceState();
        map.put("voiceChannelId", voice != null && voice.getChannel() != null ? voice.getChannel().getId() : null);
        return map;
    }

    private static Map<String, Object> messageToMap(Message message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", message.getId());
        map.put("channelId", message.getChannelId());
        map.put("guildId", message.isFromGuild() ? message.getGuild().getId() : null);
        map.put("content", message.getContentRaw());
        map.put("authorId", message.getAuthor().getId());
        map.put("authorName", message.getAuthor().getName());
        map.put("isBot", message.getAuthor().isBot());
        map.put("pinned", message.isPinned());
        map.put("createdAt", message.getTimeCreated().toString());
        map.put("editedAt", message.getTimeEdited() == null ? null : message.getTimeEdited().toString());
        map.put("referencedMessageId", message.getReferencedMessage() == null ? null : message.getReferencedMessage().getId());
        map.put("embedCount", message.getEmbeds().size());
        map.put("attachments", message.getAttachments().stream().map(Message.Attachment::getUrl).toArray(String[]::new));
        map.put("mentionedUserIds", message.getMentions().getUsers().stream().map(User::getId).toArray(String[]::new));
        return map;
    }

    private static Map<String, Object> reactionPayload(String userId, String messageId, String channelId, String guildId, String emoji, String emojiName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("messageId", messageId);
        payload.put("channelId", channelId);
        payload.put("guildId", guildId);
        payload.put("emoji", emoji);
        payload.put("emojiName", emojiName);
        return payload;
    }


    public boolean sendMessage(String channelKey, String message, Consumer<String> onSent) {
        return sendMessageWithComponents(channelKey, message, null, onSent);
    }

    public boolean replyToMessageWithEmbed(String channelKey, String messageId, Map<String, Object> embedData,
                                           List<Map<String, Object>> components, Consumer<String> onSent) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            var action = channel.sendMessageEmbeds(buildEmbed(embedData))
                    .setMessageReference(messageId)
                    .mentionRepliedUser(false);
            if (components != null) action = action.setComponents(buildRows(components));
            action.queue(msg -> deliver(onSent, msg.getId()), logFailure("reply with embed"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to reply with Discord embed: " + e.getMessage());
            return false;
        }
    }

    public boolean sendEmbeds(String channelKey, List<Map<String, Object>> embedList, Consumer<String> onSent) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null || embedList == null || embedList.isEmpty()) return false;
            List<MessageEmbed> embeds = new ArrayList<>();
            for (Map<String, Object> data : embedList) {
                if (embeds.size() >= 10) break;
                embeds.add(buildEmbed(data));
            }
            channel.sendMessageEmbeds(embeds).queue(msg -> deliver(onSent, msg.getId()), logFailure("send embeds"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to send Discord embeds: " + e.getMessage());
            return false;
        }
    }

    public boolean sendTextFile(String channelKey, String fileName, String content, String text) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null || content == null) return false;
            String name = fileName == null || fileName.isBlank() ? "message.txt" : fileName;
            FileUpload upload = FileUpload.fromData(content.getBytes(java.nio.charset.StandardCharsets.UTF_8), name);
            var action = text == null || text.isEmpty()
                    ? channel.sendFiles(upload)
                    : channel.sendMessage(prepareText(text)).addFiles(upload);
            action.queue(null, logFailure("send text file"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to send Discord text file: " + e.getMessage());
            return false;
        }
    }

    public boolean editMessageWithComponents(String channelKey, String messageId, String text, List<Map<String, Object>> components) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            var action = channel.editMessageById(messageId, prepareText(text));
            if (components != null) action = action.setComponents(buildRows(components));
            action.queue(null, logFailure("edit message"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to edit Discord message: " + e.getMessage());
            return false;
        }
    }

    public boolean unpinMessage(String channelKey, String messageId) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.unpinMessageById(messageId).queue(null, logFailure("unpin message"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to unpin Discord message: " + e.getMessage());
            return false;
        }
    }

    public boolean removeReaction(String channelKey, String messageId, String emoji) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.removeReactionById(messageId, Emoji.fromFormatted(emoji)).queue(null, logFailure("remove reaction"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to remove Discord reaction: " + e.getMessage());
            return false;
        }
    }

    public boolean clearReactions(String channelKey, String messageId) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            if (!(channel instanceof GuildMessageChannel guildChannel)) return false;
            guildChannel.clearReactionsById(messageId).queue(null, logFailure("clear reactions"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to clear Discord reactions: " + e.getMessage());
            return false;
        }
    }

    public boolean startTyping(String channelKey) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.sendTyping().queue(null, logFailure("send typing"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to send Discord typing: " + e.getMessage());
            return false;
        }
    }

    public boolean purgeMessages(String channelKey, int count) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.getHistory().retrievePast(Math.max(1, Math.min(100, count))).queue(list -> {
                if (list.size() > 1) {
                    channel.purgeMessages(list);
                } else if (list.size() == 1) {
                    list.get(0).delete().queue(null, logFailure("purge message"));
                }
            }, logFailure("purge messages"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to purge Discord messages: " + e.getMessage());
            return false;
        }
    }

    public boolean getMessage(String channelKey, String messageId, Consumer<Map<String, Object>> callback) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.retrieveMessageById(messageId).queue(
                    msg -> deliver(callback, messageToMap(msg)),
                    logFailure("get message"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to get Discord message: " + e.getMessage());
            return false;
        }
    }

    public boolean getHistory(String channelKey, int limit, Consumer<List<Map<String, Object>>> callback) {
        try {
            MessageChannel channel = resolveChannel(channelKey);
            if (channel == null) return false;
            channel.getHistory().retrievePast(Math.max(1, Math.min(100, limit))).queue(list -> {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Message m : list) out.add(messageToMap(m));
                deliver(callback, out);
            }, logFailure("get history"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to get Discord history: " + e.getMessage());
            return false;
        }
    }

    public boolean sendDMEmbed(String userId, Map<String, Object> embedData) {
        try {
            MessageEmbed embed = buildEmbed(embedData);
            jda.retrieveUserById(userId).queue(
                    user -> user.openPrivateChannel().queue(
                            channel -> channel.sendMessageEmbeds(embed).queue(null, logFailure("send DM embed")),
                            logFailure("open DM")),
                    logFailure("find user " + userId));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to send Discord DM embed: " + e.getMessage());
            return false;
        }
    }


    public boolean createThread(String channelKey, String name, Consumer<String> onCreated) {
        try {
            GuildChannel channel = resolveGuildChannel(channelKey);
            if (!(channel instanceof IThreadContainer container)) return false;
            container.createThreadChannel(truncate(name, 100))
                    .queue(thread -> deliver(onCreated, thread.getId()), logFailure("create thread"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to create Discord thread: " + e.getMessage());
            return false;
        }
    }

    public boolean createThreadFromMessage(String channelKey, String messageId, String name, Consumer<String> onCreated) {
        try {
            GuildChannel channel = resolveGuildChannel(channelKey);
            if (!(channel instanceof IThreadContainer container)) return false;
            container.createThreadChannel(truncate(name, 100), messageId)
                    .queue(thread -> deliver(onCreated, thread.getId()), logFailure("create thread from message"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to create Discord thread from message: " + e.getMessage());
            return false;
        }
    }

    public boolean setThreadArchived(String threadId, boolean archived) {
        try {
            ThreadChannel thread = jda.getThreadChannelById(threadId);
            if (thread == null) return false;
            thread.getManager().setArchived(archived).queue(null, logFailure("archive thread"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to archive Discord thread: " + e.getMessage());
            return false;
        }
    }

    public boolean setThreadLocked(String threadId, boolean locked) {
        try {
            ThreadChannel thread = jda.getThreadChannelById(threadId);
            if (thread == null) return false;
            thread.getManager().setLocked(locked).queue(null, logFailure("lock thread"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to lock Discord thread: " + e.getMessage());
            return false;
        }
    }

    public boolean addThreadMember(String threadId, String userId) {
        try {
            ThreadChannel thread = jda.getThreadChannelById(threadId);
            if (thread == null) return false;
            thread.addThreadMemberById(userId).queue(null, logFailure("add thread member"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to add Discord thread member: " + e.getMessage());
            return false;
        }
    }

    public boolean removeThreadMember(String threadId, String userId) {
        try {
            ThreadChannel thread = jda.getThreadChannelById(threadId);
            if (thread == null) return false;
            thread.removeThreadMemberById(userId).queue(null, logFailure("remove thread member"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to remove Discord thread member: " + e.getMessage());
            return false;
        }
    }


    public boolean createTextChannel(String name, String categoryId, Consumer<String> onCreated) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            Category category = categoryId == null || categoryId.isBlank() ? null : guild.getCategoryById(categoryId);
            guild.createTextChannel(truncate(name, 100), category)
                    .queue(channel -> deliver(onCreated, channel.getId()), logFailure("create text channel"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to create Discord text channel: " + e.getMessage());
            return false;
        }
    }

    public boolean createVoiceChannel(String name, String categoryId, Consumer<String> onCreated) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            Category category = categoryId == null || categoryId.isBlank() ? null : guild.getCategoryById(categoryId);
            guild.createVoiceChannel(truncate(name, 100), category)
                    .queue(channel -> deliver(onCreated, channel.getId()), logFailure("create voice channel"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to create Discord voice channel: " + e.getMessage());
            return false;
        }
    }

    public boolean createCategory(String name, Consumer<String> onCreated) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            guild.createCategory(truncate(name, 100))
                    .queue(category -> deliver(onCreated, category.getId()), logFailure("create category"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to create Discord category: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteChannel(String channelKey) {
        try {
            GuildChannel channel = resolveGuildChannel(channelKey);
            if (channel == null) return false;
            channel.delete().queue(null, logFailure("delete channel"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to delete Discord channel: " + e.getMessage());
            return false;
        }
    }

    public boolean renameChannel(String channelKey, String name) {
        try {
            GuildChannel channel = resolveGuildChannel(channelKey);
            if (channel == null) return false;
            channel.getManager().setName(truncate(name, 100)).queue(null, logFailure("rename channel"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to rename Discord channel: " + e.getMessage());
            return false;
        }
    }

    public boolean setChannelTopic(String channelKey, String topic) {
        try {
            if (!(resolveGuildChannel(channelKey) instanceof TextChannel text)) return false;
            text.getManager().setTopic(truncate(topic, 1024)).queue(null, logFailure("set channel topic"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord channel topic: " + e.getMessage());
            return false;
        }
    }

    public boolean setSlowmode(String channelKey, int seconds) {
        try {
            if (!(resolveGuildChannel(channelKey) instanceof TextChannel text)) return false;
            text.getManager().setSlowmode(Math.max(0, Math.min(21600, seconds))).queue(null, logFailure("set slowmode"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord slowmode: " + e.getMessage());
            return false;
        }
    }

    public boolean setChannelNsfw(String channelKey, boolean nsfw) {
        try {
            if (!(resolveGuildChannel(channelKey) instanceof TextChannel text)) return false;
            text.getManager().setNSFW(nsfw).queue(null, logFailure("set channel nsfw"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord channel nsfw: " + e.getMessage());
            return false;
        }
    }

    public boolean setChannelLocked(String channelKey, boolean locked) {
        try {
            Guild guild = getGuild();
            if (guild == null || !(resolveGuildChannel(channelKey) instanceof IPermissionContainer container)) return false;
            var action = container.upsertPermissionOverride(guild.getPublicRole());
            action = locked ? action.deny(Permission.MESSAGE_SEND) : action.clear(Permission.MESSAGE_SEND);
            action.queue(null, logFailure("lock channel"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to lock Discord channel: " + e.getMessage());
            return false;
        }
    }

    public boolean setChannelPermission(String channelKey, String targetId, Object allow, Object deny) {
        try {
            Guild guild = getGuild();
            if (guild == null || !(resolveGuildChannel(channelKey) instanceof IPermissionContainer container)) return false;
            IPermissionHolder holder = guild.getRoleById(targetId);
            if (holder == null) holder = guild.getMemberById(targetId);
            if (holder == null) return false;
            container.upsertPermissionOverride(holder)
                    .setAllowed(parsePermissions(allow))
                    .setDenied(parsePermissions(deny))
                    .queue(null, logFailure("set channel permission"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord channel permission: " + e.getMessage());
            return false;
        }
    }

    public boolean createInvite(String channelKey, int maxAgeSeconds, int maxUses, Consumer<String> onCreated) {
        try {
            if (!(resolveGuildChannel(channelKey) instanceof IInviteContainer container)) return false;
            container.createInvite()
                    .setMaxAge((long) Math.max(0, maxAgeSeconds), TimeUnit.SECONDS)
                    .setMaxUses(Math.max(0, maxUses))
                    .queue(invite -> deliver(onCreated, invite.getUrl()), logFailure("create invite"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to create Discord invite: " + e.getMessage());
            return false;
        }
    }


    public boolean moveVoiceMember(String userId, String voiceChannelKey) {
        try {
            final Guild guild = getGuild();
            if (guild == null || voiceChannelKey == null) return false;
            final VoiceChannel voice = guild.getVoiceChannelById(channels.getOrDefault(voiceChannelKey, voiceChannelKey));
            if (voice == null) return false;
            guild.retrieveMemberById(userId).queue(
                    member -> guild.moveVoiceMember(member, voice).queue(null, logFailure("move voice member")),
                    logFailure("find member " + userId));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to move Discord voice member: " + e.getMessage());
            return false;
        }
    }

    public boolean disconnectVoiceMember(String userId) {
        try {
            final Guild guild = getGuild();
            if (guild == null) return false;
            guild.retrieveMemberById(userId).queue(
                    member -> guild.kickVoiceMember(member).queue(null, logFailure("disconnect voice member")),
                    logFailure("find member " + userId));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to disconnect Discord voice member: " + e.getMessage());
            return false;
        }
    }

    public boolean muteVoiceMember(String userId, boolean muted) {
        try {
            final Guild guild = getGuild();
            if (guild == null) return false;
            guild.retrieveMemberById(userId).queue(
                    member -> guild.mute(member, muted).queue(null, logFailure("mute voice member")),
                    logFailure("find member " + userId));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to mute Discord voice member: " + e.getMessage());
            return false;
        }
    }

    public boolean deafenVoiceMember(String userId, boolean deafened) {
        try {
            final Guild guild = getGuild();
            if (guild == null) return false;
            guild.retrieveMemberById(userId).queue(
                    member -> guild.deafen(member, deafened).queue(null, logFailure("deafen voice member")),
                    logFailure("find member " + userId));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to deafen Discord voice member: " + e.getMessage());
            return false;
        }
    }


    public boolean createRole(String name, Object color, Object permissions, boolean hoisted, boolean mentionable, Consumer<String> onCreated) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            var action = guild.createRole()
                    .setName(truncate(name, 100))
                    .setHoisted(hoisted)
                    .setMentionable(mentionable);
            Integer rgb = parseColor(color);
            if (rgb != null) action = action.setColor(rgb);
            if (permissions != null) action = action.setPermissions(parsePermissions(permissions));
            action.queue(role -> deliver(onCreated, role.getId()), logFailure("create role"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to create Discord role: " + e.getMessage());
            return false;
        }
    }

    public boolean editRole(String roleId, Map<String, Object> data) {
        try {
            Guild guild = getGuild();
            Role role = guild == null ? null : guild.getRoleById(roleId);
            if (role == null || data == null) return false;
            RoleManager manager = role.getManager();
            if (data.get("name") != null) manager = manager.setName(truncate(String.valueOf(data.get("name")), 100));
            Integer rgb = parseColor(data.get("color"));
            if (rgb != null) manager = manager.setColor(rgb);
            if (data.get("hoisted") instanceof Boolean hoisted) manager = manager.setHoisted(hoisted);
            if (data.get("mentionable") instanceof Boolean mentionable) manager = manager.setMentionable(mentionable);
            if (data.get("permissions") != null) manager = manager.setPermissions(parsePermissions(data.get("permissions")));
            manager.queue(null, logFailure("edit role"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to edit Discord role: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRole(String roleId) {
        try {
            Guild guild = getGuild();
            Role role = guild == null ? null : guild.getRoleById(roleId);
            if (role == null) return false;
            role.delete().queue(null, logFailure("delete role"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to delete Discord role: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> listRoles() {
        List<Map<String, Object>> out = new ArrayList<>();
        Guild guild = getGuild();
        if (guild == null) return out;
        for (Role role : guild.getRoles()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", role.getId());
            map.put("name", role.getName());
            map.put("color", role.getColorRaw());
            map.put("position", role.getPosition());
            map.put("hoisted", role.isHoisted());
            map.put("mentionable", role.isMentionable());
            map.put("managed", role.isManaged());
            map.put("permissions", role.getPermissions().stream().map(Permission::name).toArray(String[]::new));
            out.add(map);
        }
        return out;
    }


    public boolean unban(String userId) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            guild.unban(UserSnowflake.fromId(userId)).queue(null, logFailure("unban"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to unban Discord user: " + e.getMessage());
            return false;
        }
    }

    public boolean removeTimeout(String userId) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            guild.removeTimeout(UserSnowflake.fromId(userId)).queue(null, logFailure("remove timeout"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to remove Discord timeout: " + e.getMessage());
            return false;
        }
    }

    public boolean setNickname(String userId, String nickname) {
        try {
            final Guild guild = getGuild();
            if (guild == null) return false;
            final String finalNick = nickname == null || nickname.isEmpty() ? null : truncate(nickname, 32);
            guild.retrieveMemberById(userId).queue(
                    member -> guild.modifyNickname(member, finalNick).queue(null, logFailure("set nickname")),
                    logFailure("find member " + userId));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord nickname: " + e.getMessage());
            return false;
        }
    }

    public boolean setBotNickname(String nickname) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            String finalNick = nickname == null || nickname.isEmpty() ? null : truncate(nickname, 32);
            guild.modifyNickname(guild.getSelfMember(), finalNick).queue(null, logFailure("set bot nickname"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord bot nickname: " + e.getMessage());
            return false;
        }
    }

    public boolean getMemberInfo(String userId, Consumer<Map<String, Object>> callback) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            guild.retrieveMemberById(userId).queue(
                    member -> deliver(callback, memberToMap(member)),
                    err -> deliver(callback, null));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to get Discord member info: " + e.getMessage());
            return false;
        }
    }

    public boolean getUserInfo(String userId, Consumer<Map<String, Object>> callback) {
        try {
            jda.retrieveUserById(userId).queue(
                    user -> deliver(callback, userToMap(user)),
                    err -> deliver(callback, null));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to get Discord user info: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getMembers() {
        List<Map<String, Object>> out = new ArrayList<>();
        Guild guild = getGuild();
        if (guild == null) return out;
        for (Member member : guild.getMembers()) out.add(memberToMap(member));
        return out;
    }

    public boolean getBans(Consumer<List<Map<String, Object>>> callback) {
        try {
            Guild guild = getGuild();
            if (guild == null) return false;
            guild.retrieveBanList().queue(bans -> {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Guild.Ban ban : bans) {
                    Map<String, Object> map = userToMap(ban.getUser());
                    map.put("reason", ban.getReason());
                    out.add(map);
                }
                deliver(callback, out);
            }, logFailure("get bans"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to get Discord bans: " + e.getMessage());
            return false;
        }
    }


    public Map<String, Object> getGuildInfo() {
        Guild guild = getGuild();
        if (guild == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", guild.getId());
        map.put("name", guild.getName());
        map.put("description", guild.getDescription());
        map.put("iconUrl", guild.getIconUrl());
        map.put("ownerId", guild.getOwnerId());
        map.put("memberCount", guild.getMemberCount());
        map.put("boostCount", guild.getBoostCount());
        map.put("boostTier", guild.getBoostTier().getKey());
        map.put("roleCount", guild.getRoles().size());
        map.put("channelCount", guild.getChannels().size());
        map.put("maxFileSize", guild.getMaxFileSize());
        map.put("createdAt", guild.getTimeCreated().toString());
        return map;
    }

    public List<Map<String, Object>> listChannels() {
        List<Map<String, Object>> out = new ArrayList<>();
        Guild guild = getGuild();
        if (guild == null) return out;
        for (GuildChannel channel : guild.getChannels()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", channel.getId());
            map.put("name", channel.getName());
            map.put("type", channel.getType().name());
            if (channel instanceof TextChannel text) map.put("topic", text.getTopic());
            out.add(map);
        }
        return out;
    }

    public Map<String, Object> getChannelInfo(String channelKey) {
        GuildChannel channel = resolveGuildChannel(channelKey);
        if (channel == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", channel.getId());
        map.put("name", channel.getName());
        map.put("type", channel.getType().name());
        map.put("createdAt", channel.getTimeCreated().toString());
        if (channel instanceof TextChannel text) {
            map.put("topic", text.getTopic());
            map.put("nsfw", text.isNSFW());
            map.put("slowmode", text.getSlowmode());
        }
        return map;
    }


    public boolean setBotName(String name) {
        try {
            jda.getSelfUser().getManager().setName(truncate(name, 32)).queue(null, logFailure("set bot name"));
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord bot name: " + e.getMessage());
            return false;
        }
    }

    public boolean setBotAvatar(String imageUrl) {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(imageUrl.trim()))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "DiscordBot (https://khoinhan.local, 1.0)")
                    .GET()
                    .build();
            FILE_HTTP.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray()).whenComplete((response, error) -> {
                try {
                    if (error != null || response.statusCode() != 200) {
                        KhoinhanJS.LOGGER.error("Discord: failed to download bot avatar");
                        return;
                    }
                    Icon icon = Icon.from(response.body());
                    jda.getSelfUser().getManager().setAvatar(icon).queue(null, logFailure("set bot avatar"));
                } catch (Exception e) {
                    KhoinhanJS.LOGGER.error("Failed to set Discord bot avatar: " + e.getMessage());
                }
            });
            return true;
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord bot avatar: " + e.getMessage());
            return false;
        }
    }

    public void setActivityStreaming(String name, String url) {
        try {
            jda.getPresence().setActivity(Activity.streaming(name, url));
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord streaming activity: " + e.getMessage());
        }
    }

    public void setActivityCustom(String text) {
        try {
            jda.getPresence().setActivity(Activity.customStatus(text));
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Failed to set Discord custom activity: " + e.getMessage());
        }
    }

    public void clearActivity() {
        jda.getPresence().setActivity(null);
    }


    // Dang ky callback
    // ------------------------------------------------------------------

    public void onMessage(Consumer<DiscordMessage> callback) { this.messageCallbacks.add(callback); }
    public void onButton(Consumer<DiscordButton> callback) { this.buttonCallback = callback; }
    public void onSelect(Consumer<DiscordSelect> callback) { this.selectCallback = callback; }
    public void onSlashCommand(Consumer<DiscordCommand> callback) { this.commandCallback = callback; }
    public void onAutocomplete(Consumer<DiscordAutocomplete> callback) { this.autocompleteCallback = callback; }
    public void onModal(Consumer<DiscordModal> callback) { this.modalCallback = callback; }
    public void onReactionAdd(Consumer<Map<String, Object>> callback) { this.reactionAddCallback = callback; }
    public void onReactionRemove(Consumer<Map<String, Object>> callback) { this.reactionRemoveCallback = callback; }
    public void onMessageDelete(Consumer<Map<String, Object>> callback) { this.messageDeleteCallback = callback; }
    public void onMessageEdit(Consumer<Map<String, Object>> callback) { this.messageEditCallback = callback; }
    public void onMemberJoin(Consumer<Map<String, Object>> callback) { this.memberJoinCallback = callback; }
    public void onMemberLeave(Consumer<Map<String, Object>> callback) { this.memberLeaveCallback = callback; }
    public void onVoiceUpdate(Consumer<Map<String, Object>> callback) { this.voiceCallback = callback; }

    /** Goi khi /reload script de tranh callback bi nhan doi. */
    public void clearCallbacks() {
        messageCallbacks.clear();
        buttonCallback = null;
        selectCallback = null;
        commandCallback = null;
        autocompleteCallback = null;
        modalCallback = null;
        reactionAddCallback = null;
        reactionRemoveCallback = null;
        messageDeleteCallback = null;
        messageEditCallback = null;
        memberJoinCallback = null;
        memberLeaveCallback = null;
        voiceCallback = null;
    }

    public void shutdown() {
        clearCallbacks();
        if (jda != null) {
            jda.shutdown();
        }
    }

    // ------------------------------------------------------------------
    // Wrapper cho interaction (script dung)
    // ------------------------------------------------------------------

    /** Chuc nang chung cho moi loai interaction: reply, defer, modal, check quyen. */
    public static class DiscordInteraction {
        protected final DiscordBinding owner;
        protected final IReplyCallback callback;

        protected DiscordInteraction(DiscordBinding owner, IReplyCallback callback) {
            this.owner = owner;
            this.callback = callback;
        }

        public String getUserId() { return callback.getUser().getId(); }
        public String getUserName() { return callback.getUser().getName(); }
        public String getChannelId() { return callback.getChannel() != null ? callback.getChannel().getId() : null; }
        public String getGuildId() { return callback.getGuild() != null ? callback.getGuild().getId() : null; }
        public boolean isAcknowledged() { return callback.isAcknowledged(); }

        public boolean hasRole(String roleId) {
            Member m = callback.getMember();
            return m != null && m.getRoles().stream().anyMatch(r -> r.getId().equals(roleId));
        }

        public String[] getRoleIds() {
            Member m = callback.getMember();
            if (m == null) return new String[0];
            return m.getRoles().stream().map(Role::getId).toArray(String[]::new);
        }

        /** name: ten Permission cua JDA, vd "ADMINISTRATOR", "MANAGE_SERVER", "KICK_MEMBERS". */
        public boolean hasPermission(String name) {
            Member m = callback.getMember();
            if (m == null || name == null) return false;
            try {
                return m.hasPermission(Permission.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        public boolean isAdmin() { return hasPermission("ADMINISTRATOR"); }

        public String getUserAvatarUrl() { return callback.getUser().getEffectiveAvatarUrl(); }
        public boolean isFromGuild() { return callback.getGuild() != null; }
        public String getLocale() { return callback.getUserLocale().getLocale(); }

        public String getNickname() {
            Member m = callback.getMember();
            return m == null ? null : m.getEffectiveName();
        }

        public boolean replyWithComponents(String text, List<Map<String, Object>> components, boolean ephemeral) {
            try {
                String finalText = text == null || text.isEmpty() ? "\u200B" : truncate(text, 2000);
                List<ActionRow> rows = buildRows(components);
                if (callback.isAcknowledged()) {
                    callback.getHook().sendMessage(finalText).setComponents(rows).setEphemeral(ephemeral).queue();
                } else {
                    callback.reply(finalText).setComponents(rows).setEphemeral(ephemeral).queue();
                }
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to reply with components to Discord interaction: " + e.getMessage());
                return false;
            }
        }

        public boolean editReply(String text) {
            try {
                if (!callback.isAcknowledged()) return false;
                callback.getHook().editOriginal(truncate(text, 2000)).queue();
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to edit Discord interaction reply: " + e.getMessage());
                return false;
            }
        }

        public boolean deleteReply() {
            try {
                if (!callback.isAcknowledged()) return false;
                callback.getHook().deleteOriginal().queue();
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to delete Discord interaction reply: " + e.getMessage());
                return false;
            }
        }

        /** Bao Discord "dang xu ly" (can lam neu xu ly > 3 giay), sau do dung reply(). */
        public boolean deferReply(boolean ephemeral) {
            try {
                if (callback.isAcknowledged()) return false;
                callback.deferReply(ephemeral).queue();
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to defer Discord interaction: " + e.getMessage());
                return false;
            }
        }

        /** Tra loi chi nguoi bam thay (ephemeral). */
        public boolean reply(String text) { return sendReply(text, true); }

        /** Tra loi cong khai cho ca kenh. */
        public boolean replyPublic(String text) { return sendReply(text, false); }

        public boolean replyEmbed(Map<String, Object> embedData, boolean ephemeral) {
            return replyEmbed(embedData, null, ephemeral);
        }

        public boolean replyEmbed(Map<String, Object> embedData, List<Map<String, Object>> components, boolean ephemeral) {
            try {
                MessageEmbed embed = owner.buildEmbed(embedData);
                List<ActionRow> rows = buildRows(components);
                if (callback.isAcknowledged()) {
                    callback.getHook().sendMessageEmbeds(embed).setComponents(rows).setEphemeral(ephemeral).queue();
                } else {
                    callback.replyEmbeds(embed).setComponents(rows).setEphemeral(ephemeral).queue();
                }
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to reply embed to Discord interaction: " + e.getMessage());
                return false;
            }
        }

        /** Hien modal (form nhap chu). Phai la phan hoi dau tien, chua defer/reply. */
        public boolean showModal(Map<String, Object> modalData) {
            try {
                Modal modal = buildModal(modalData);
                if (modal == null) return false;
                if (callback.isAcknowledged() || !(callback instanceof IModalCallback modalCallback)) return false;
                modalCallback.replyModal(modal).queue();
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to show Discord modal: " + e.getMessage());
                return false;
            }
        }

        private boolean sendReply(String text, boolean ephemeral) {
            try {
                String finalText = text == null || text.isEmpty() ? "\u200B" : text;
                if (finalText.length() > 2000) finalText = finalText.substring(0, 2000);
                if (callback.isAcknowledged()) {
                    callback.getHook().sendMessage(finalText).setEphemeral(ephemeral).queue();
                } else {
                    callback.reply(finalText).setEphemeral(ephemeral).queue();
                }
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to reply to Discord interaction: " + e.getMessage());
                return false;
            }
        }
    }

    /** Chung cho button va dropdown: sua lai chinh tin nhan chua component. */
    public static class DiscordComponentInteraction extends DiscordInteraction {
        protected final GenericComponentInteractionCreateEvent event;

        protected DiscordComponentInteraction(DiscordBinding owner, GenericComponentInteractionCreateEvent event) {
            super(owner, event);
            this.event = event;
        }

        public String getCustomId() { return event.getComponentId(); }
        public String getMessageId() { return event.getMessageId(); }

        /** Xac nhan "da nhan" nhung khong thay doi gi tren tin nhan. */
        public boolean deferEdit() {
            try {
                if (event.isAcknowledged()) return false;
                event.deferEdit().queue();
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to defer Discord component: " + e.getMessage());
                return false;
            }
        }

        /** Luu y: ban 1 tham so se XOA het component cu (giu hanh vi cu). */
        public boolean editEmbed(Map<String, Object> embedData) {
            return editEmbed(embedData, Collections.emptyList());
        }

        /** components = null -> giu nguyen; [] -> xoa; danh sach -> thay moi (button + dropdown). */
        public boolean editEmbed(Map<String, Object> embedData, List<Map<String, Object>> components) {
            try {
                MessageEmbed embed = owner.buildEmbed(embedData);
                if (event.isAcknowledged()) {
                    var action = event.getHook().editOriginalEmbeds(embed);
                    if (components != null) action = action.setComponents(buildRows(components));
                    action.queue();
                } else {
                    var action = event.editMessageEmbeds(embed);
                    if (components != null) action = action.setComponents(buildRows(components));
                    action.queue();
                }
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to edit Discord component message: " + e.getMessage());
                return false;
            }
        }

        public boolean editMessage(String text) {
            return editMessage(text, null);
        }

        public boolean editMessage(String text, List<Map<String, Object>> components) {
            try {
                String finalText = text == null ? "" : text;
                if (finalText.length() > 2000) finalText = finalText.substring(0, 2000);
                if (event.isAcknowledged()) {
                    var action = event.getHook().editOriginal(finalText);
                    if (components != null) action = action.setComponents(buildRows(components));
                    action.queue();
                } else {
                    var action = event.editMessage(finalText);
                    if (components != null) action = action.setComponents(buildRows(components));
                    action.queue();
                }
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to edit Discord message: " + e.getMessage());
                return false;
            }
        }

        /** Vo hieu hoa toan bo button/dropdown cua tin nhan (vd sau khi da chon xong). */
        public boolean disableComponents() {
            try {
                List<ActionRow> rows = new ArrayList<>();
                for (ActionRow row : event.getMessage().getActionRows()) {
                    rows.add(row.asDisabled());
                }
                if (event.isAcknowledged()) {
                    event.getHook().editOriginalComponents(rows).queue();
                } else {
                    event.editComponents(rows).queue();
                }
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to disable Discord components: " + e.getMessage());
                return false;
            }
        }
    }

    public static class DiscordButton extends DiscordComponentInteraction {
        public DiscordButton(DiscordBinding owner, ButtonInteractionEvent event) {
            super(owner, event);
        }
    }

    /** Dropdown (string select menu) da duoc chon. */
    public static class DiscordSelect extends DiscordComponentInteraction {
        private final StringSelectInteractionEvent selectEvent;

        public DiscordSelect(DiscordBinding owner, StringSelectInteractionEvent event) {
            super(owner, event);
            this.selectEvent = event;
        }

        /** Cac value da chon (dropdown cho chon nhieu thi co nhieu phan tu). */
        public String[] getValues() { return selectEvent.getValues().toArray(new String[0]); }

        /** Value dau tien da chon (tien cho dropdown chon 1). */
        public String getValue() {
            List<String> values = selectEvent.getValues();
            return values.isEmpty() ? null : values.get(0);
        }

        public String[] getLabels() {
            return selectEvent.getSelectedOptions().stream().map(SelectOption::getLabel).toArray(String[]::new);
        }
    }

    public static class DiscordCommand extends DiscordInteraction {
        private final SlashCommandInteractionEvent event;

        public DiscordCommand(DiscordBinding owner, SlashCommandInteractionEvent event) {
            super(owner, event);
            this.event = event;
        }

        public String getName() { return event.getName(); }
        public String getSubcommandName() { return event.getSubcommandName(); }
        public String getSubcommandGroup() { return event.getSubcommandGroup(); }

        public Long getInteger(String name) {
            OptionMapping m = event.getOption(name);
            return m == null ? null : m.getAsLong();
        }

        public Boolean getBoolean(String name) {
            OptionMapping m = event.getOption(name);
            return m == null ? null : m.getAsBoolean();
        }

        public String getAttachmentUrl(String name) {
            OptionMapping m = event.getOption(name);
            return m == null ? null : m.getAsAttachment().getUrl();
        }

        /** Gia tri option: String / Long / Double / Boolean / ID (voi user, channel, role). */
        public Object getOption(String name) {
            OptionMapping m = event.getOption(name);
            return m == null ? null : optionValue(m);
        }

        public String getString(String name) {
            OptionMapping m = event.getOption(name);
            return m == null ? null : m.getAsString();
        }

        public Map<String, Object> getOptions() {
            Map<String, Object> map = new LinkedHashMap<>();
            for (OptionMapping m : event.getOptions()) {
                map.put(m.getName(), optionValue(m));
            }
            return map;
        }
    }

    public static class DiscordAutocomplete {
        private final CommandAutoCompleteInteractionEvent event;

        public DiscordAutocomplete(DiscordBinding owner, CommandAutoCompleteInteractionEvent event) {
            this.event = event;
        }

        public String getCommandName() { return event.getName(); }
        public String getFocusedName() { return event.getFocusedOption().getName(); }
        public String getFocusedValue() { return event.getFocusedOption().getValue(); }
        public String getUserId() { return event.getUser().getId(); }

        /** Goi y toi da 25 lua chon. */
        public boolean respond(List<Object> choices) {
            try {
                List<String> list = new ArrayList<>();
                if (choices != null) {
                    for (Object c : choices) {
                        if (list.size() >= 25) break;
                        list.add(truncate(String.valueOf(c), 100));
                    }
                }
                event.replyChoiceStrings(list).queue();
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to reply Discord autocomplete: " + e.getMessage());
                return false;
            }
        }
    }

    public static class DiscordModal extends DiscordInteraction {
        private final ModalInteractionEvent event;

        public DiscordModal(DiscordBinding owner, ModalInteractionEvent event) {
            super(owner, event);
            this.event = event;
        }

        public String getModalId() { return event.getModalId(); }

        public String getValue(String fieldId) {
            var mapping = event.getValue(fieldId);
            return mapping == null ? null : mapping.getAsString();
        }

        public Map<String, String> getValues() {
            Map<String, String> map = new LinkedHashMap<>();
            event.getValues().forEach(v -> map.put(v.getId(), v.getAsString()));
            return map;
        }
    }

    public static class DiscordMessage {
        private final String content;
        private final String author;
        private final String channelId;
        private final String channelName;
        private final boolean isFromConfiguredChannel;
        private final boolean isBot;
        private final Map<String, Object> user;
        private Message message;

        public DiscordMessage(String content, String author, String channelId, String channelName, boolean isFromConfiguredChannel, boolean isBot, Map<String, Object> user) {
            this.content = content;
            this.author = author;
            this.channelId = channelId;
            this.channelName = channelName;
            this.isFromConfiguredChannel = isFromConfiguredChannel;
            this.isBot = isBot;
            this.user = user;
        }

        void attach(Message message) { this.message = message; }

        public String getContent() { return content; }
        public String getAuthor() { return author; }
        public String getChannelId() { return channelId; }
        public String getChannelName() { return channelName; }
        public boolean isFromConfiguredChannel() { return isFromConfiguredChannel; }
        public boolean isBot() { return isBot; }
        public Map<String, Object> getUser() { return user; }

        public String getId() { return message == null ? null : message.getId(); }
        public String getRawContent() { return message == null ? content : message.getContentRaw(); }
        public String getGuildId() { return message != null && message.isFromGuild() ? message.getGuild().getId() : null; }
        public boolean isDirectMessage() { return message != null && !message.isFromGuild(); }

        /** Tra loi truc tiep tin nhan nay (khong ping nguoi gui). */
        public boolean reply(String text) {
            if (message == null) return false;
            try {
                String t = text == null ? "" : text;
                if (t.length() > 2000) t = t.substring(0, 2000);
                message.reply(t).mentionRepliedUser(false).queue(null, logFailure("reply"));
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to reply to Discord message: " + e.getMessage());
                return false;
            }
        }

        public boolean react(String emoji) {
            if (message == null) return false;
            try {
                message.addReaction(Emoji.fromFormatted(emoji)).queue(null, logFailure("react"));
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to react to Discord message: " + e.getMessage());
                return false;
            }
        }

        public boolean delete() {
            if (message == null) return false;
            try {
                message.delete().queue(null, logFailure("delete"));
                return true;
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Failed to delete Discord message: " + e.getMessage());
                return false;
            }
        }
    }

    // ------------------------------------------------------------------
    // Listener
    // ------------------------------------------------------------------

    private void dispatch(IReplyCallback event, String what, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            KhoinhanJS.LOGGER.error("Error in Discord " + what + " callback: " + e.getMessage(), e);
            try {
                if (!event.isAcknowledged()) {
                    event.reply("Co loi khi xu ly yeu cau nay.").setEphemeral(true).queue();
                }
            } catch (Exception ignored) {}
        }
    }

    private class MessageListener extends ListenerAdapter {
        @Override
        public void onButtonInteraction(ButtonInteractionEvent event) {
            Consumer<DiscordButton> handler = buttonCallback;
            if (handler == null) return;
            dispatch(event, "button", () -> handler.accept(new DiscordButton(DiscordBinding.this, event)));
        }

        @Override
        public void onStringSelectInteraction(StringSelectInteractionEvent event) {
            Consumer<DiscordSelect> handler = selectCallback;
            if (handler == null) return;
            dispatch(event, "select", () -> handler.accept(new DiscordSelect(DiscordBinding.this, event)));
        }

        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            Consumer<DiscordCommand> handler = commandCallback;
            if (handler == null) return;
            dispatch(event, "slash command", () -> handler.accept(new DiscordCommand(DiscordBinding.this, event)));
        }

        @Override
        public void onModalInteraction(ModalInteractionEvent event) {
            Consumer<DiscordModal> handler = modalCallback;
            if (handler == null) return;
            dispatch(event, "modal", () -> handler.accept(new DiscordModal(DiscordBinding.this, event)));
        }

        @Override
        public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
            Consumer<DiscordAutocomplete> handler = autocompleteCallback;
            if (handler == null) return;
            try {
                handler.accept(new DiscordAutocomplete(DiscordBinding.this, event));
            } catch (Exception e) {
                KhoinhanJS.LOGGER.error("Error in Discord autocomplete callback: " + e.getMessage(), e);
            }
        }

        @Override
        public void onMessageReactionAdd(MessageReactionAddEvent event) {
            Consumer<Map<String, Object>> handler = reactionAddCallback;
            if (handler == null || event.getUserId().equals(jda.getSelfUser().getId())) return;
            fire(handler, reactionPayload(event.getUserId(), event.getMessageId(), event.getChannel().getId(),
                    event.isFromGuild() ? event.getGuild().getId() : null,
                    event.getEmoji().getFormatted(), event.getEmoji().getName()), "reaction add");
        }

        @Override
        public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
            Consumer<Map<String, Object>> handler = reactionRemoveCallback;
            if (handler == null || event.getUserId().equals(jda.getSelfUser().getId())) return;
            fire(handler, reactionPayload(event.getUserId(), event.getMessageId(), event.getChannel().getId(),
                    event.isFromGuild() ? event.getGuild().getId() : null,
                    event.getEmoji().getFormatted(), event.getEmoji().getName()), "reaction remove");
        }

        @Override
        public void onMessageDelete(MessageDeleteEvent event) {
            Consumer<Map<String, Object>> handler = messageDeleteCallback;
            if (handler == null) return;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messageId", event.getMessageId());
            payload.put("channelId", event.getChannel().getId());
            payload.put("guildId", event.isFromGuild() ? event.getGuild().getId() : null);
            fire(handler, payload, "message delete");
        }

        @Override
        public void onMessageUpdate(MessageUpdateEvent event) {
            Consumer<Map<String, Object>> handler = messageEditCallback;
            if (handler == null || event.getAuthor().isBot()) return;
            fire(handler, messageToMap(event.getMessage()), "message edit");
        }

        @Override
        public void onGuildMemberJoin(GuildMemberJoinEvent event) {
            Consumer<Map<String, Object>> handler = memberJoinCallback;
            if (handler == null) return;
            Map<String, Object> payload = memberToMap(event.getMember());
            payload.put("guildId", event.getGuild().getId());
            payload.put("memberCount", event.getGuild().getMemberCount());
            fire(handler, payload, "member join");
        }

        @Override
        public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
            Consumer<Map<String, Object>> handler = memberLeaveCallback;
            if (handler == null) return;
            Map<String, Object> payload = userToMap(event.getUser());
            payload.put("guildId", event.getGuild().getId());
            payload.put("memberCount", event.getGuild().getMemberCount());
            fire(handler, payload, "member leave");
        }

        @Override
        public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
            Consumer<Map<String, Object>> handler = voiceCallback;
            if (handler == null) return;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userId", event.getMember().getId());
            payload.put("userName", event.getMember().getUser().getName());
            payload.put("guildId", event.getGuild().getId());
            payload.put("joinedChannelId", event.getChannelJoined() == null ? null : event.getChannelJoined().getId());
            payload.put("leftChannelId", event.getChannelLeft() == null ? null : event.getChannelLeft().getId());
            fire(handler, payload, "voice update");
        }

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (messageCallbacks.isEmpty()) return;
            if (event.getAuthor().isBot()) return;

            String content = event.getMessage().getContentDisplay();
            String author = event.getAuthor().getName();
            String channelId = event.getChannel().getId();
            String channelName = event.getChannel().getName();
            boolean isBot = event.getAuthor().isBot();

            boolean isFromConfiguredChannel = channels.containsValue(channelId);

            Map<String, Object> user = new HashMap<>();
            user.put("id", event.getAuthor().getId());
            user.put("name", event.getAuthor().getName());
            user.put("displayName", event.getAuthor().getEffectiveName());
            user.put("discriminator", event.getAuthor().getDiscriminator());
            user.put("avatarUrl", event.getAuthor().getAvatarUrl());
            user.put("isBot", event.getAuthor().isBot());
            user.put("isSystem", event.getAuthor().isSystem());

            if (event.getMember() != null) {
                Member member = event.getMember();
                user.put("nickname", member.getNickname());
                user.put("colorRaw", member.getColorRaw());
                user.put("isOwner", member.isOwner());
                user.put("isPending", member.isPending());
                user.put("joinedAt", member.getTimeJoined().toString());
                user.put("boostedAt", member.getTimeBoosted() != null ? member.getTimeBoosted().toString() : null);
                user.put("isAdmin", member.hasPermission(Permission.ADMINISTRATOR));

                List<String> roleNames = new ArrayList<>();
                List<String> roleIds = new ArrayList<>();
                String highestRole = null;
                String highestRoleId = null;

                var roles = member.getRoles();
                if (!roles.isEmpty()) {
                    highestRole = roles.get(0).getName();
                    highestRoleId = roles.get(0).getId();
                    for (var role : roles) {
                        roleNames.add(role.getName());
                        roleIds.add(role.getId());
                    }
                }

                user.put("roles", roleNames.toArray(new String[0]));
                user.put("roleIds", roleIds.toArray(new String[0]));
                user.put("highestRole", highestRole);
                user.put("highestRoleId", highestRoleId);
                user.put("roleCount", roleNames.size());
            } else {
                user.put("nickname", null);
                user.put("isAdmin", false);
                user.put("roles", new String[0]);
                user.put("roleIds", new String[0]);
                user.put("highestRole", null);
                user.put("highestRoleId", null);
                user.put("roleCount", 0);
            }

            DiscordMessage discordMessage = new DiscordMessage(content, author, channelId, channelName, isFromConfiguredChannel, isBot, user);
            discordMessage.attach(event.getMessage());

            for (Consumer<DiscordMessage> callback : messageCallbacks) {
                try {
                    callback.accept(discordMessage);
                } catch (Exception e) {
                    KhoinhanJS.LOGGER.error("Error in Discord message callback: " + e.getMessage());
                }
            }
        }
    }
}
