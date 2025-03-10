package searchengine.controllers;

import com.sun.xml.bind.v2.TODO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.ApiResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    @Autowired
    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    /*
    Запуск полной индексации
    */
    @GetMapping("/startIndexing")
    public ResponseEntity<ApiResponse> startIndexing(@RequestParam(value = "param", required = false) String param) {
        if (param == null) {
            return new ResponseEntity<>(ApiResponse.error("Индексация уже запущена"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(ApiResponse.success(), HttpStatus.OK);
    }

    /*
    Остановка текущей индексации
     */
    @GetMapping("/stopIndexing")
    public ResponseEntity<ApiResponse> stopIndexing(@RequestParam(value = "param", required = false) String param) {
        if (param == null) {
            return new ResponseEntity<>(ApiResponse.error("Индексация не запущена"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(ApiResponse.success(), HttpStatus.OK);
    }

    /*
    Добавление или обновление отдельной страницы
     */
    @PostMapping("/indexPage")
    public ResponseEntity<ApiResponse> addOrUpdateIndexPage(@RequestParam(value = "url", required = false) String url) {
        if (url != null) {
            return new ResponseEntity<>(ApiResponse.success(), HttpStatus.CREATED);
        }
        return new ResponseEntity<>(ApiResponse.error("Данная страница находится за пределами сайтов,\n" +
                                                      "указанных в конфигурационном файле"), HttpStatus.BAD_REQUEST);
    }


    /*
    TODO: Написать метод (Получение данных по поисковому запросу) GET /api/search
     */


}
