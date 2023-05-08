package org.rainy.log.listener;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@Builder
@Data
public class FileContent {

    private Path file;

    private String content;

    private LocalDateTime dateTime;

}
