package org.springframework.beans.factory.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConstructorArgumentValues;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by arahansa on 2016-03-20.
 */
@Slf4j
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
        implements AutowireCapableBeanFactory {

    static {
        // Eagerly load the DisposableBean class to avoid weird classloader
        // issues on EJB shutdown within WebLogic 8.1's EJB container.
        // (Reported by Andreas Senft.)
        DisposableBean.class.getName();
    }


    private final Set disposableInnerBeans = Collections.synchronizedSet(new HashSet());

    public AbstractAutowireCapableBeanFactory() {
    }
    public AbstractAutowireCapableBeanFactory(BeanFactory parentBeanFactory) {
        super(parentBeanFactory);
    }

    //---------------------------------------------------------------------
    // AutowireCapableBeanFactory 의 구현
    // Implementation of AutowireCapableBeanFactory
    //---------------------------------------------------------------------
    public Object autowire(Class beanClass, int autowireMode, boolean dependencyCheck)
            throws BeansException {
        RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
        if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
            return autowireConstructor(beanClass.getName(), bd).getWrappedInstance();
        }
        else {
            Object bean = BeanUtils.instantiateClass(beanClass);
            populateBean(bean.getClass().getName(), bd, new BeanWrapperImpl(bean));
            return bean;
        }
    }

    public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
            throws BeansException {
        if (autowireMode != AUTOWIRE_BY_NAME && autowireMode != AUTOWIRE_BY_TYPE) {
            throw new IllegalArgumentException("Just constants AUTOWIRE_BY_NAME and AUTOWIRE_BY_TYPE allowed");
        }
        RootBeanDefinition bd = new RootBeanDefinition(existingBean.getClass(), autowireMode, dependencyCheck);
        populateBean(existingBean.getClass().getName(), bd, new BeanWrapperImpl(existingBean));
    }

    public Object applyBeanPostProcessorsBeforeInitialization(Object bean, String name) throws BeansException {
        if (log.isDebugEnabled()) {
            log.debug("Invoking BeanPostProcessors before initialization of bean '" + name + "'");
        }
        Object result = bean;
        for (Iterator it = getBeanPostProcessors().iterator(); it.hasNext();) {
            BeanPostProcessor beanProcessor = (BeanPostProcessor) it.next();
            result = beanProcessor.postProcessBeforeInitialization(result, name);
        }
        return result;
    }

    public Object applyBeanPostProcessorsAfterInitialization(Object bean, String name) throws BeansException {
        if (log.isDebugEnabled()) {
            log.debug("Invoking BeanPostProcessors after initialization of bean '" + name + "'");
        }
        Object result = bean;
        for (Iterator it = getBeanPostProcessors().iterator(); it.hasNext();) {
            BeanPostProcessor beanProcessor = (BeanPostProcessor) it.next();
            result = beanProcessor.postProcessAfterInitialization(result, name);
        }
        return result;
    }

    //---------------------------------------------------------------------
    // 상위의 추상메소드 구현
    // Implementation of superclass abstract methods
    //---------------------------------------------------------------------

    protected Object createBean(String beanName, RootBeanDefinition mergedBeanDefinition) throws BeansException {
        if (log.isDebugEnabled()) {
            log.debug("Creating instance of bean '" + beanName + "' with merged definition [" + mergedBeanDefinition + "]");
        }

        if (mergedBeanDefinition.getDependsOn() != null) {
            for (int i = 0; i < mergedBeanDefinition.getDependsOn().length; i++) {
                // guarantee initialization of beans that the current one depends on
                getBean(mergedBeanDefinition.getDependsOn()[i]);
            }
        }

        BeanWrapper instanceWrapper = null;
        if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR ||
                mergedBeanDefinition.hasConstructorArgumentValues()) {
            instanceWrapper = autowireConstructor(beanName, mergedBeanDefinition);
        }
        else {
            instanceWrapper = new BeanWrapperImpl(mergedBeanDefinition.getBeanClass());
            initBeanWrapper(instanceWrapper);
        }
        Object bean = instanceWrapper.getWrappedInstance();

        // Eagerly cache singletons to be able to resolve circular references
        // even when triggered by lifecycle interfaces like BeanFactoryAware.
        if (mergedBeanDefinition.isSingleton()) {
            addSingleton(beanName, bean);
        }

        populateBean(beanName, mergedBeanDefinition, instanceWrapper);

        try {
            if (bean instanceof BeanNameAware) {
                if (log.isDebugEnabled()) {
                    log.debug("Invoking setBeanName() on BeanNameAware bean '" + beanName + "'");
                }
                ((BeanNameAware) bean).setBeanName(beanName);
            }

            if (bean instanceof BeanFactoryAware) {
                if (log.isDebugEnabled()) {
                    log.debug("Invoking setBeanFactory() on BeanFactoryAware bean '" + beanName + "'");
                }
                ((BeanFactoryAware) bean).setBeanFactory(this);
            }


            bean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
            invokeInitMethods(bean, beanName, mergedBeanDefinition);
            bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
        }
        catch (InvocationTargetException ex) {
            throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
                    "Initialization of bean failed", ex.getTargetException());
        }
        catch (Exception ex) {
            throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
                    "Initialization of bean failed", ex);
        }
        return bean;
    }



    /**
     * "autowire constructor" (with constructor arguments by type) behaviour.
     * Also applied if explicit constructor argument values are specified,
     * matching all remaining arguments with beans from the bean factory.
     * <p>This corresponds to constructor injection: In this mode, a Spring
     * bean factory is able to host components that expect constructor-based
     * dependency resolution.
     * @param beanName name of the bean to autowire by type
     * @param mergedBeanDefinition bean definition to update through autowiring
     * @return BeanWrapper for the new instance
     */
    protected BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mergedBeanDefinition)
            throws BeansException {

        ConstructorArgumentValues cargs = mergedBeanDefinition.getConstructorArgumentValues();
        ConstructorArgumentValues resolvedValues = new ConstructorArgumentValues();

        int minNrOfArgs = 0;
        if (cargs != null) {
            minNrOfArgs = cargs.getNrOfArguments();
            for (Iterator it = cargs.getIndexedArgumentValues().entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                int index = ((Integer) entry.getKey()).intValue();
                if (index < 0) {
                    throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
                            "Invalid constructor argument index: " + index);
                }
                if (index > minNrOfArgs) {
                    minNrOfArgs = index + 1;
                }
                String argName = "constructor argument with index " + index;
                ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) entry.getValue();
                Object resolvedValue = resolveValueIfNecessary(beanName, mergedBeanDefinition, argName, valueHolder.getValue());
                resolvedValues.addIndexedArgumentValue(index, resolvedValue, valueHolder.getType());
            }
            for (Iterator it = cargs.getGenericArgumentValues().iterator(); it.hasNext();) {
                ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) it.next();
                String argName = "constructor argument";
                Object resolvedValue = resolveValueIfNecessary(beanName, mergedBeanDefinition, argName, valueHolder.getValue());
                resolvedValues.addGenericArgumentValue(resolvedValue, valueHolder.getType());
            }
        }

        Constructor[] constructors = mergedBeanDefinition.getBeanClass().getConstructors();
        Arrays.sort(constructors, new Comparator() {
            public int compare(Object o1, Object o2) {
                int c1pl = ((Constructor) o1).getParameterTypes().length;
                int c2pl = ((Constructor) o2).getParameterTypes().length;
                return (new Integer(c1pl)).compareTo(new Integer(c2pl)) * -1;
            }
        });

        BeanWrapperImpl bw = new BeanWrapperImpl();
        initBeanWrapper(bw);
        Constructor constructorToUse = null;
        Object[] argsToUse = null;
        int minTypeDiffWeight = Integer.MAX_VALUE;
        for (int i = 0; i < constructors.length; i++) {
            try {
                Constructor constructor = constructors[i];
                if (constructor.getParameterTypes().length < minNrOfArgs) {
                    throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
                            minNrOfArgs + " constructor arguments specified but no matching constructor found in bean '" +
                                    beanName + "' (hint: specify index arguments for simple parameters to avoid type ambiguities)");
                }
                Class[] argTypes = constructor.getParameterTypes();
                Object[] args = new Object[argTypes.length];
                for (int j = 0; j < argTypes.length; j++) {
                    ConstructorArgumentValues.ValueHolder valueHolder = resolvedValues.getArgumentValue(j, argTypes[j]);
                    if (valueHolder != null) {
                        // synchronize if custom editors are registered
                        // necessary because PropertyEditors are not thread-safe
                        if (!getCustomEditors().isEmpty()) {
                            synchronized (this) {
                                args[j] = bw.doTypeConversionIfNecessary(valueHolder.getValue(), argTypes[j]);
                            }
                        }
                        else {
                            args[j] = bw.doTypeConversionIfNecessary(valueHolder.getValue(), argTypes[j]);
                        }
                    }
                    else {
                        if (mergedBeanDefinition.getResolvedAutowireMode() != RootBeanDefinition.AUTOWIRE_CONSTRUCTOR) {
                            throw new UnsatisfiedDependencyException(beanName, j, argTypes[j],
                                    "Did you specify the correct bean references as generic constructor arguments?");
                        }
                        Map matchingBeans = findMatchingBeans(argTypes[j]);
                        if (matchingBeans == null || matchingBeans.size() != 1) {
                            throw new UnsatisfiedDependencyException(beanName, j, argTypes[j],
                                    "There are " + matchingBeans.size() + " beans of type [" + argTypes[j] + "] for autowiring constructor. " +
                                            "There should have been 1 to be able to autowire constructor of bean '" + beanName + "'.");
                        }
                        args[j] = matchingBeans.values().iterator().next();
                        log.info("Autowiring by type from bean name '" + beanName +
                                "' via constructor to bean named '" + matchingBeans.keySet().iterator().next() + "'");
                    }
                }
                int typeDiffWeight = getTypeDifferenceWeight(argTypes, args);
                if (typeDiffWeight < minTypeDiffWeight) {
                    constructorToUse = constructor;
                    argsToUse = args;
                    minTypeDiffWeight = typeDiffWeight;
                }
            }
            catch (BeansException ex) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring constructor [" + constructors[i] + "] of bean '" + beanName +
                            "': could not satisfy dependencies. Detail: " + ex.getMessage());
                }
                if (i == constructors.length - 1 && constructorToUse == null) {
                    // all constructors tried
                    throw ex;
                }
                else {
                    // swallow and try next constructor
                }
            }
        }

        if (constructorToUse == null) {
            throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
                    "Could not resolve matching constructor");
        }
        bw.setWrappedInstance(BeanUtils.instantiateClass(constructorToUse, argsToUse));
        log.info("Bean '" + beanName + "' instantiated via constructor [" + constructorToUse + "]");
        return bw;
    }

    /**
     * Determine a weight that represents the class hierarchy difference between types and
     * arguments. A direct match, i.e. type Integer -> arg of class Integer, does not increase
     * the result - all direct matches means weight 0. A match between type Object and arg of
     * class Integer would increase the weight by 2, due to the superclass 2 steps up in the
     * hierarchy (i.e. Object) being the last one that still matches the required type Object.
     * Type Number and class Integer would increase the weight by 1 accordingly, due to the
     * superclass 1 step up the hierarchy (i.e. Number) still matching the required type Number.
     * Therefore, with an arg of type Integer, a constructor (Integer) would be preferred to a
     * constructor (Number) which would in turn be preferred to a constructor (Object).
     * All argument weights get accumulated.
     * @param argTypes the argument types to match
     * @param args the arguments to match
     * @return the accumulated weight for all arguments
     */
    private int getTypeDifferenceWeight(Class[] argTypes, Object[] args) {
        int result = 0;
        for (int i = 0; i < argTypes.length; i++) {
            if (!BeanUtils.isAssignable(argTypes[i], args[i])) {
                return Integer.MAX_VALUE;
            }
            if (args[i] != null) {
                Class superClass = args[i].getClass().getSuperclass();
                while (superClass != null) {
                    if (argTypes[i].isAssignableFrom(superClass)) {
                        result++;
                        superClass = superClass.getSuperclass();
                    }
                    else {
                        superClass = null;
                    }
                }
            }
        }
        return result;
    }










    //---------------------------------------------------------------------
    // Abstract methods to be implemented by concrete subclasses
    //---------------------------------------------------------------------

    /**
     * Find bean instances that match the required type. Called by autowiring.
     * If a subclass cannot obtain information about bean names by type,
     * a corresponding exception should be thrown.
     * @param requiredType the type of the beans to look up
     * @return a Map of bean names and bean instances that match the required type,
     * or null if none found
     * @throws BeansException in case of errors
     * @see #autowireByType
     * @see #autowireConstructor
     */
    protected abstract Map findMatchingBeans(Class requiredType) throws BeansException;

    /**
     * Return the names of the beans that depend on the given bean.
     * Called by destroyBean, to be able to destroy depending beans first.
     * @param beanName name of the bean to find depending beans for
     * @return array of names of depending beans, or null if none
     * @throws BeansException in case of errors
     * @see #destroyBean
     */
    protected abstract String[] getDependingBeanNames(String beanName) throws BeansException;
}
