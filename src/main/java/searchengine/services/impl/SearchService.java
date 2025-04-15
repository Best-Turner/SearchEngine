package searchengine.services.impl;

import searchengine.dto.response.SearchApiResponse;
import searchengine.exception.ApiException;

public interface SearchService {

    SearchApiResponse getSearchData(String url, String query, int offset, int limit) throws ApiException;
}
