/*
 * Copyright 2015-2018 Canoo Engineering AG.
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
package com.canoo.dp.impl.platform.projector.server.projection;

import com.canoo.dp.impl.platform.projector.action.Action;
import com.canoo.dp.impl.platform.projector.action.DefaultServerActionBean;
import com.canoo.dp.impl.platform.projector.base.Icon;
import com.canoo.dp.impl.platform.projector.base.IconBean;
import com.canoo.dp.impl.platform.projector.form.Form;
import com.canoo.dp.impl.platform.projector.form.FormBean;
import com.canoo.dp.impl.platform.projector.form.FormSection;
import com.canoo.dp.impl.platform.projector.form.FormSectionBean;
import com.canoo.dp.impl.platform.projector.metadata.AbstractKeyValueBean;
import com.canoo.dp.impl.platform.projector.metadata.KeyValue;
import com.canoo.dp.impl.platform.projector.server.i18n.BeanLocalization;
import com.canoo.dp.impl.platform.projector.server.i18n.LocaleKey;
import com.canoo.platform.remoting.BeanManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FormBuilder<T extends Form> {

    private final Class<T> formClass;

    private final BeanManager beanManager;

    private String description;

    private LocaleKey descriptionLocaleKey;

    private String title;

    private LocaleKey titleLocaleKey;

    private String iconFamily;

    private String iconCode;

    private List<FormSection> sections = new ArrayList<>();

    private List<Action> actions = new ArrayList<>();

    private Map<String, Object> metadata = new HashMap<>();

    private final BeanLocalization localization;

    private FormBuilder(Class<T> formClass, BeanManager beanManager, BeanLocalization localization) {
        this.formClass = formClass;
        this.beanManager = beanManager;
        this.localization = localization;
    }

    private FormBuilder(Class<T> formClass, BeanManager beanManager) {
        this(formClass, beanManager, null);
    }

    public FormBuilder<T> withIcon(String iconFamily, String iconCode) {
        this.iconCode = iconCode;
        this.iconFamily = iconFamily;
        return this;
    }

    public FormBuilder<T> withDescription(String description) {
        this.description = description;
        this.descriptionLocaleKey = null;
        return this;
    }

    public FormBuilder<T> withLocalizedDescription(String key, Object... args) {
        if(localization == null) {
            throw new NullPointerException("localization == null");
        }
        this.description = null;
        this.descriptionLocaleKey = new LocaleKey(key, args);
        return this;
    }

    public FormBuilder<T> withTitle(String title) {
        this.title = title;
        this.titleLocaleKey = null;
        return this;
    }

    public FormBuilder<T> withLocalizedTitle(String key, Object... args) {
        if(localization == null) {
            throw new NullPointerException("localization == null");
        }
        this.title = null;
        this.titleLocaleKey = new LocaleKey(key, args);
        return this;
    }

    public FormBuilder<T> withLayoutMetadata(String key, String value) {
        metadata.put(key, value);
        return this;
    }

    public FormBuilder<T> withLayoutMetadata(String key, Integer value) {
        metadata.put(key, value);
        return this;
    }

    public FormBuilder<T> withLayoutMetadata(String key, Double value) {
        metadata.put(key, value);
        return this;
    }

    public FormBuilder<T> withLayoutMetadata(String key, Long value) {
        metadata.put(key, value);
        return this;
    }

    public FormBuilder<T> withLayoutMetadata(String key, Float value) {
        metadata.put(key, value);
        return this;
    }

    public FormBuilder<T> withLayoutMetadata(String key, Boolean value) {
        metadata.put(key, value);
        return this;
    }

    public FormBuilder<T> removeLayoutMetadata(String key) {
        metadata.remove(key);
        return this;
    }

    public FormBuilder<T> withSection(FormSection section) {
        sections.add(section);
        return this;
    }

    public FormBuilder<T> withAction(Action action) {
        actions.add(action);
        return this;
    }

    public InternalFormSectionBuilder<T> withSection() {
        return new InternalFormSectionBuilder<>(this);
    }

    public InternalServerActionBuilder<T> withServerAction(String actionName) {
        return new InternalServerActionBuilder<>(this, actionName);
    }

    public T build() {
        T form = beanManager.create(formClass);

        if(titleLocaleKey == null) {
            form.setTitle(title);
        } else {
            localization.add(form.titleProperty(), titleLocaleKey);
        }

        if(descriptionLocaleKey == null) {
            form.setDescription(description);
        } else {
            localization.add(form.descriptionProperty(), descriptionLocaleKey);
        }

        metadata.forEach((k, v) -> {
            if(v instanceof String) {
                KeyValue<String> keyValue = beanManager.create(AbstractKeyValueBean.class);
                keyValue.setKey(k);
                keyValue.setValue((String) v);
                form.getLayoutMetadata().add(keyValue);
            } else if(v instanceof Boolean) {
                KeyValue<Boolean> keyValue = beanManager.create(AbstractKeyValueBean.class);
                keyValue.setKey(k);
                keyValue.setValue((Boolean) v);
                form.getLayoutMetadata().add(keyValue);
            }
        });

        if(iconCode != null) {
            Icon icon = beanManager.create(IconBean.class);
            icon.setIconFamily(iconFamily);
            icon.setIconCode(iconCode);
            form.setIcon(icon);
        }

        form.getSections().addAll(sections);
        form.getActions().addAll(actions);

        return form;
    }

    public static FormBuilder<FormBean> create(BeanManager beanManager) {
        return new FormBuilder<>(FormBean.class, beanManager);
    }

    public class InternalServerActionBuilder<T extends Form> extends ServerActionBuilder<DefaultServerActionBean> {

        private final FormBuilder<T> formBuilder;

        public InternalServerActionBuilder(FormBuilder<T> formBuilder, String actionName) {
            super(DefaultServerActionBean.class, beanManager, actionName, localization);
            this.formBuilder = formBuilder;
        }

        public FormBuilder<T> addToForm() {
            return formBuilder.withAction(build());
        }

        public FormBuilder<T> addToForm(Consumer<DefaultServerActionBean> sectionConsumer) {
            DefaultServerActionBean actionBean = build();
            sectionConsumer.accept(actionBean);
            return formBuilder.withAction(actionBean);
        }
    }

    public class InternalFormSectionBuilder<T extends Form> extends FormSectionBuilder<FormSectionBean> {

        private final FormBuilder<T> formBuilder;

        private InternalFormSectionBuilder(FormBuilder<T> formBuilder) {
            super(FormSectionBean.class, beanManager, localization);
            this.formBuilder = formBuilder;
        }

        public FormBuilder<T> addToForm() {
            return formBuilder.withSection(build());
        }

        public FormBuilder<T> addToForm(Consumer<FormSectionBean> sectionConsumer) {
            FormSectionBean formSection = build();
            sectionConsumer.accept(formSection);
            return formBuilder.withSection(formSection);
        }

    }
}
