package com.example.back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;


@Data  // génère tous les getters/setters automatiquement
public class IgdbGameDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("cover")
    private Cover cover;

    @JsonProperty("genres")
    private List<Genre> genres;

    @JsonProperty("platforms")
    private List<Platform> platforms;

    @JsonProperty("first_release_date")
    private Long firstReleaseDate;

    @Data
    public static class Genre {
        @JsonProperty("name")
        private String name;
    }

    @Data
    public static class Platform {
        @JsonProperty("name")
        private String name;
    }

    @Data
    public static class Cover {
        @JsonProperty("url")
        private String url;
    }
}






