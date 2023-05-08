package org.rainy.log.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
public class LogController {

    @GetMapping(value = "/writing/{count}")
    public void writing(@PathVariable("count") int count) {
        for (int i = 0; i < count; i++) {
            log.info("hello word");
        }
    }

}
