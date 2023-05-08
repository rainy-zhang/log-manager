package org.rainy.log.controller;

import lombok.extern.slf4j.Slf4j;
import org.rainy.log.search.SearchService;
import org.rainy.log.search.param.SearchParam;
import org.rainy.log.utils.JsonMapper;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@Slf4j
@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @RequestMapping(value = "/search")
    public String search(@RequestBody SearchParam param) {
        log.info("Searching by: {}", JsonMapper.object2String(param));
        return searchService.search(param);
    }

}
