package dev.salmonllama.fsbot.endpoints.imgur;

public class ImgurUpload {
    private final String id;
    private final long dateTime;
    private final String deleteHash;
    private final String link;

    public ImgurUpload(ImgurUploadBuilder builder) {
        id = builder.id;
        dateTime = builder.dateTime;
        deleteHash = builder.deleteHash;
        link = builder.link;
    }

    public String getId() {
        return id;
    }

    public long getDateTime() {
        return dateTime;
    }

    public String getDeleteHash() {
        return deleteHash;
    }

    public String getLink() {
        return link;
    }

    public static class ImgurUploadBuilder {
        private String id;
        private long dateTime;
        private String deleteHash;
        private String link;

        public ImgurUploadBuilder() {

        }

        public ImgurUploadBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public ImgurUploadBuilder setDateTime(long dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public ImgurUploadBuilder setDeleteHash(String deleteHash) {
            this.deleteHash = deleteHash;
            return this;
        }

        public ImgurUploadBuilder setLink(String link) {
            this.link = link;
            return this;
        }

        public ImgurUpload build() {
            return new ImgurUpload(this);
        }
    }
}
