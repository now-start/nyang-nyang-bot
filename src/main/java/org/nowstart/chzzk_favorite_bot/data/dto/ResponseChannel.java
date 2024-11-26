package org.nowstart.chzzk_favorite_bot.data.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bouncycastle.asn1.x509.sigi.PersonalData;

@Deprecated
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseChannel {

    private int code;
    private String message;
    private Content content;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private int size;
        private List<Data> data;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Data {
            private Channel channel;

            @Getter
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Channel {
                private String channelId;
                private String channelName;
                private String channelImageUrl;
                private boolean verifiedMark;
                private String channelDescription;
                private int followerCount;
                private boolean openLive;
                private PersonalData personalData;
            }
        }
    }
}
