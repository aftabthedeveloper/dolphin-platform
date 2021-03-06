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
package com.canoo.dp.impl.platform.projector.form;

import com.canoo.dp.impl.platform.projector.action.Action;
import com.canoo.dp.impl.platform.projector.base.Projectable;
import com.canoo.dp.impl.platform.projector.base.WithDescription;
import com.canoo.dp.impl.platform.projector.base.WithIcon;
import com.canoo.dp.impl.platform.projector.base.WithLayoutMetadata;
import com.canoo.dp.impl.platform.projector.base.WithTitle;
import com.canoo.platform.remoting.ObservableList;
import com.canoo.platform.remoting.RemotingBean;

@RemotingBean
public interface Form extends WithDescription, WithTitle, WithIcon, WithLayoutMetadata, Projectable {

    ObservableList<FormSection> getSections();

    ObservableList<Action> getActions();
}
