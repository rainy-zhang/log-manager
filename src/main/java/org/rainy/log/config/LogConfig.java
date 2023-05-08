package org.rainy.log.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@Data
@Configuration
public class LogConfig {

    @Value(value = "${logging.file.name}")
    private String filename;

}
