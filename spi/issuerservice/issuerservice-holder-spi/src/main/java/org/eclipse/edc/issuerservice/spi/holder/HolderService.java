/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.spi.holder;

import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

public interface HolderService {

    ServiceResult<Void> createHolder(Holder holder);

    ServiceResult<Void> deleteHolder(String holderId);

    ServiceResult<Void> updateHolder(Holder holder);

    ServiceResult<Collection<Holder>> queryHolders(QuerySpec querySpec);

    ServiceResult<Holder> findById(String holderId);
}
