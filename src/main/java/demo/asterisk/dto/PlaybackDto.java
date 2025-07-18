package demo.asterisk.dto;

import lombok.Data;

@Data
public class PlaybackDto {
    private String name;
    private String format;
    private String state;
    private String target_uri;
    private Integer duration;
}
