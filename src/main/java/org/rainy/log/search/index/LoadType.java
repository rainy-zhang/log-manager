package org.rainy.log.search.index;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@Getter
@AllArgsConstructor
public enum LoadType {

    INCR(0, "增量"),
    FULL(1, "全量");

    private final int code;
    private final String desc;

}
