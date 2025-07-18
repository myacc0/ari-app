package demo.asterisk.dto;

import lombok.Data;

@Data
public class EventDto {
    private String application;
    private String type;
    private String timestamp;
    private String asterisk_id;
    private String[] args;
    private ChannelDto channel;
    private PlaybackDto recording;
}
