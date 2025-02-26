/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.javadoc.internal.doclets.formats.html;

import com.sun.source.doctree.DeprecatedTree;
import java.util.List;

import javax.lang.model.element.Element;

import jdk.javadoc.internal.doclets.formats.html.markup.HtmlAttr;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlId;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.Navigation.PageMode;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DeprecatedAPIListBuilder;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;

/**
 * Generate File to list all the deprecated classes and class members with the
 * appropriate links.
 */
public class DeprecatedListWriter extends SummaryListWriter<DeprecatedAPIListBuilder> {

    private static final String TERMINALLY_DEPRECATED_KEY = "doclet.Terminally_Deprecated_Elements";

    /**
     * Constructor.
     *
     * @param configuration the configuration for this doclet
     * @param filename the file to be generated
     */
    public DeprecatedListWriter(HtmlConfiguration configuration, DocPath filename) {
        super(configuration, filename, PageMode.DEPRECATED, "deprecated elements",
              configuration.contents.deprecatedAPI, "doclet.Window_Deprecated_List");
    }

    /**
     * Get list of all the deprecated classes and members in all the Packages
     * specified on the command line.
     * Then instantiate DeprecatedListWriter and generate File.
     *
     * @param configuration the current configuration of the doclet.
     * @throws DocFileIOException if there is a problem writing the deprecated list
     */
    public static void generate(HtmlConfiguration configuration) throws DocFileIOException {
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.DEPRECATED)) {
            DocPath filename = DocPaths.DEPRECATED_LIST;
            DeprecatedListWriter depr = new DeprecatedListWriter(configuration, filename);
            depr.generateSummaryListFile(configuration.deprecatedAPIListBuilder);
        }
    }

    @Override
    protected void addExtraSection(DeprecatedAPIListBuilder list, Content content) {
        List<String> releases = configuration.deprecatedAPIListBuilder.releases;
        if (releases.size() > 1) {
            Content tabs = HtmlTree.DIV(HtmlStyle.checkboxes, contents.getContent(
                    "doclet.Deprecated_API_Checkbox_Label"));
            for (int i = 0; i < releases.size(); i++) {
                // Table column ids are 1-based
                tabs.add(getReleaseCheckbox(releases.get(i), i + 1));
            }
            content.add(tabs);
        }
        addSummaryAPI(list.getForRemoval(), HtmlIds.FOR_REMOVAL,
                TERMINALLY_DEPRECATED_KEY, "doclet.Element", content);
    }

    private Content getReleaseCheckbox(String name, int index) {
        // Empty string represents other/uncategorized releases. Since we can't make any assumptions
        // about release names this is arguably the safest way to avoid naming collisions.
        boolean isOtherReleases = name.isEmpty();
        Content releaseLabel = isOtherReleases
                ? contents.getContent("doclet.Deprecated_API_Checkbox_Other_Releases")
                : Text.of(name);
        HtmlId htmlId = HtmlId.of("release-" + index);
        String releaseId = isOtherReleases ? "" : Integer.toString(index);
        return HtmlTree.LABEL(htmlId.name(),
                        HtmlTree.INPUT("checkbox", htmlId)
                                .put(HtmlAttr.CHECKED, "")
                                .put(HtmlAttr.ONCLICK,
                                        "toggleGlobal(this, '" + releaseId + "', 3)"))
                .add(HtmlTree.SPAN(releaseLabel));
    }

    @Override
    protected void addExtraIndexLink(DeprecatedAPIListBuilder list, Content target) {
        if (!list.getForRemoval().isEmpty()) {
            addIndexLink(HtmlIds.FOR_REMOVAL, "doclet.Terminally_Deprecated", target);
        }
    }

    @Override
    protected void addComments(Element e, Content desc) {
        List<? extends DeprecatedTree> tags = utils.getDeprecatedTrees(e);
        if (!tags.isEmpty()) {
            addInlineDeprecatedComment(e, tags.get(0), desc);
        } else {
            desc.add(Text.EMPTY);
        }
    }

    @Override
    protected void addTableTabs(Table table, String headingKey) {
        List<String> releases = configuration.deprecatedAPIListBuilder.releases;
        if (!releases.isEmpty()) {
            table.setGridStyle(HtmlStyle.threeColumnReleaseSummary);
        }
        if (releases.size() > 1) {
            table.setDefaultTab(getTableCaption(headingKey))
                    .setAlwaysShowDefaultTab(true)
                    .setRenderTabs(false);
            for (String release : releases) {
                Content tab = TERMINALLY_DEPRECATED_KEY.equals(headingKey)
                        ? contents.getContent("doclet.Terminally_Deprecated_In_Release", release)
                        : contents.getContent("doclet.Deprecated_In_Release", release);
                table.addTab(tab,
                        element -> release.equals(utils.getDeprecatedSince(element)));
            }
        }
    }

    @Override
    protected Content getExtraContent(Element element) {
        List<String> releases = configuration.deprecatedAPIListBuilder.releases;
        if (releases.isEmpty()) {
            return null;
        }
        String deprecatedSince = utils.getDeprecatedSince(element);
        return deprecatedSince == null || deprecatedSince.isEmpty()
                ? Text.EMPTY : Text.of(deprecatedSince);
    }

    @Override
    protected TableHeader getTableHeader(String headerKey) {
        List<String> releases = configuration.deprecatedAPIListBuilder.releases;
        if (releases.isEmpty()) {
            return super.getTableHeader(headerKey);
        }
        return new TableHeader(
                contents.getContent(headerKey),
                contents.getContent("doclet.Deprecated_Elements_Release_Column_Header"),
                contents.descriptionLabel)
                .sortable(true, true, false); // Allow sorting by element name and release
    }

    @Override
    protected HtmlStyle[] getColumnStyles() {
        List<String> releases = configuration.deprecatedAPIListBuilder.releases;
        if (releases.isEmpty()) {
            return super.getColumnStyles();
        }
        return new HtmlStyle[]{ HtmlStyle.colSummaryItemName, HtmlStyle.colSecond, HtmlStyle.colLast };
    }

    @Override
    protected Content getTableCaption(String headingKey) {
        Content caption = contents.getContent(headingKey);
        return TERMINALLY_DEPRECATED_KEY.equals(headingKey)
                ? caption : contents.getContent("doclet.Deprecated_Elements", caption);
    }
}
