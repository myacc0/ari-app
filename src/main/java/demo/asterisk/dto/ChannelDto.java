package demo.asterisk.dto;

import lombok.Data;

@Data
public class ChannelDto {
    private String id;
    private String state;
    private String name;
    private String language;
    private String protocol_id;
    private String accountcode;
    private String creationtime;
    private Caller caller;
    private Caller connected;
    private Dialplan dialplan;

    @Data
    public static class Caller {
        private String name;
        private String number;
    }

    @Data
    public static class Dialplan {
        private String context;
        private String exten;
        private String app_name;
        private String app_data;
        private Integer priority;
    }
}
