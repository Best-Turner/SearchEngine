package searchengine.dto.response;

import lombok.Data;

@Data
public class DataSearchResponse {

    private String site;
    private String siteName;
    private String uri;
    private String snippet;
    private double relevance;

}
