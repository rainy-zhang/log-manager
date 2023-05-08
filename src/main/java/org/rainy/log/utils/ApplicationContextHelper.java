package org.rainy.log.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * <p>
 *
 * </p>
 *
 * @author zhangyu
 */
@Component
public class ApplicationContextHelper implements ApplicationContextAware {

    private static ApplicationContext _ac;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        _ac = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        return _ac.getBean(clazz);
    }

}
