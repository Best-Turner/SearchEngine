package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.config.SiteFromConfig;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.SiteConverter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static searchengine.model.Site.Status.FAILED;
import static searchengine.model.Site.Status.INDEXED;

@Slf4j
@Service
public class WebLinkCrawlerService {

    @Value(value = "${jsoup.user-agent}")
    private String userAgent;
    @Value(value = "${jsoup.referrer}")
    private String referrer;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    private final SiteConverter converter;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private Map<String, AtomicInteger> urlLinkCounts;
    private final ConcurrentHashMap<String, Boolean> addedLinks = new ConcurrentHashMap<>();

    public WebLinkCrawlerService(PageRepository pageRepository,
                                 SiteRepository siteRepository,
                                 SitesList sitesList,
                                 SiteConverter converter) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
        urlLinkCounts = new HashMap<>();
        this.converter = converter;
    }

    public void startIndexing() {
        stopped.set(false);
        long before = System.currentTimeMillis();
        List<SiteFromConfig> fromConfigFile = sitesList.getSites();
        List<Site> list;
        List<LinkCrawler> tasks = new ArrayList<>();


        siteRepository.deleteAll();// удалить все потому что запускается новая индексация

        list = converter.convert(fromConfigFile);
        list.forEach(site ->
                tasks.add(new LinkCrawler(site, site.getUrl())));


        log.debug("Прочитали все сайты из конфиг файла и сохранили в List");
        siteRepository.saveAll(list);
        log.info("Сохранили все сайты из конфиг файла в БД");

        log.info("Проходим по каждому сайту и запускаем его индексацию");
        tasks.forEach(forkJoinPool::invoke);

        log.info("Закончили индексацию каждого сайта");
        siteRepository.saveAll(list);
        log.info("Статус сайтов обновлен в БД");
        long after = System.currentTimeMillis();
        log.info("Время выполнения  = {}, мс", (after - before));
    }


    public boolean stopIndexing() {
        stopped.set(true);
        return stopped.get();
    }


    private class LinkCrawler extends RecursiveAction {
        private final Site site;
        private final String path;

        public LinkCrawler(Site site, String path) {
            this.site = site;
            this.path = path;
        }

        @Override
        protected void compute() {

            if (pageRepository.existsByPath(path)) {
                return;
            }

            List<LinkCrawler> taskList = new ArrayList<>();
            if (stopped.get() && !site.getStatus().equals(FAILED)) {
                site.setStatus(FAILED);
                site.setLastError("Индексация остановлена пользователем");
                log.info("Сохранил Failed");
                return;
            }
            final Page page;
            try {
                String tempUrl = path;
                if (site.getUrl().equals(path)) {
                    tempUrl = "/";
                }
                log.info("переходим по ссылке - {}", site.getUrl().concat(tempUrl));
                Connection connection = Jsoup.connect(site.getUrl() + tempUrl);

                Document document = connection
                        .userAgent(userAgent)
                        .referrer(referrer)
                        .timeout(0)
                        .get();

                int statusCode = connection.response().statusCode();
                String html = document.html();
                page = new Page();
                page.setSite(site);
                page.setCode(statusCode);
                page.setPath(tempUrl);
                page.setContent(html);
                pageRepository.save(page);

                if (!stopped.get()) {
                    getLinksForPage(document)
                            .forEach(link ->
                                    taskList.add(new LinkCrawler(site, link)));
                    urlLinkCounts.computeIfAbsent(site.getUrl(), k -> new AtomicInteger(0)).incrementAndGet();
                }
                invokeAll(taskList);

                AtomicInteger atomicInteger = urlLinkCounts.get(site.getUrl());
                if (atomicInteger.decrementAndGet() == 0) {
                    site.setStatus(INDEXED);
                    siteRepository.save(site);
                    log.info("Сайт - {} проиндексирован, статус поменян на {}", site.getUrl(), site.getStatus());
                }

            } catch (IOException e) {

                log.error("Ошибка при обработке страницы: {}", e.getMessage());
                site.setLastError(e.getMessage());
                site.setStatus(Site.Status.FAILED);

            }
        }


        private List<String> getLinksForPage(Document document) {
            Elements links = document.select("a");
            List<String> linksList = new ArrayList<>();
            for (Element link : links) {
                String absLink = link.attr("abs:href"); // Получаем абсолютный URL
                // Проверяем, что ссылка принадлежит текущему сайту и еще не посещена
                if (checkLinkOnCorrectFormat(absLink)) {
                    String relativeLink = getRelativeLink(absLink);
                    if (addedLinks.putIfAbsent(relativeLink, true) == null) {
                        log.info("Этой ссылки {} нет в БД", relativeLink);
                        siteRepository.updateStatusTime(site.getId(), LocalDateTime.now());
                        linksList.add(relativeLink);
                    }

//                        if (!pageRepository.existsByPath(relativeLink)) {
//                            log.info("Этой ссылки {} нет в БД", relativeLink);
//                            siteRepository.updateStatusTime(site.getId(), LocalDateTime.now());
//                            linksList.add(relativeLink);
//                        }
                }
            }
            return linksList;
        }


        private static boolean checkLinkOnCorrectFormat(String link) {
            String regex = "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?(?!#.*)$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(link);
            if (!matcher.matches()) {
                return false;
            }
            return (checkExtension(link));
        }

        private String getRelativeLink(String absLink) {
            if (checkBaseUrl(absLink, site.getUrl())) {
                return absLink.substring(site.getUrl().length());
            }
            return "/";
        }

        private static boolean checkBaseUrl(String link, String baseUrl) {
            if (!link.isBlank()) {
                return link.toLowerCase().startsWith(baseUrl);
            }
            return false;
        }

        private static boolean checkExtension(String link) {
            String[] excludeFormats = {".jpg", ".jpeg", ".png", ".gif",
                    ".bmp", ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".zip", ".rar"};

            for (String extension : excludeFormats) {
                if (link.toLowerCase().endsWith(extension)) {
                    return false;
                }
            }
            return true;
        }
    }
}

