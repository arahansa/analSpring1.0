package org.springframework.beans.factory;

/**
 * Created by arahansa on 2016-03-28.
 */
public interface DisposableBean {

    /**
     * Invoked by a BeanFactory on destruction of a singleton.
     * @throws Exception in case of shutdown errors.
     * Exceptions will get logged but not rethrown to allow
     * other beans to release their resources too.
     */
    void destroy() throws Exception;

}
