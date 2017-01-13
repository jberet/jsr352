/*
 * Copyright (c) 2015-2017 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jberet.se;

import org.jberet.spi.JobXmlResolver;
import org.jberet.tools.AbstractJobXmlResolver;

/**
 * An implementation of {@link JobXmlResolver} that resolves job xml
 * by searching the class path.
 * <p>
 * Implementation notes: as of version 1.3.0.Beta5, this class extends
 * {@link AbstractJobXmlResolver}, and {@code resolveJobXml} method is
 * moved to the parent class.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ClassPathJobXmlResolver extends AbstractJobXmlResolver {
}
