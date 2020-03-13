/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     pierre
 */
package org.nuxeo.ecm.core.scroll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestRepositoryScroll {

    protected static final String USERNAME = "bob";

    @Inject
    public ScrollService service;

    @Inject
    public CoreSession session;

    @Inject
    public TransactionalFeature txFeature;

    public String getScrollName() {
        return "repository";
    }

    @Test
    public void testService() {
        assertNotNull(service);
    }

    @Test
    public void testNormal() throws Exception {
        String docId = createADocument();
        String nxql = "SELECT * FROM Document";
        ScrollRequest request = DocumentScrollRequest.builder(nxql).name(getScrollName()).username(USERNAME).build();
        try (Scroll scroll = service.scroll(request)) {
            assertNotNull(scroll);
            assertTrue(scroll.toString(), scroll.hasNext());
            assertEquals(Arrays.asList(docId), scroll.next());
            assertFalse(scroll.toString(), scroll.hasNext());
            try {
                scroll.next();
                fail("Exception expected");
            } catch (NoSuchElementException e) {
                // expected
            }
        }
        try (Scroll scroll = service.scroll(request)) {
            // don't need to call hasNext
            assertEquals(Arrays.asList(docId), scroll.next());
            assertFalse(scroll.toString(), scroll.hasNext());
        }
        try (Scroll scroll = service.scroll(request)) {
            // or call it many time
            assertTrue(scroll.toString(), scroll.hasNext());
            assertTrue(scroll.toString(), scroll.hasNext());
            assertTrue(scroll.toString(), scroll.hasNext());
            assertEquals(Arrays.asList(docId), scroll.next());
            assertFalse(scroll.toString(), scroll.hasNext());
        }
    }

    @Test
    public void testNoResults() {
        String nxql = "SELECT * FROM Document";
        ScrollRequest request = DocumentScrollRequest.builder(nxql).name(getScrollName()).username(USERNAME).build();
        try (Scroll scroll = service.scroll(request)) {
            assertNotNull(scroll);
            assertFalse(scroll.toString(), scroll.hasNext());
        }
    }

    @Test
    public void testInvalidQuery() {
        String nxql = "foo,bar";
        ScrollRequest request = DocumentScrollRequest.builder(nxql).name(getScrollName()).username(USERNAME).build();
        try (Scroll scroll = service.scroll(request)) {
            scroll.hasNext();
            fail("Expecting an NXQL parse execption");
        } catch (QueryParseException e) {
            // expected
        }
    }

    protected String createADocument() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "myFolder", "Folder");
        doc = session.createDocument(doc);

        ACP acp = new ACPImpl();
        acp.getOrCreateACL().add(new ACE("Administrator", "Everything", true));
        acp.getOrCreateACL().add(new ACE(USERNAME, "Read", true));
        doc.setACP(acp, false);
        session.save();
        txFeature.nextTransaction();
        return doc.getId();
    }

}
