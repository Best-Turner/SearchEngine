package searchengine.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SearchApiResponse {

    private boolean result;
    private int count;
    private List<DataSearchResponse> data;

}


