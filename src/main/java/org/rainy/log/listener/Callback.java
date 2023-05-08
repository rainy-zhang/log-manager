package org.rainy.log.listener;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@FunctionalInterface
public interface Callback<T> {

    void execute(T t);

}
