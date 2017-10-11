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
package com.canoo.dp.impl.client;

import com.canoo.dp.impl.remoting.EventDispatcherImpl;
import com.canoo.dp.impl.remoting.legacy.RemotingConstants;
import com.canoo.dp.impl.remoting.legacy.core.ModelStore;

public class ClientEventDispatcher extends EventDispatcherImpl {

    public ClientEventDispatcher(final ModelStore modelStore) {
        super(modelStore);
    }

    @Override
    public String getLocalSystemIdentifier() {
        return RemotingConstants.SOURCE_SYSTEM_CLIENT;
    }
}