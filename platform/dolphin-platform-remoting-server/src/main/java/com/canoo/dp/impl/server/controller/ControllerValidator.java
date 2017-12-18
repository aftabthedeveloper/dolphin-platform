/*
 * Copyright 2015-2017 Canoo Engineering AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.canoo.dp.impl.server.controller;

import com.canoo.dp.impl.platform.core.Assert;
import com.canoo.dp.impl.platform.core.ReflectionHelper;
import com.canoo.platform.remoting.server.DolphinAction;
import com.canoo.platform.remoting.server.DolphinModel;
import com.canoo.platform.remoting.server.Param;
import org.apiguardian.api.API;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * This class validates the DolphinController
 */
@API(since = "0.x", status = INTERNAL)
public class ControllerValidator {

    public void validate(final Class<?> clazz) throws ControllerValidationException {

        Assert.requireNonNull(clazz, "Controller class");

        if (isInterface(clazz)) {
            throw new ControllerValidationException("Dolphin Controller " + clazz.getName() + " must be a class.");
        }
        if (isAbstract(clazz)) {
            throw new ControllerValidationException("Dolphin Controller " + clazz.getName() + " can't be abstract.");
        }
        if (isFinal(clazz)) {
            throw new ControllerValidationException("Dolphin Controller " + clazz.getName() + " can't be final.");
        }
        if (isMoreThanOnePostConstruct(clazz)) {
            throw new ControllerValidationException("Only one PostConstruct method is allowed in Controller " + clazz.getName());
        }
        if (isMoreThanOnePreDestroy(clazz)) {
            throw new ControllerValidationException("Only one PreDestroy method is allowed in Controller " + clazz.getName());
        }

        if (!isDolphinModelPresent(clazz)) {
            throw new ControllerValidationException("Controller " + clazz.getName() + " must have a DolphinModel.");
        }
        if (isMoreThanOneDolphinModel(clazz)) {
            throw new ControllerValidationException("Controller " + clazz.getName() + " should not contain more than one DolphinModel.");
        }
        checkPreDestroyContainsParameter(clazz);
        checkPostConstructContainsParameter(clazz);
        checkDolphinActionVoid(clazz);
        checkDolphinActionAnnotatedWithParam(clazz);
    }

    private boolean isInterface(final Class<?> clazz) {
        return clazz.isInterface();
    }

    private boolean isAbstract(final Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    private boolean isFinal(final Class<?> clazz) {
        return Modifier.isFinal(clazz.getModifiers());
    }

    private void checkPostConstructContainsParameter(final Class<?> clazz) throws ControllerValidationException {
        Assert.requireNonNull(clazz, "clazz");
        final ControllerValidationException controllerValidationException = ReflectionHelper.getInheritedDeclaredMethods(clazz).stream().
                filter(method -> method.isAnnotationPresent(PostConstruct.class)).
                filter(method -> method.getParameterTypes().length > 0).
                findAny().map(method ->new ControllerValidationException("PreDestroy method " + method.getName() + " should not contain parameter in Controller " + clazz.getName())).
                orElse(null);
        if(controllerValidationException != null){
            throw controllerValidationException;
        }
    }

    private void checkPreDestroyContainsParameter(final Class<?> clazz)  throws ControllerValidationException {
        Assert.requireNonNull(clazz, "clazz");
        final ControllerValidationException controllerValidationException = ReflectionHelper.getInheritedDeclaredMethods(clazz).stream().
                filter(method -> method.isAnnotationPresent(PreDestroy.class)).
                filter(method -> method.getParameterTypes().length > 0).
                findAny().map(method ->new ControllerValidationException("PreDestroy method " + method.getName() + " should not contain parameter in Controller " + clazz.getName())).
                orElse(null);
        if(controllerValidationException != null){
            throw controllerValidationException;
        }
    }

    private void checkDolphinActionVoid(final Class<?> clazz) throws ControllerValidationException {
        Assert.requireNonNull(clazz, "clazz");
        final ControllerValidationException controllerValidationException = ReflectionHelper.getInheritedDeclaredMethods(clazz).stream().
                filter(method -> method.isAnnotationPresent(DolphinAction.class)).
                filter(method -> !method.getReturnType().equals(Void.TYPE)).
                findAny().
                map(method -> new ControllerValidationException("Return type of controller action " + method.getName() + " in controller type " + clazz.getName() + " must be of type void.")).
                orElse(null);

        if(controllerValidationException != null){
            throw controllerValidationException;
        }
    }

    private void checkDolphinActionAnnotatedWithParam(final Class<?> clazz)  throws ControllerValidationException {
        Assert.requireNonNull(clazz, "clazz");
        final ControllerValidationException controllerValidationException = ReflectionHelper.getInheritedDeclaredMethods(clazz).stream().
                filter(method -> method.isAnnotationPresent(DolphinAction.class)).
                filter(method -> checkMethodForMissingParamAnnotation(method)).
                findAny().
                map(method -> new ControllerValidationException("DolphinAction " + ControllerUtils.getActionMethodName(method) + " parameters must be annotated with @param in Controller " + clazz.getName())).
                orElse(null);

        if(controllerValidationException != null){
            throw controllerValidationException;
        }
    }

    private boolean checkMethodForMissingParamAnnotation(final Method method) {
        Assert.requireNonNull(method, "method");
        return Arrays.asList(method.getParameters()).stream().
                filter(param -> !param.isAnnotationPresent(Param.class)).
                findAny().
                map(param -> true).
                orElse(false);
    }

    private boolean isDolphinModelPresent(final Class<?> clazz) {
        List<Field> fields = ReflectionHelper.getInheritedDeclaredFields(clazz);
        if(null != fields){
            long count = fields.stream().filter(field -> field.isAnnotationPresent(DolphinModel.class)).count();
            return count > 0;
        }
        return false;
    }

    private boolean isMoreThanOneDolphinModel(final Class<?> clazz) {
        List<Field> fields = ReflectionHelper.getInheritedDeclaredFields(clazz);
        if(null != fields){
            long count = fields.stream().filter(field -> field.isAnnotationPresent(DolphinModel.class)).count();
            return count > 1;
        }
        return false;
    }


    private boolean isMoreThanOnePreDestroy(final Class<?> clazz) {
        List<Method> methods = ReflectionHelper.getInheritedDeclaredMethods(clazz);
        if(null != methods) {
            long count = methods.stream().filter(method -> method.isAnnotationPresent(PreDestroy.class)).count();
            return count > 1;
        }
        return false;
    }

    private boolean isMoreThanOnePostConstruct(final Class<?> clazz) {
        List<Method> methods = ReflectionHelper.getInheritedDeclaredMethods(clazz);
        if(null != methods) {
            long count = methods.stream().filter(method -> method.isAnnotationPresent(PostConstruct.class)).count();
            return count > 1;
        }
        return false;
    }
}
