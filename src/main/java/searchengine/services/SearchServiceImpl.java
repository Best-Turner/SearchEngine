package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.response.SearchApiResponse;
import searchengine.exception.ApiException;
import searchengine.exception.InvalidQueryException;
import searchengine.exception.InvalidUrlException;
import searchengine.exception.OutOfBoundsUrlException;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.impl.SearchService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchServiceImpl implements SearchService {

    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 20;
    private static final String URL_REGEX = "^https:\\/\\/([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaFinder lemmaFinder;
    private final Pattern pattern = Pattern.compile(URL_REGEX);

    @Autowired
    public SearchServiceImpl(SiteRepository siteRepository, LemmaRepository lemmaRepository, LemmaFinder lemmaFinder) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.lemmaFinder = lemmaFinder;
    }


    @Override
    public SearchApiResponse getSearchData(String url, String query, int offset, int limit) throws ApiException {
        if (query.isEmpty()) {
            throw new InvalidQueryException("Задан пустой поисковый запрос");
        }
        List<Site> siteList = (url == null) ? siteRepository.findAll() : List.of(getSite(url));

        Map<String, Integer> map = lemmaFinder.collectLemmas(query);


        int validatedOffset = offset <= 0 ? DEFAULT_OFFSET : offset;
        int validatedLimit = limit <= 0 ? DEFAULT_LIMIT : limit;


        return null;
    }

    private Site getSite(String str) throws ApiException {
        Matcher matcher = pattern.matcher(str);
        if (!matcher.matches()) {
            throw new InvalidUrlException("Неверно указан формат адреса сайта");
        }
        return siteRepository
                .findSiteByUrl(str)
                .orElseThrow(() -> new OutOfBoundsUrlException("Указанная страница не найдена"));
    }
}
