package com.oneshot.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Class used to execute Discord Webhooks with low effort
 * Come from: https://gist.github.com/k3kdude/fba6f6b37594eae3d6f9475330733bdb
 */
public class DiscordWebhook {
    private static final Logger log = LoggerFactory.getLogger(DiscordWebhook.class);

    private final String url;
    private String content;
    private String username;
    private String avatarUrl;
    private boolean tts;
    private List<EmbedObject> embeds = new ArrayList<>();

    private byte[] avatarImageBytes;
    private String avatarImageFileName;


    /**
     * Constructs a new DiscordWebhook instance
     *
     * @param url The webhook URL obtained in Discord
     */
    public DiscordWebhook(String url) {
        this.url = url;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setTts(boolean tts) {
        this.tts = tts;
    }

    public void addEmbed(EmbedObject embed) {
        this.embeds.add(embed);
    }

    private void sendJsonPayload() throws IOException {
        JSONObject json = buildNormalJson();

        URL url = new URL(this.url);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("User-Agent", "Java-DiscordWebhook-BY-Gelox_");
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        OutputStream stream = connection.getOutputStream();
        stream.write(json.toString().getBytes());
        stream.flush();
        stream.close();

        connection.getInputStream().close();
        connection.disconnect();
    }

        public JSONObject buildNormalJson() throws IOException {
        if (this.content == null && this.embeds.isEmpty()) {
            throw new IllegalArgumentException("Set content or add at least one EmbedObject");
        }

        JSONObject json = new JSONObject();

        json.put("content", this.content);
        json.put("username", this.username);
        json.put("avatar_url", this.avatarUrl);
        json.put("tts", this.tts);

        if (!this.embeds.isEmpty()) {
            List<JSONObject> embedObjects = new ArrayList<>();

            for (EmbedObject embed : this.embeds) {
                JSONObject jsonEmbed = new JSONObject();

                jsonEmbed.put("title", embed.getTitle());
                jsonEmbed.put("description", embed.getDescription());
                jsonEmbed.put("url", embed.getUrl());

                if (embed.getColor() != null) {
                    Color color = embed.getColor();
                    int rgb = color.getRed();
                    rgb = (rgb << 8) + color.getGreen();
                    rgb = (rgb << 8) + color.getBlue();

                    jsonEmbed.put("color", rgb);
                }

                EmbedObject.Footer footer = embed.getFooter();
                EmbedObject.Image image = embed.getImage();
                EmbedObject.Thumbnail thumbnail = embed.getThumbnail();
                EmbedObject.Author author = embed.getAuthor();
                List<EmbedObject.Field> fields = embed.getFields();

                if (footer != null) {
                    JSONObject jsonFooter = new JSONObject();

                    jsonFooter.put("text", footer.getText());

                    if (embed.footerImageBytes != null) {
                        // In normal JSON mode, Discord cannot attach files.
                        // So you must either send the image URL as an external URL
                        // or switch to multipart mode (preferred).
                        jsonFooter.put("icon_url", "attachment://" + embed.footerImageFileName);
                    } else {
                        jsonFooter.put("icon_url", footer.getIconUrl());
                    }

                    jsonEmbed.put("footer", jsonFooter);
                }

                if (image != null && image.getUrl() != null) {
                    JSONObject jsonImage = new JSONObject();

                    jsonImage.put("url", image.getUrl());
                    jsonEmbed.put("image", jsonImage);
                }


                if (thumbnail != null && thumbnail.getUrl() != null) {
                    JSONObject jsonThumbnail = new JSONObject();

                    jsonThumbnail.put("url", thumbnail.getUrl());
                    jsonEmbed.put("thumbnail", jsonThumbnail);
                }


                if (author != null && author.getName() != null && !author.getName().isBlank()) {
                    JSONObject jsonAuthor = new JSONObject();

                    jsonAuthor.put("name", author.getName());
                    jsonAuthor.put("url", author.getUrl());
                    jsonAuthor.put("icon_url", author.getIconUrl());
                    jsonEmbed.put("author", jsonAuthor);
                }

                List<JSONObject> jsonFields = new ArrayList<>();
                for (EmbedObject.Field field : fields) {
                    JSONObject jsonField = new JSONObject();

                    jsonField.put("name", field.getName());
                    jsonField.put("value", field.getValue());
                    jsonField.put("inline", field.isInline());

                    jsonFields.add(jsonField);
                }

                if (!jsonFields.isEmpty()) {
                    jsonEmbed.put("fields", jsonFields.toArray());
                }
                embedObjects.add(jsonEmbed);
            }

            json.put("embeds", embedObjects.toArray());
        }

        return json;
    }

    public void execute() throws IOException {
        if (this.content == null && this.embeds.isEmpty()) {
            throw new IllegalArgumentException("Set content or add at least one EmbedObject");
        }

        boolean hasFile = avatarImageBytes != null ||
                embeds.stream().anyMatch(
                        e -> e.getImageBytes() != null ||
                                e.getFooterImageBytes() != null ||
                                e.getAuthorImageBytes() != null ||
                                e.getThumbnailImageBytes() != null
                );

        if (!hasFile) {
            // ------------ NORMAL JSON MODE ------------
            sendJsonPayload();
            return;
        }

        // ------------ MULTIPART MODE (with files) ------------
        String boundary = "----DiscordWebhookBoundary" + System.currentTimeMillis();

        URL url = new URL(this.url);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        OutputStream output = connection.getOutputStream();

        // JSON Part
        JSONObject multipartJson = buildJsonForMultipart();
        writePart(output, boundary, "payload_json", "application/json", multipartJson.toString().getBytes());

        int index = 0;

        // AVATAR FILE
        if (avatarImageBytes != null) {
            writeFilePart(output, boundary,
                    "files[" + index + "]",
                    avatarImageFileName,
                    avatarImageBytes
            );
            index++;
        }

        // Files Part

        for (EmbedObject embed : embeds) {
            // Image in embed
            if (embed.getImageBytes() != null) {
                writeFilePart(output, boundary,
                        "files[" + index + "]",
                        embed.getImageFileName(),
                        embed.getImageBytes()
                );
                index++;
            }

            // Author icon
            if (embed.getAuthorImageBytes() != null) {
                writeFilePart(output, boundary,
                        "files[" + index + "]",
                        embed.getAuthorImageFileName(),
                        embed.getAuthorImageBytes()
                );
                index++;
            }

            // Footer icon
            if (embed.footerImageBytes != null) {
                writeFilePart(output, boundary,
                        "files[" + index + "]",
                        embed.footerImageFileName,
                        embed.footerImageBytes
                );
                index++;
            }

            // Thumbnail icon
            if (embed.getThumbnailImageBytes() != null) {
                writeFilePart(output, boundary,
                        "files[" + index + "]",
                        embed.getThumbnailImageFileName(),
                        embed.getThumbnailImageBytes()
                );
                index++;
            }
        }

        // Final boundary
        output.write(("--" + boundary + "--").getBytes());
        output.flush();
        output.close();

        int status = connection.getResponseCode();

        InputStream responseStream =
                status >= 400 ? connection.getErrorStream() : connection.getInputStream();

        String response = responseStream != null
                ? new String(responseStream.readAllBytes(), StandardCharsets.UTF_8)
                : "";

        responseStream.close();

        if (status >= 400) {
            log.error("Discord webhook failed:");
            log.error("HTTP {} -> {}", status, response);
        }

        connection.disconnect();
    }

    private JSONObject buildJsonForMultipart() {
        JSONObject json = new JSONObject();

        json.put("content", this.content);
        json.put("username", this.username);
        if (avatarImageBytes != null) {
            json.put("avatar_url", "attachment://" + avatarImageFileName);
        } else {
            json.put("avatar_url", this.avatarUrl);
        }
        json.put("tts", this.tts);

        List<JSONObject> embedObjects = new ArrayList<>();

        for (EmbedObject embed : this.embeds) {
            JSONObject jsonEmbed = new JSONObject();

            jsonEmbed.put("title", embed.getTitle());
            jsonEmbed.put("description", embed.getDescription());
            jsonEmbed.put("url", embed.getUrl());

            if (embed.getColor() != null) {
                Color color = embed.getColor();
                int rgb = color.getRed();
                rgb = (rgb << 8) + color.getGreen();
                rgb = (rgb << 8) + color.getBlue();
                jsonEmbed.put("color", rgb);
            }

            // FOOTER
            if (embed.getFooter() != null) {
                JSONObject footer = new JSONObject();
                footer.put("text", embed.getFooter().getText());
                if (embed.getFooterImageBytes() != null) {
                    footer.put("icon_url", "attachment://" + embed.getFooterImageFileName());
                } else {
                    footer.put("icon_url", embed.getFooter().getIconUrl());
                }
                jsonEmbed.put("footer", footer);
            }

            // IMAGE
            if (embed.getImageBytes() != null) {
                JSONObject image = new JSONObject();
                image.put("url", "attachment://" + embed.getImageFileName());
                jsonEmbed.put("image", image);
            } else if (embed.getImage() != null && embed.getImage().getUrl() != null) {
                JSONObject image = new JSONObject();
                image.put("url", embed.getImage().getUrl());
                jsonEmbed.put("image", image);
            }

            // THUMBNAIL
            if (embed.getThumbnailImageBytes() != null) {
                JSONObject thumb = new JSONObject();
                thumb.put("url", "attachment://" + embed.getThumbnailImageFileName());
                jsonEmbed.put("thumbnail", thumb);
            } else if (embed.getThumbnail() != null && embed.getThumbnail().getUrl() != null) {
                JSONObject thumb = new JSONObject();
                thumb.put("url", embed.getThumbnail().getUrl());
                jsonEmbed.put("thumbnail", thumb);
            }

            // AUTHOR
            if (embed.getAuthor() != null) {
                JSONObject author = new JSONObject();
                author.put("name", embed.getAuthor().getName());
                author.put("url", embed.getAuthor().getUrl());
                if (embed.getAuthorImageBytes() != null) {
                    author.put("icon_url", "attachment://" + embed.getAuthorImageFileName());
                } else {
                    author.put("icon_url", embed.getAuthor().getIconUrl());
                }
                jsonEmbed.put("author", author);
            }

            // FIELDS
            List<JSONObject> jsonFields = new ArrayList<>();
            for (EmbedObject.Field field : embed.getFields()) {
                JSONObject jsonField = new JSONObject();
                jsonField.put("name", field.getName());
                jsonField.put("value", field.getValue());
                jsonField.put("inline", field.isInline());
                jsonFields.add(jsonField);
            }
            jsonEmbed.put("fields", jsonFields.toArray());

            embedObjects.add(jsonEmbed);
        }

        json.put("embeds", embedObjects.toArray());
        return json;
    }


    private void writePart(OutputStream os, String boundary, String name, String contentType, byte[] data) throws IOException {
        os.write(("--" + boundary + "\r\n").getBytes());
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes());
        os.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes());
        os.write(data);
        os.write("\r\n".getBytes());
    }

    private void writeFilePart(OutputStream os, String boundary, String name, String filename, byte[] data) throws IOException {
        os.write(("--" + boundary + "\r\n").getBytes());
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes());
        os.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes());
        os.write(data);
        os.write("\r\n".getBytes());
    }


    public static class EmbedObject {

        private String title;
        private String description;
        private String url;
        private Color color;

        private Footer footer;
        private Thumbnail thumbnail;
        private Image image;
        private Author author;
        private List<Field> fields = new ArrayList<>();

        public byte[] imageBytes;
        public String imageFileName;

        public byte[] authorImageBytes;
        public String authorImageFileName;

        public byte[] footerImageBytes;
        public String footerImageFileName;

        public byte[] thumbnailImageBytes;
        public String thumbnailImageFileName;

        // -------- Getters required by DiscordWebhook --------

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getUrl() { return url; }
        public Color getColor() { return color; }

        public Footer getFooter() { return footer; }
        public Thumbnail getThumbnail() { return thumbnail; }
        public Image getImage() { return image; }
        public Author getAuthor() { return author; }

        public List<Field> getFields() { return fields; }

        public byte[] getImageBytes() { return imageBytes; }
        public String getImageFileName() { return imageFileName; }

        public byte[] getAuthorImageBytes() { return authorImageBytes; }
        public String getAuthorImageFileName() { return authorImageFileName; }

        public byte[] getFooterImageBytes() { return footerImageBytes; }
        public String getFooterImageFileName() { return footerImageFileName; }

        public byte[] getThumbnailImageBytes() { return thumbnailImageBytes; }
        public String getThumbnailImageFileName() { return thumbnailImageFileName; }


        // ----- Builder methods -----

        public EmbedObject setTitle(String t) { this.title = t; return this; }
        public EmbedObject setDescription(String d) { this.description = d; return this; }
        public EmbedObject setUrl(String u) { this.url = u; return this; }
        public EmbedObject setColor(Color c) { this.color = c; return this; }

        public EmbedObject setFooter(String text, byte[] img, String fileName) {
            this.footer = new Footer(text, null);
            this.footerImageBytes = img;
            this.footerImageFileName = fileName;
            return this;
        }

        public EmbedObject setThumbnail(byte[] img, String fileName) {
            this.thumbnailImageBytes = img;
            this.thumbnailImageFileName = fileName;
            this.thumbnail = new Thumbnail(null);
            return this;
        }

        public EmbedObject setThumbnail(String url) {
            this.thumbnail = new Thumbnail(url);
            return this;
        }

        public EmbedObject setImage(byte[] img, String fileName) {
            this.imageBytes = img;
            this.imageFileName = fileName;
            this.image = new Image(null);
            return this;
        }

        public EmbedObject setImage(String url) {
            this.image = new Image(url);
            return this;
        }

        public EmbedObject setAuthor(String name, byte[] img, String fileName) {
            this.author = new Author(name, null, null);
            this.authorImageBytes = img;
            this.authorImageFileName = fileName;
            return this;
        }

        public EmbedObject setAuthor(String name, String url, String iconUrl) {
            this.author = new Author(name, url, iconUrl);
            return this;
        }

        public EmbedObject addField(String name, String value, boolean inline) {
            fields.add(new Field(name, value, inline));
            return this;
        }

        // ----- JSON Builder -----
        public JSONObject toJson() {
            JSONObject json = new JSONObject();

            json.put("title", title);
            json.put("description", description);
            json.put("url", url);

            if (color != null) {
                int rgb = (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
                json.put("color", rgb);
            }

            // Footer
            if (footer != null) {
                JSONObject f = new JSONObject();
                f.put("text", footer.text);

                f.put("icon_url",
                        footerImageBytes != null
                                ? "attachment://" + footerImageFileName
                                : footer.iconUrl
                );

                json.put("footer", f);
            }

            // Image
            if (imageBytes != null) {
                JSONObject i = new JSONObject();
                i.put("url", "attachment://" + imageFileName);
                json.put("image", i);
            } else if (image != null && image.url != null) {
                JSONObject i = new JSONObject();
                i.put("url", image.url);
                json.put("image", i);
            }

            // Thumbnail
            if (thumbnailImageBytes != null) {
                JSONObject t = new JSONObject();
                t.put("url", "attachment://" + thumbnailImageFileName);
                json.put("thumbnail", t);
            } else if (thumbnail != null && thumbnail.url != null) {
                JSONObject t = new JSONObject();
                t.put("url", thumbnail.url);
                json.put("thumbnail", t);
            }

            // Author
            if (author != null) {
                JSONObject a = new JSONObject();
                a.put("name", author.name);
                a.put("url", author.url);
                a.put("icon_url",
                        authorImageBytes != null
                                ? "attachment://" + authorImageFileName
                                : author.iconUrl
                );
                json.put("author", a);
            }

            // Fields
            if (!fields.isEmpty()) {
                List<JSONObject> arr = new ArrayList<>();
                for (Field f : fields) {
                    JSONObject jf = new JSONObject();
                    jf.put("name", f.name);
                    jf.put("value", f.value);
                    jf.put("inline", f.inline);
                    arr.add(jf);
                }
                json.put("fields", arr.toArray());
            }

            return json;
        }

        // ----- Inner classes -----

        private static class Footer {
            String text, iconUrl;
            Footer(String t, String u) { text = t; iconUrl = u; }

            public String getText() { return text; }
            public String getIconUrl() { return iconUrl; }
        }


        private static class Thumbnail {
            String url;
            Thumbnail(String u) { url = u; }
            public String getUrl() { return url; }
        }


        private static class Image {
            String url;

            Image(String u) { this.url = u; }

            public String getUrl() {
                return url;
            }
        }

        private static class Author {
            String name, url, iconUrl;
            Author(String n, String u, String i) { name = n; url = u; iconUrl = i; }

            public String getName() { return name; }
            public String getUrl() { return url; }
            public String getIconUrl() { return iconUrl; }
        }


        private static class Field {
            String name, value;
            boolean inline;

            Field(String n, String v, boolean i) {
                name = n;
                value = v;
                inline = i;
            }

            public String getName() { return name; }
            public String getValue() { return value; }
            public boolean isInline() { return inline; }
        }
    }


    static class JSONObject {

        private final HashMap<String, Object> map = new HashMap<>();

        void put(String key, Object value) {
            if (value != null) {
                map.put(key, value);
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            Set<Map.Entry<String, Object>> entrySet = map.entrySet();
            builder.append("{");

            int i = 0;
            for (Map.Entry<String, Object> entry : entrySet) {
                Object val = entry.getValue();
                builder.append(quote(entry.getKey())).append(":");

                if (val instanceof String) {
                    builder.append(quote(String.valueOf(val)));
                } else if (val instanceof Integer) {
                    builder.append(Integer.valueOf(String.valueOf(val)));
                } else if (val instanceof Boolean) {
                    builder.append(val);
                } else if (val instanceof JSONObject) {
                    builder.append(val.toString());
                } else if (val instanceof Object[]) {
                    Object[] arr = (Object[]) val;
                    builder.append("[");
                    for (int j = 0; j < arr.length; j++) {
                        builder.append(arr[j].toString());
                        if (j != arr.length - 1) {
                            builder.append(",");
                        }
                    }
                    builder.append("]");
                }


                builder.append(++i == entrySet.size() ? "}" : ",");
            }

            return builder.toString();
        }

        private String quote(String string) {
            return "\"" + string
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                    + "\"";
        }
    }
}
