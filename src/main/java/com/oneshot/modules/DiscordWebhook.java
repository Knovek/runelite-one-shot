package com.oneshot.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.*;

/**
 * Class used to execute Discord Webhooks with low effort
 * Come from: https://gist.github.com/k3kdude/fba6f6b37594eae3d6f9475330733bdb
 */
public class DiscordWebhook {
    private static final Logger log = LoggerFactory.getLogger(DiscordWebhook.class);

    public static class EmbedObject {

        private String title;
        private String description;
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


        // ----- Builder methods -----
        public EmbedObject setTitle(String t) { this.title = t; return this; }
        public EmbedObject setDescription(String d) { this.description = d; return this; }
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

        public EmbedObject setAuthor(String name, byte[] img, String fileName) {
            this.author = new Author(name, null, null);
            this.authorImageBytes = img;
            this.authorImageFileName = fileName;
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
        }

        private static class Thumbnail {
            String url;
            Thumbnail(String u) { url = u; }
        }

        private static class Image {
            String url;

            Image(String u) { this.url = u; }

        }

        private static class Author {
            String name, url, iconUrl;
            Author(String n, String u, String i) { name = n; url = u; iconUrl = i; }
        }

        private static class Field {
            String name, value;
            boolean inline;

            Field(String n, String v, boolean i) {
                name = n;
                value = v;
                inline = i;
            }
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
