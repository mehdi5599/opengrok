/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
 */
package org.opengrok.indexer.history;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengrok.indexer.logger.LoggerFactory;
import org.opengrok.indexer.util.Executor;

/**
 * handles parsing the output of the {@code hg tags} command
 * into a set of tag entries.
 */
public class MercurialTagParser implements Executor.StreamHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MercurialTagParser.class);

    /**
     * Store tag entries created by processStream.
     */
    private TreeSet<TagEntry> entries = new TreeSet<>();

    /**
     * Returns the set of entries that has been created.
     *
     * @return entries a set of tag entries
     */
    public NavigableSet<TagEntry> getEntries() {
        return entries;
    }

    @Override
    public void processStream(InputStream input) throws IOException {
        try {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(input))) {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length < 2) {
                        LOGGER.log(Level.WARNING,
                                "Failed to parse tag list: {0}",
                                "Tag line contains more than 2 columns: " + line);
                        entries = null;
                        break;
                    }
                    String rev = parts[0];
                    String tag = parts[1];

                    // The implicit 'tip' tag only causes confusion so ignore it.
                    if (tag.contentEquals("tip")) {
                        continue;
                    }

                    TagEntry tagEntry = new MercurialTagEntry(Integer.parseInt(rev), tag);
                    // Reverse the order of the list.
                    entries.add(tagEntry);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read tag list: {0}", e.getMessage());
            entries = null;
        }
    }
}
